package com.theragenx.intake.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

/** How sure the model is of an extracted value. Declared from least to most confident. */
public enum Confidence {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String code;

    Confidence(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static Confidence fromCode(String code) {
        return parse(code).orElseThrow(() -> new IllegalArgumentException("Unknown confidence: " + code));
    }

    public static Optional<Confidence> parse(String code) {
        return Arrays.stream(values()).filter(confidence -> confidence.code.equals(code)).findFirst();
    }

    /** This confidence, lowered to {@code ceiling} when it is above it. */
    public Confidence atMost(Confidence ceiling) {
        return compareTo(ceiling) > 0 ? ceiling : this;
    }
}
