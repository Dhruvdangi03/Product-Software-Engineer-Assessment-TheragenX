package com.theragenx.intake.llm;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A provider-neutral request.
 *
 * @param systemPrompt   the instructions to the model
 * @param userPrompt     the field list and the document
 * @param responseSchema the JSON shape the answer must have, in the OpenAPI schema subset
 *                       (type, properties, required, items, enum, nullable, description)
 */
public record LlmRequest(String systemPrompt, String userPrompt, JsonNode responseSchema) {
}
