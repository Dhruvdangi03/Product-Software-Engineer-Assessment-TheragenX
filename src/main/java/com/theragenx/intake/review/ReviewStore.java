package com.theragenx.intake.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theragenx.intake.config.IntakeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads and writes {@code reviews/<documentId>.json}. A JSON file per document is enough; no database is needed. */
@Component
public class ReviewStore {

    private final Path reviewsDir;
    private final ObjectMapper objectMapper;

    @Autowired
    public ReviewStore(IntakeProperties properties, ObjectMapper objectMapper) {
        this(properties.reviewsDir(), objectMapper);
    }

    public ReviewStore(Path reviewsDir, ObjectMapper objectMapper) {
        this.reviewsDir = reviewsDir;
        this.objectMapper = objectMapper;
    }

    public DocumentReview load(String documentId) {
        Path file = fileFor(documentId);
        if (!Files.exists(file)) {
            return DocumentReview.empty();
        }
        try {
            return objectMapper.readValue(file.toFile(), DocumentReview.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read review file " + file, e);
        }
    }

    public void save(String documentId, DocumentReview review) {
        Path file = fileFor(documentId);
        try {
            Files.createDirectories(reviewsDir);
            Files.writeString(file, objectMapper.writeValueAsString(review), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write review file " + file, e);
        }
    }

    private Path fileFor(String documentId) {
        return reviewsDir.resolve(documentId + ".json");
    }
}
