package com.theragenx.intake.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.theragenx.intake.config.IntakeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Checks output files against the JSON schemas shipped with the exercise before they are written.
 * A failure here is a bug in the pipeline, so it stops the run instead of writing a bad file.
 */
@Component
public class OutputSchemaValidator {

    private static final String EXTRACTION_SCHEMA_FILE = "extraction.schema.json";
    private static final String DUPLICATES_SCHEMA_FILE = "duplicates.schema.json";

    private final JsonSchema extractionSchema;
    private final JsonSchema duplicatesSchema;

    @Autowired
    public OutputSchemaValidator(IntakeProperties properties) {
        this(properties.schemaDir());
    }

    public OutputSchemaValidator(Path schemaDir) {
        this.extractionSchema = load(schemaDir.resolve(EXTRACTION_SCHEMA_FILE));
        this.duplicatesSchema = load(schemaDir.resolve(DUPLICATES_SCHEMA_FILE));
    }

    public void checkExtraction(String documentId, JsonNode output) {
        check(extractionSchema, output, "Output for " + documentId);
    }

    public void checkDuplicates(JsonNode output) {
        check(duplicatesSchema, output, "Duplicate report");
    }

    private static void check(JsonSchema schema, JsonNode output, String what) {
        Set<ValidationMessage> problems = schema.validate(output);
        if (!problems.isEmpty()) {
            String details = problems.stream().map(ValidationMessage::getMessage).collect(Collectors.joining("; "));
            throw new IllegalStateException(what + " does not match its schema: " + details);
        }
    }

    private static JsonSchema load(Path schemaFile) {
        try (InputStream in = Files.newInputStream(schemaFile)) {
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read schema " + schemaFile, e);
        }
    }
}
