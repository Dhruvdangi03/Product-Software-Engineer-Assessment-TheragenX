package com.theragenx.intake.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

/**
 * The reviewer's decisions for one document, stored apart from the model output in
 * {@code reviews/<documentId>.json}. Keeping them apart means {@code out/} stays exactly what
 * the pipeline produced, and model values and reviewer values can never be confused.
 *
 * @param cases decisions by case index, then by field id
 */
public record DocumentReview(Map<Integer, Map<String, FieldReview>> cases) {

    public static DocumentReview empty() {
        return new DocumentReview(new TreeMap<>());
    }

    public DocumentReview {
        cases = cases == null ? new TreeMap<>() : new TreeMap<>(cases);
    }

    public enum Decision {
        @JsonProperty("accepted") ACCEPTED,
        @JsonProperty("rejected") REJECTED,
        @JsonProperty("edited") EDITED
    }

    /**
     * @param value the reviewer's value; only present for {@link Decision#EDITED}
     */
    public record FieldReview(Decision decision, JsonNode value, Instant reviewedAt) {
    }

    public int reviewedFieldCount() {
        return cases.values().stream().mapToInt(Map::size).sum();
    }
}
