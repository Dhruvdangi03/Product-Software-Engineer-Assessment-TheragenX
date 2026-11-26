package com.theragenx.intake.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.theragenx.intake.config.JsonMapperFactory;
import com.theragenx.intake.output.DocumentExtraction;
import com.theragenx.intake.output.ExtractedCase;
import com.theragenx.intake.output.FieldResult;
import com.theragenx.intake.output.FieldStatus;
import com.theragenx.intake.support.TempDirs;
import com.theragenx.intake.support.TestCatalogs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The catalog change from the follow-up session: replay runs from the same recordings against a
 * changed catalog. A new field and every value the new rules reject come out unknown with a note.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ReplayWithChangedCatalogTest {

    private static final String ADDED_FIELD = "added_in_test";
    private static final Path WORK_DIR = TempDirs.create("changed-catalog");
    private static final Path CHANGED_CATALOG = WORK_DIR.resolve("catalog.json");
    private static final List<String> TIGHTENED_FIELDS = writeChangedCatalog();

    @DynamicPropertySource
    static void useTheChangedCatalog(DynamicPropertyRegistry registry) {
        registry.add("intake.llm.mode", () -> "replay");
        registry.add("intake.catalog-path", CHANGED_CATALOG::toString);
        registry.add("intake.output-dir", () -> WORK_DIR.resolve("out").toString());
    }

    @Autowired
    private ExtractionPipeline pipeline;

    @Test
    void newAndInvalidatedFieldsBecomeUnknownWithAReason() {
        List<ExtractedCase> cases = pipeline.run().stream().flatMap(document -> document.cases().stream()).toList();

        assertThat(cases).isNotEmpty();
        for (ExtractedCase extractedCase : cases) {
            FieldResult added = extractedCase.fields().get(ADDED_FIELD);
            assertThat(added.status()).isEqualTo(FieldStatus.UNKNOWN);
            assertThat(added.note()).contains("No model answer");
            for (String tightened : TIGHTENED_FIELDS) {
                assertThat(extractedCase.fields().get(tightened).status()).isNotEqualTo(FieldStatus.EXTRACTED);
            }
        }
        assertThat(cases).anySatisfy(c -> assertThat(c.fields().values()).anyMatch(f -> f.status() == FieldStatus.EXTRACTED));
    }

    /** Adds a field and replaces every pattern with one no recorded value can match. Returns the changed field ids. */
    private static List<String> writeChangedCatalog() {
        ObjectMapper json = JsonMapperFactory.create();
        try {
            ObjectNode catalog = (ObjectNode) json.readTree(TestCatalogs.REAL_CATALOG.toFile());
            List<String> tightened = new ArrayList<>();
            catalog.withArray("/fields").forEach(field -> {
                if (field.path("validation").has("pattern")) {
                    ((ObjectNode) field.get("validation")).put("pattern", "^never$");
                    tightened.add(field.get("id").asText());
                }
            });
            String firstSection = catalog.get("sections").get(0).get("id").asText();
            catalog.withArray("/fields").addObject()
                    .put("id", ADDED_FIELD).put("label", "Added in test").put("section", firstSection).put("type", "string");
            json.writeValue(CHANGED_CATALOG.toFile(), catalog);
            return tightened;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
