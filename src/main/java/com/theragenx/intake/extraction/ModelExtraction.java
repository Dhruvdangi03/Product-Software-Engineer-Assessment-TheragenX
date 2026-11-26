package com.theragenx.intake.extraction;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * The model's answer for one document, as parsed from its JSON. The shape is defined by
 * {@link ResponseSchemaBuilder}; property names are the snake_case form of the record components.
 * Nothing here is trusted yet: {@link FieldResolver} checks every value before it reaches the output.
 */
public record ModelExtraction(
        boolean containsEmbeddedInstructions,
        MinimumElements minimumElements,
        List<ModelCase> cases) {

    public ModelExtraction {
        cases = cases == null ? List.of() : cases;
    }

    /** The four elements that make a report a case. A missing element means no case. */
    public record MinimumElements(
            boolean identifiablePatient,
            boolean reporter,
            boolean suspectDrug,
            boolean adverseEvent) {
    }

    public record ModelCase(String label, Map<String, ModelField> fields) {

        public ModelCase {
            fields = fields == null ? Map.of() : fields;
        }
    }

    public record ModelField(
            List<String> quotes,
            String status,
            JsonNode value,
            String confidence,
            boolean derived,
            String note) {

        public ModelField {
            quotes = quotes == null ? List.of() : quotes;
        }
    }
}
