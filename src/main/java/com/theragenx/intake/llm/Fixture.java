package com.theragenx.intake.llm;

import java.time.Instant;

/**
 * A recorded model exchange for one document, stored as {@code fixtures/<documentId>.json}.
 *
 * @param documentId the document the exchange belongs to
 * @param recordedAt when it was recorded
 * @param exchange   the request as sent and the response as received, with the API key removed
 */
public record Fixture(String documentId, Instant recordedAt, LlmExchange exchange) {
}
