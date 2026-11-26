package com.theragenx.intake.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.theragenx.intake.config.IntakeProperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Google Gemini through the {@code generateContent} REST endpoint, with JSON output constrained
 * by a response schema. The API key is sent in the {@code x-goog-api-key} header, so it never
 * appears in a URL, a log line or a recorded request body.
 */
public class GeminiClient implements LlmClient {

    private static final String PROVIDER = "gemini";
    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final String API_KEY_VARIABLE = "GEMINI_API_KEY";
    private static final String COMPLETE_FINISH_REASON = "STOP";
    private static final String RETRY_INFO_TYPE = "type.googleapis.com/google.rpc.RetryInfo";
    private static final String DAILY_QUOTA_MARKER = "PerDay";
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 500, 502, 503, 504);

    private final IntakeProperties.Llm settings;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(IntakeProperties.Llm settings, HttpClient httpClient, ObjectMapper objectMapper) {
        this.settings = settings;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String modelName() {
        return PROVIDER + "/" + settings.model();
    }

    @Override
    public LlmExchange complete(LlmRequest request) {
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            throw new LlmException("No API key: set the " + API_KEY_VARIABLE + " environment variable");
        }
        ObjectNode body = requestBody(request);
        HttpResponse<String> response = send(body);
        JsonNode responseBody = parse(response.body());
        if (response.statusCode() != 200) {
            throw errorFrom(response.statusCode(), responseBody);
        }
        return new LlmExchange(modelName(), body, responseBody, answerText(responseBody));
    }

    private ObjectNode requestBody(LlmRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.putObject("systemInstruction").putArray("parts").addObject().put("text", request.systemPrompt());
        ObjectNode userTurn = body.putArray("contents").addObject();
        userTurn.put("role", "user");
        userTurn.putArray("parts").addObject().put("text", request.userPrompt());
        ObjectNode generation = body.putObject("generationConfig");
        generation.put("temperature", settings.temperature());
        generation.put("maxOutputTokens", settings.maxOutputTokens());
        generation.put("responseMimeType", "application/json");
        generation.set("responseSchema", toGeminiSchema(request.responseSchema().deepCopy()));
        return body;
    }

    /** Gemini expects upper-case type names (STRING, OBJECT, ...); the neutral schema uses lower case. */
    private static JsonNode toGeminiSchema(JsonNode schema) {
        if (schema instanceof ObjectNode object) {
            if (object.path("type").isTextual()) {
                object.put("type", object.get("type").asText().toUpperCase());
            }
            for (Iterator<Map.Entry<String, JsonNode>> it = object.fields(); it.hasNext(); ) {
                toGeminiSchema(it.next().getValue());
            }
        } else if (schema.isArray()) {
            schema.forEach(GeminiClient::toGeminiSchema);
        }
        return schema;
    }

    private HttpResponse<String> send(ObjectNode body) {
        URI endpoint = URI.create(settings.baseUrl() + "/models/" + settings.model() + ":generateContent");
        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                .timeout(settings.requestTimeout())
                .header("Content-Type", "application/json")
                .header(API_KEY_HEADER, settings.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new LlmException("Gemini request failed: " + e.getMessage(), true, null, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted while waiting for Gemini", false, null, e);
        }
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new LlmException("Gemini returned a body that is not JSON: " + e.getOriginalMessage(), true, null, e);
        }
    }

    private static String answerText(JsonNode responseBody) {
        JsonNode candidate = responseBody.path("candidates").path(0);
        String finishReason = candidate.path("finishReason").asText();
        if (!COMPLETE_FINISH_REASON.equals(finishReason)) {
            throw new LlmException("Gemini did not finish its answer (finishReason " + finishReason + ")");
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            boolean isThinking = part.path("thought").asBoolean(false);
            if (!isThinking) {
                text.append(part.path("text").asText());
            }
        }
        return text.toString();
    }

    private static LlmException errorFrom(int statusCode, JsonNode responseBody) {
        JsonNode error = responseBody.path("error");
        String message = "Gemini answered HTTP " + statusCode + ": " + error.path("message").asText();
        boolean dailyQuotaUsedUp = error.toString().contains(DAILY_QUOTA_MARKER);
        if (dailyQuotaUsedUp) {
            return new LlmException(message + " (daily quota used up; run again tomorrow, finished documents are kept)");
        }
        return new LlmException(message, RETRYABLE_STATUS_CODES.contains(statusCode), retryDelay(error), null);
    }

    /** Gemini states the wait in a RetryInfo detail, for example {@code "retryDelay": "36s"}. */
    private static Duration retryDelay(JsonNode error) {
        for (JsonNode detail : error.path("details")) {
            if (RETRY_INFO_TYPE.equals(detail.path("@type").asText())) {
                String delay = detail.path("retryDelay").asText().replace("s", "");
                try {
                    return Duration.ofMillis(Math.round(Double.parseDouble(delay) * 1000));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
