package com.theragenx.intake.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum FieldStatus {

    /** The document states a value; value, confidence and provenance are present. */
    EXTRACTED("extracted"),

    /** The document does not state it, or it could not be determined. Never a guess. */
    UNKNOWN("unknown"),

    /** The document states two or more incompatible values. The candidates are in the note. */
    CONFLICT("conflict");

    private final String code;

    FieldStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static FieldStatus fromCode(String code) {
        return parse(code).orElseThrow(() -> new IllegalArgumentException("Unknown field status: " + code));
    }

    public static Optional<FieldStatus> parse(String code) {
        return Arrays.stream(values()).filter(status -> status.code.equals(code)).findFirst();
    }
}
