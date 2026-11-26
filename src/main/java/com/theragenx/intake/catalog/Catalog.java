package com.theragenx.intake.catalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The field catalog, as read from {@code catalog/field_catalog.json}.
 * It is the only place where field ids, types, options and validation rules are defined.
 */
public record Catalog(
        String catalogVersion,
        String description,
        Map<String, String> statuses,
        List<String> confidenceLevels,
        List<Section> sections,
        List<FieldDefinition> fields) {

    public Catalog {
        statuses = statuses == null ? Map.of() : Map.copyOf(statuses);
        confidenceLevels = confidenceLevels == null ? List.of() : List.copyOf(confidenceLevels);
        sections = sections == null ? List.of() : List.copyOf(sections);
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public Optional<FieldDefinition> field(String fieldId) {
        return fields.stream().filter(field -> field.id().equals(fieldId)).findFirst();
    }
}
