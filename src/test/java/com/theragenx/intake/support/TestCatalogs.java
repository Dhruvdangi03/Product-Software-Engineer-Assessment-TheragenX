package com.theragenx.intake.support;

import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.catalog.CatalogLoader;
import com.theragenx.intake.catalog.FieldDefinition;
import com.theragenx.intake.catalog.FieldOption;
import com.theragenx.intake.catalog.FieldType;
import com.theragenx.intake.catalog.Section;
import com.theragenx.intake.catalog.Validation;
import com.theragenx.intake.config.JsonMapperFactory;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Catalogs for tests. Most tests use {@link #small()}, a made-up catalog with one field of every
 * type, so they show that the code works for any catalog and never relies on the real field ids.
 */
public final class TestCatalogs {

    public static final Path REAL_CATALOG = Path.of("catalog/field_catalog.json");

    private TestCatalogs() {
    }

    public static Catalog real() {
        return new CatalogLoader(JsonMapperFactory.create()).load(REAL_CATALOG);
    }

    public static Catalog small() {
        return new Catalog("test-1", "Test catalog", Map.of(), List.of("high", "medium", "low"),
                List.of(new Section("person", "Person"), new Section("visit", "Visit")),
                List.of(
                        field("code_name", "person", FieldType.STRING, List.of(), new Validation("^[A-Z]{2}$", null, null)),
                        field("years", "person", FieldType.INTEGER, List.of(), new Validation(null, 0.0, 120.0)),
                        field("weight", "visit", FieldType.NUMBER, List.of(), new Validation(null, 0.0, null)),
                        field("visit_date", "visit", FieldType.DATE, List.of(), null),
                        field("colour", "visit", FieldType.CODED, options("red", "blue"), null),
                        field("tags", "visit", FieldType.CODED_MULTI, options("a", "b", "none"), null),
                        field("notes", "visit", FieldType.STRING_LIST, List.of(), null)));
    }

    public static FieldDefinition field(Catalog catalog, String id) {
        return catalog.field(id).orElseThrow();
    }

    private static FieldDefinition field(String id, String section, FieldType type, List<FieldOption> options, Validation validation) {
        return new FieldDefinition(id, id.replace('_', ' '), section, type, options, validation, null);
    }

    private static List<FieldOption> options(String... codes) {
        return Arrays.stream(codes).map(code -> new FieldOption(code, code.toUpperCase())).toList();
    }
}
