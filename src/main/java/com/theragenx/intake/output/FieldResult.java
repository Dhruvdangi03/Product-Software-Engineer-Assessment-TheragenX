package com.theragenx.intake.output;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * The result for one catalog field in one case. Use the factory methods; they make sure an
 * extracted value always has confidence and provenance, and the other statuses have no value.
 */
public record FieldResult(
        FieldStatus status,
        JsonNode value,
        Confidence confidence,
        List<Provenance> provenance,
        String note) {

    public static FieldResult extracted(JsonNode value, Confidence confidence, List<Provenance> provenance, String note) {
        if (provenance.isEmpty()) {
            throw new IllegalArgumentException("An extracted value needs at least one quote");
        }
        return new FieldResult(FieldStatus.EXTRACTED, value, confidence, List.copyOf(provenance), note);
    }

    public static FieldResult unknown(String note) {
        return new FieldResult(FieldStatus.UNKNOWN, null, null, null, note);
    }

    /** A conflict has no value; its quotes, when found, show the reviewer the contradicting passages. */
    public static FieldResult conflict(String note, List<Provenance> provenance) {
        return new FieldResult(FieldStatus.CONFLICT, null, null, provenance.isEmpty() ? null : List.copyOf(provenance), note);
    }
}
