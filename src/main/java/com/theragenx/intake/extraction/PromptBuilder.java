package com.theragenx.intake.extraction;

import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.catalog.FieldDefinition;
import com.theragenx.intake.catalog.Section;
import com.theragenx.intake.catalog.Validation;
import com.theragenx.intake.document.SourceDocument;
import com.theragenx.intake.llm.LlmRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Builds the request for one document. The system prompt (rules and domain knowledge) lives in
 * {@code prompts/system-prompt.md}; the field list is generated from the catalog, so a new
 * catalog field is asked for without a code change.
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT_RESOURCE = "prompts/system-prompt.md";

    private final String systemPrompt;
    private final ResponseSchemaBuilder schemaBuilder;

    public PromptBuilder(ResponseSchemaBuilder schemaBuilder) {
        this.schemaBuilder = schemaBuilder;
        this.systemPrompt = readResource(SYSTEM_PROMPT_RESOURCE);
    }

    public LlmRequest build(Catalog catalog, SourceDocument document, String text) {
        String userPrompt = "# Fields to extract for each case\n\n"
                + describeFields(catalog)
                + "\n# Document " + document.id() + "\n\n"
                + "<document>\n" + text + "\n</document>\n";
        return new LlmRequest(systemPrompt, userPrompt, schemaBuilder.build(catalog));
    }

    private String describeFields(Catalog catalog) {
        return catalog.fields().stream()
                .map(field -> describeField(catalog, field))
                .collect(Collectors.joining("\n"));
    }

    private String describeField(Catalog catalog, FieldDefinition field) {
        StringBuilder text = new StringBuilder();
        text.append("## ").append(field.id()).append('\n');
        text.append("- label: ").append(field.label()).append('\n');
        text.append("- section: ").append(sectionLabel(catalog, field)).append('\n');
        text.append("- type: ").append(describeType(field)).append('\n');
        if (!field.options().isEmpty()) {
            String options = field.options().stream()
                    .map(option -> option.code() + " (" + option.label() + ")")
                    .collect(Collectors.joining(", "));
            text.append("- options: ").append(options).append('\n');
        }
        describeValidation(field.validation(), text);
        if (field.guidance() != null) {
            text.append("- guidance: ").append(field.guidance()).append('\n');
        }
        return text.toString();
    }

    private static String describeType(FieldDefinition field) {
        return switch (field.type()) {
            case STRING -> "text";
            case INTEGER -> "whole number";
            case NUMBER -> "number";
            case DATE -> "ISO 8601 date, YYYY-MM-DD";
            case CODED -> "exactly one option code";
            case CODED_MULTI -> "list of option codes";
            case STRING_LIST -> "list of short texts";
        };
    }

    private static void describeValidation(Validation validation, StringBuilder text) {
        if (validation.pattern() != null) {
            text.append("- must match the regular expression: ").append(validation.pattern()).append('\n');
        }
        if (validation.min() != null) {
            text.append("- minimum: ").append(formatNumber(validation.min())).append('\n');
        }
        if (validation.max() != null) {
            text.append("- maximum: ").append(formatNumber(validation.max())).append('\n');
        }
    }

    private static String sectionLabel(Catalog catalog, FieldDefinition field) {
        return catalog.sections().stream()
                .filter(section -> section.id().equals(field.section()))
                .map(Section::label)
                .findFirst()
                .orElse(field.section());
    }

    private static String formatNumber(double number) {
        return number == Math.rint(number) ? String.valueOf((long) number) : String.valueOf(number);
    }

    private static String readResource(String name) {
        try {
            return new ClassPathResource(name).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read prompt resource " + name, e);
        }
    }
}
