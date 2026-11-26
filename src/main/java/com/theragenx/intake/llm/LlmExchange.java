package com.theragenx.intake.llm;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One request to a model and its answer, as they went over the wire.
 *
 * @param model        provider and model, for example {@code gemini/gemini-2.5-flash}
 * @param request      the provider-specific request body (the API key travels in a header, never here)
 * @param response     the provider-specific response body
 * @param responseText the model's answer text, which the pipeline parses
 */
public record LlmExchange(String model, JsonNode request, JsonNode response, String responseText) {
}
