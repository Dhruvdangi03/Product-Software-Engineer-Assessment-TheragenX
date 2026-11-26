package com.theragenx.intake.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theragenx.intake.support.TempDirs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** What replay.sh promises: the fixtures, documents and catalog rebuild exactly the committed out/. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ReplayReproducesCommittedOutputTest {

    private static final Path COMMITTED_OUTPUT = Path.of("out");
    private static final Path REPLAY_OUTPUT = TempDirs.create("replay-out");

    @DynamicPropertySource
    static void replayIntoATemporaryFolder(DynamicPropertyRegistry registry) {
        registry.add("intake.llm.mode", () -> "replay");
        registry.add("intake.output-dir", REPLAY_OUTPUT::toString);
    }

    @Autowired
    private ExtractionPipeline pipeline;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void replayWritesTheSameFilesWithTheSameValues() throws IOException {
        pipeline.run();

        List<String> committed = jsonFiles(COMMITTED_OUTPUT);
        assertThat(jsonFiles(REPLAY_OUTPUT)).containsExactlyInAnyOrderElementsOf(committed);
        for (String file : committed) {
            assertThat(objectMapper.readTree(REPLAY_OUTPUT.resolve(file).toFile()))
                    .as(file)
                    .isEqualTo(objectMapper.readTree(COMMITTED_OUTPUT.resolve(file).toFile()));
        }
    }

    private static List<String> jsonFiles(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(file -> file.getFileName().toString()).filter(name -> name.endsWith(".json")).toList();
        }
    }
}
