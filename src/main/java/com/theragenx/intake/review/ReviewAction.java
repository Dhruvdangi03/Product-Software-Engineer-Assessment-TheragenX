package com.theragenx.intake.review;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/** What a reviewer does with one field. */
public enum ReviewAction {
    /** The model's value (or unknown/conflict status) is right. */
    ACCEPT("accept"),
    /** The model's value is wrong, and the reviewer has no replacement. */
    REJECT("reject"),
    /** The reviewer replaces the value with their own. */
    EDIT("edit"),
    /** Undo the review of this field. */
    RESET("reset");

    private final String code;

    ReviewAction(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static ReviewAction fromCode(String code) {
        return Arrays.stream(values())
                .filter(action -> action.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new InvalidReviewException("Unknown review action: " + code));
    }
}
