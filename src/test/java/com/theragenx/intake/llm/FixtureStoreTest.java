package com.theragenx.intake.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theragenx.intake.config.JsonMapperFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixtureStoreTest {

    private final ObjectMapper json = JsonMapperFactory.create();

    @Test
    void savesAndLoadsAnExchangeWithTheKeyRemoved(@TempDir Path dir) throws Exception {
        FixtureStore store = new FixtureStore(dir, "secret-key-123", json);
        LlmExchange exchange = new LlmExchange("test/model",
                json.readTree("{\"url\": \"https://x?key=secret-key-123\"}"), json.readTree("{}"), "{\"ok\": true}");

        store.save(new Fixture("D01", Instant.parse("2026-01-01T00:00:00Z"), exchange));

        assertThat(Files.readString(dir.resolve("D01.json"))).doesNotContain("secret-key-123").contains(FixtureStore.REDACTED);
        assertThat(store.load("D01").exchange().responseText()).isEqualTo("{\"ok\": true}");
    }

    @Test
    void replayWithoutARecordingFailsWithAClearMessage(@TempDir Path dir) {
        FixtureStore store = new FixtureStore(dir, null, json);

        assertThat(store.find("D09")).isEmpty();
        assertThatThrownBy(() -> store.load("D09")).isInstanceOf(FixtureNotFoundException.class).hasMessageContaining("D09");
    }
}
