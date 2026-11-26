package com.theragenx.intake.llm;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** The committed recordings must never contain an API key. */
class CommittedFixturesTest {

    /** Google API keys start with AIza followed by 35 characters. */
    private static final Pattern GOOGLE_API_KEY = Pattern.compile("AIza[0-9A-Za-z_-]{35}");
    private static final Path FIXTURES = Path.of("fixtures");

    @Test
    void containNoApiKey() throws IOException {
        String keyInEnvironment = System.getenv("GEMINI_API_KEY");
        for (Path file : fixtureFiles()) {
            String content = Files.readString(file);
            assertThat(GOOGLE_API_KEY.matcher(content).find()).as(file + " contains a Google API key").isFalse();
            if (keyInEnvironment != null && !keyInEnvironment.isBlank()) {
                assertThat(content).as(file + " contains the API key").doesNotContain(keyInEnvironment);
            }
        }
    }

    private static List<Path> fixtureFiles() throws IOException {
        if (!Files.isDirectory(FIXTURES)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(FIXTURES)) {
            return files.filter(file -> file.toString().endsWith(".json")).toList();
        }
    }
}
