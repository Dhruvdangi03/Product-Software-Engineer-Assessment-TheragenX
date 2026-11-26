package com.theragenx.intake.catalog;

import java.util.List;
import java.util.Objects;

/** One field of the catalog: what to extract, its type and how to validate it. */
public record FieldDefinition(
        String id,
        String label,
        String section,
        FieldType type,
        List<FieldOption> options,
        Validation validation,
        String guidance) {

    public FieldDefinition {
        Objects.requireNonNull(id, "field id");
        Objects.requireNonNull(type, "field type of " + id);
        options = options == null ? List.of() : List.copyOf(options);
        validation = validation == null ? Validation.NONE : validation;
    }

    public List<String> optionCodes() {
        return options.stream().map(FieldOption::code).toList();
    }
}
