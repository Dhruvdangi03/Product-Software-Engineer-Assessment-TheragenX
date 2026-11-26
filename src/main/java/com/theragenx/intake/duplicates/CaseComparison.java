package com.theragenx.intake.duplicates;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Field-by-field comparison of two cases, in catalog order. */
public record CaseComparison(List<FieldComparison> fields) {

    public enum Outcome {
        /** Both cases have a value and the values are the same. */
        @JsonProperty("same") SAME,
        /** Both cases have a value and the values differ. */
        @JsonProperty("different") DIFFERENT,
        /** At least one case has no value, so the field says nothing either way. */
        @JsonProperty("not_compared") NOT_COMPARED
    }

    public record FieldComparison(String fieldId, String label, String section, Outcome outcome) {
    }

    public List<FieldComparison> inSections(List<String> sections) {
        return fields.stream().filter(field -> sections.contains(field.section())).toList();
    }

    public List<FieldComparison> outsideSections(List<String> sections) {
        return fields.stream().filter(field -> !sections.contains(field.section())).toList();
    }
}
