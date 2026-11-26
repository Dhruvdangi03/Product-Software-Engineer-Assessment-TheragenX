package com.theragenx.intake.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/** Document-level observations for the reviewer. The pipeline sets these; the model cannot. */
public enum DocumentFlag {
    UNSUPPORTED_FORMAT("unsupported_format"),
    NO_ADVERSE_EVENT("no_adverse_event"),
    NO_IDENTIFIABLE_PATIENT("no_identifiable_patient"),
    NO_REPORTER("no_reporter"),
    NO_SUSPECT_DRUG("no_suspect_drug"),
    MULTIPLE_PATIENTS("multiple_patients"),
    POSSIBLE_DUPLICATE("possible_duplicate"),
    EMBEDDED_INSTRUCTIONS_IGNORED("embedded_instructions_ignored"),
    MODEL_RESPONSE_UNREADABLE("model_response_unreadable");

    private final String code;

    DocumentFlag(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static DocumentFlag fromCode(String code) {
        return Arrays.stream(values())
                .filter(flag -> flag.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown document flag: " + code));
    }
}
