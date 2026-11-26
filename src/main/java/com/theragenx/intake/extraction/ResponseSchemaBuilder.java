package com.theragenx.intake.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.catalog.FieldDefinition;
import com.theragenx.intake.output.Confidence;
import com.theragenx.intake.output.FieldStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Builds the JSON schema the model's answer must follow, from the catalog. Each field's value
 * gets the JSON type of its catalog type, and option fields list their codes, so the model
 * cannot answer outside the catalog's options.
 * <p>
 * Uses the OpenAPI schema subset (type, properties, required, items, enum, nullable) that
 * structured-output APIs accept. {@code propertyOrdering} asks for quotes before the value,
 * so the model finds its evidence before it commits to an answer; providers that do not know
 * the key ignore it. Property names must match the components of {@link ModelExtraction}.
 */
@Component
public class ResponseSchemaBuilder {

    private final ObjectMapper objectMapper;

    public ResponseSchemaBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode build(Catalog catalog) {
        ObjectNode root = object(List.of("contains_embedded_instructions", "minimum_elements", "cases"));
        ObjectNode properties = (ObjectNode) root.get("properties");
        properties.set("contains_embedded_instructions", type("boolean"));
        properties.set("minimum_elements", minimumElementsSchema());
        properties.set("cases", arrayOf(caseSchema(catalog)));
        return root;
    }

    private ObjectNode minimumElementsSchema() {
        List<String> elements = List.of("identifiable_patient", "reporter", "suspect_drug", "adverse_event");
        ObjectNode schema = object(elements);
        elements.forEach(element -> ((ObjectNode) schema.get("properties")).set(element, type("boolean")));
        return schema;
    }

    private ObjectNode caseSchema(Catalog catalog) {
        ObjectNode schema = object(List.of("label", "fields"));
        ObjectNode properties = (ObjectNode) schema.get("properties");
        properties.set("label", type("string"));

        List<String> fieldIds = catalog.fields().stream().map(FieldDefinition::id).toList();
        ObjectNode fields = object(fieldIds);
        catalog.fields().forEach(field -> ((ObjectNode) fields.get("properties")).set(field.id(), fieldSchema(field)));
        properties.set("fields", fields);
        return schema;
    }

    private ObjectNode fieldSchema(FieldDefinition field) {
        ObjectNode schema = object(List.of("quotes", "status", "value", "confidence", "derived", "note"));
        schema.set("required", strings(List.of("quotes", "status", "derived")));
        ObjectNode properties = (ObjectNode) schema.get("properties");
        properties.set("quotes", arrayOf(type("string")));
        properties.set("status", enumOf(Arrays.stream(FieldStatus.values()).map(FieldStatus::code).toList()));
        properties.set("value", valueSchema(field).put("nullable", true));
        properties.set("confidence", enumOf(Arrays.stream(Confidence.values()).map(Confidence::code).toList()).put("nullable", true));
        properties.set("derived", type("boolean"));
        properties.set("note", type("string").put("nullable", true));
        return schema;
    }

    private ObjectNode valueSchema(FieldDefinition field) {
        return switch (field.type()) {
            case STRING -> type("string");
            case DATE -> type("string").put("description", "ISO 8601 date, YYYY-MM-DD");
            case INTEGER -> type("integer");
            case NUMBER -> type("number");
            case CODED -> enumOf(field.optionCodes());
            case CODED_MULTI -> arrayOf(enumOf(field.optionCodes()));
            case STRING_LIST -> arrayOf(type("string"));
        };
    }

    /** An object whose listed properties are all required, in that order. */
    private ObjectNode object(List<String> propertyNames) {
        ObjectNode schema = type("object");
        schema.putObject("properties");
        schema.set("required", strings(propertyNames));
        schema.set("propertyOrdering", strings(propertyNames));
        return schema;
    }

    private ObjectNode arrayOf(ObjectNode items) {
        ObjectNode schema = type("array");
        schema.set("items", items);
        return schema;
    }

    private ObjectNode enumOf(List<String> values) {
        ObjectNode schema = type("string");
        schema.set("enum", strings(values));
        return schema;
    }

    private ObjectNode type(String type) {
        return objectMapper.createObjectNode().put("type", type);
    }

    private ArrayNode strings(List<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        values.forEach(array::add);
        return array;
    }
}
