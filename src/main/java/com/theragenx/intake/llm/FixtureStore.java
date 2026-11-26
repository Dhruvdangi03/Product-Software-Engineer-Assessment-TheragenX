package com.theragenx.intake.llm;

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
 * Reads and writes recorded model exchanges.
 * <p>
 * Fixtures are keyed by document id, not by a hash of the prompt. The prompt is built from the
 * catalog, so a changed catalog changes the prompt; keying by document lets replay still find
 * the recording and check the recorded values against the new catalog.
 */
@Component
public class FixtureStore {

    static final String REDACTED = "[REDACTED]";

    private final Path fixturesDir;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    @Autowired
    public FixtureStore(IntakeProperties properties, ObjectMapper objectMapper) {
        this(properties.fixturesDir(), properties.llm().apiKey(), objectMapper);
    }

    public FixtureStore(Path fixturesDir, String apiKey, ObjectMapper objectMapper) {
        this.fixturesDir = fixturesDir;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    public Optional<Fixture> find(String documentId) {
        Path file = fileFor(documentId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), Fixture.class));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read fixture " + file, e);
        }
    }

    public Fixture load(String documentId) {
        return find(documentId).orElseThrow(() -> new FixtureNotFoundException(documentId, fileFor(documentId).toString()));
    }

    /** Writes the fixture. The key is not in the request body, but we remove it anywhere it might appear. */
    public void save(Fixture fixture) {
        Path file = fileFor(fixture.documentId());
        try {
            String json = objectMapper.writeValueAsString(fixture);
            if (apiKey != null && !apiKey.isBlank()) {
                json = json.replace(apiKey, REDACTED);
            }
            Files.createDirectories(fixturesDir);
            Files.writeString(file, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write fixture " + file, e);
        }
    }

    private Path fileFor(String documentId) {
        return fixturesDir.resolve(documentId + ".json");
    }
}
