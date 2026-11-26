package com.theragenx.intake.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks a value against the type and validation rules of its catalog field.
 * Used for model output, for reviewer edits and when a changed catalog is replayed.
 */
@Component
public class FieldValueValidator {

    /**
     * In a multi-select field, this option means "none of the others apply", so it must stand alone.
     * The exercise brief defines this rule for the seriousness options; we apply it to any coded_multi field.
     */
    static final String EXCLUSIVE_OPTION_CODE = "none";

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    public ValueCheck check(FieldDefinition field, JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return new ValueCheck.Invalid("no value given");
        }
        return switch (field.type()) {
            case STRING -> checkString(field, value);
            case INTEGER -> checkInteger(field, value);
            case NUMBER -> checkNumber(field, value);
            case DATE -> checkDate(value);
            case CODED -> checkCoded(field, value);
            case CODED_MULTI -> checkCodedMulti(field, value);
            case STRING_LIST -> checkStringList(value);
        };
    }

    private ValueCheck checkString(FieldDefinition field, JsonNode value) {
        if (!value.isTextual() || value.asText().isBlank()) {
            return new ValueCheck.Invalid("a non-empty text value is expected");
        }
        String text = value.asText().trim();
        String pattern = field.validation().pattern();
        if (pattern != null && !Pattern.compile(pattern).matcher(text).matches()) {
            return new ValueCheck.Invalid("'" + text + "' does not match the pattern " + pattern);
        }
        return new ValueCheck.Valid(TextNode.valueOf(text));
    }

    private ValueCheck checkInteger(FieldDefinition field, JsonNode value) {
        if (!value.isNumber() || value.asDouble() != Math.rint(value.asDouble())) {
            return new ValueCheck.Invalid("a whole number is expected, got " + value);
        }
        return checkRange(field, value.asDouble(), JSON.numberNode(value.asLong()));
    }

    private ValueCheck checkNumber(FieldDefinition field, JsonNode value) {
        if (!value.isNumber() || !Double.isFinite(value.asDouble())) {
            return new ValueCheck.Invalid("a number is expected, got " + value);
        }
        return checkRange(field, value.asDouble(), value);
    }

    private ValueCheck checkRange(FieldDefinition field, double number, JsonNode normalised) {
        Validation validation = field.validation();
        if (validation.min() != null && number < validation.min()) {
            return new ValueCheck.Invalid(number + " is below the minimum " + validation.min());
        }
        if (validation.max() != null && number > validation.max()) {
            return new ValueCheck.Invalid(number + " is above the maximum " + validation.max());
        }
        return new ValueCheck.Valid(normalised);
    }

    private ValueCheck checkDate(JsonNode value) {
        String text = value.asText().trim();
        try {
            LocalDate.parse(text, ISO_DATE);
            return new ValueCheck.Valid(TextNode.valueOf(text));
        } catch (DateTimeParseException e) {
            return new ValueCheck.Invalid("'" + text + "' is not a valid ISO date (YYYY-MM-DD)");
        }
    }

    private ValueCheck checkCoded(FieldDefinition field, JsonNode value) {
        String code = value.asText();
        if (!value.isTextual() || !field.optionCodes().contains(code)) {
            return new ValueCheck.Invalid("'" + code + "' is not one of the options " + field.optionCodes());
        }
        return new ValueCheck.Valid(value);
    }

    private ValueCheck checkCodedMulti(FieldDefinition field, JsonNode value) {
        if (!value.isArray() || value.isEmpty()) {
            return new ValueCheck.Invalid("a non-empty list of option codes is expected");
        }
        Set<String> codes = new LinkedHashSet<>();
        for (JsonNode item : value) {
            if (!item.isTextual() || !field.optionCodes().contains(item.asText())) {
                return new ValueCheck.Invalid("'" + item.asText() + "' is not one of the options " + field.optionCodes());
            }
            codes.add(item.asText());
        }
        if (codes.contains(EXCLUSIVE_OPTION_CODE) && codes.size() > 1) {
            return new ValueCheck.Invalid("'" + EXCLUSIVE_OPTION_CODE + "' cannot be combined with other options");
        }
        return new ValueCheck.Valid(toArray(new ArrayList<>(codes)));
    }

    private ValueCheck checkStringList(JsonNode value) {
        if (!value.isArray()) {
            return new ValueCheck.Invalid("a list of text values is expected");
        }
        Set<String> items = new LinkedHashSet<>();
        for (JsonNode item : value) {
            if (!item.isTextual()) {
                return new ValueCheck.Invalid("every list item must be text, got " + item);
            }
            if (!item.asText().isBlank()) {
                items.add(item.asText().trim());
            }
        }
        return new ValueCheck.Valid(toArray(new ArrayList<>(items)));
    }

    private static ArrayNode toArray(List<String> items) {
        ArrayNode array = JSON.arrayNode();
        items.forEach(array::add);
        return array;
    }
}
