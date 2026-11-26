package com.theragenx.intake.catalog;

import com.fasterxml.jackson.databind.JsonNode;

/** The result of checking one value against its catalog field. */
public sealed interface ValueCheck {

    /** The value passed; {@code value} is its normalised form (trimmed, duplicates removed). */
    record Valid(JsonNode value) implements ValueCheck {
    }

    /** The value failed; {@code reason} says which rule it broke. */
    record Invalid(String reason) implements ValueCheck {
    }
}
