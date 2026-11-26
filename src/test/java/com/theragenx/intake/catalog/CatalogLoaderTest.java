package com.theragenx.intake.catalog;

import com.theragenx.intake.config.JsonMapperFactory;
import com.theragenx.intake.support.TestCatalogs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogLoaderTest {

    private final CatalogLoader loader = new CatalogLoader(JsonMapperFactory.create());

    @Test
    void loadsTheCommittedCatalog() {
        Catalog catalog = loader.load(TestCatalogs.REAL_CATALOG);

        assertThat(catalog.fields()).isNotEmpty();
        assertThat(catalog.fields()).allSatisfy(field -> {
            assertThat(field.label()).isNotBlank();
            assertThat(catalog.sections()).anyMatch(section -> section.id().equals(field.section()));
        });
    }

    @Test
    void rejectsAFieldDefinedTwice(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("catalog.json");
        Files.writeString(file, """
                {"catalog_version": "x", "sections": [{"id": "s", "label": "S"}],
                 "fields": [{"id": "a", "label": "A", "section": "s", "type": "string"},
                            {"id": "a", "label": "A", "section": "s", "type": "string"}]}
                """);

        assertThatThrownBy(() -> loader.load(file)).isInstanceOf(CatalogException.class).hasMessageContaining("twice");
    }

    @Test
    void rejectsACodedFieldWithoutOptions(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("catalog.json");
        Files.writeString(file, """
                {"catalog_version": "x", "sections": [{"id": "s", "label": "S"}],
                 "fields": [{"id": "a", "label": "A", "section": "s", "type": "coded"}]}
                """);

        assertThatThrownBy(() -> loader.load(file)).isInstanceOf(CatalogException.class).hasMessageContaining("no options");
    }

    @Test
    void rejectsAnUnknownType(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("catalog.json");
        Files.writeString(file, """
                {"catalog_version": "x", "sections": [{"id": "s", "label": "S"}],
                 "fields": [{"id": "a", "label": "A", "section": "s", "type": "colour"}]}
                """);

        assertThatThrownBy(() -> loader.load(file)).isInstanceOf(CatalogException.class);
    }
}
