package com.theragenx.intake.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theragenx.intake.config.IntakeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The {@code out/} folder: the pipeline writes it, the API reads it.
 * Every file is checked against its schema before it is written.
 */
@Component
public class OutputStore {

    static final String DUPLICATES_FILE = "duplicates.json";

    private final Path outputDir;
    private final OutputSchemaValidator schemaValidator;
    private final ObjectMapper objectMapper;

    @Autowired
    public OutputStore(IntakeProperties properties, OutputSchemaValidator schemaValidator, ObjectMapper objectMapper) {
        this(properties.outputDir(), schemaValidator, objectMapper);
    }

    public OutputStore(Path outputDir, OutputSchemaValidator schemaValidator, ObjectMapper objectMapper) {
        this.outputDir = outputDir;
        this.schemaValidator = schemaValidator;
        this.objectMapper = objectMapper;
    }

    public void write(DocumentExtraction extraction) {
        JsonNode json = objectMapper.valueToTree(extraction);
        schemaValidator.checkExtraction(extraction.documentId(), json);
        writeJson(outputDir.resolve(extraction.documentId() + ".json"), json);
    }

    public void writeDuplicates(DuplicateReport report) {
        JsonNode json = objectMapper.valueToTree(report);
        schemaValidator.checkDuplicates(json);
        writeJson(outputDir.resolve(DUPLICATES_FILE), json);
    }

    public Optional<DocumentExtraction> find(String documentId) {
        return read(outputDir.resolve(documentId + ".json"), DocumentExtraction.class);
    }

    public DuplicateReport findDuplicates() {
        return read(outputDir.resolve(DUPLICATES_FILE), DuplicateReport.class).orElse(DuplicateReport.EMPTY);
    }

    private <T> Optional<T> read(Path file, Class<T> type) {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), type));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read output file " + file, e);
        }
    }

    private void writeJson(Path file, JsonNode json) {
        try {
            Files.createDirectories(outputDir);
            Files.writeString(file, objectMapper.writeValueAsString(json) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write output file " + file, e);
        }
    }
}
