package com.theragenx.intake.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.catalog.FieldValueValidator;
import com.theragenx.intake.config.JsonMapperFactory;
import com.theragenx.intake.extraction.ModelExtraction.ModelField;
import com.theragenx.intake.output.Confidence;
import com.theragenx.intake.output.FieldResult;
import com.theragenx.intake.output.FieldStatus;
import com.theragenx.intake.support.TestCatalogs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FieldResolverTest {

    private static final String TEXT = "Patient AB, 54 years old, colour red. Two weeks later a rash appeared.";

    private final ObjectMapper json = JsonMapperFactory.create();
    private final Catalog catalog = TestCatalogs.small();
    private final FieldResolver resolver = new FieldResolver(new FieldValueValidator());
    private final QuoteLocator locator = new QuoteLocator(TEXT);

    @Test
    void keepsAValidGroundedValue() {
        FieldResult result = resolve("years", answer("extracted", "54", "high", false, "54 years old"));

        assertThat(result.status()).isEqualTo(FieldStatus.EXTRACTED);
        assertThat(result.value().asInt()).isEqualTo(54);
        assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(result.provenance()).singleElement().satisfies(p -> assertThat(p.quote()).isEqualTo("54 years old"));
    }

    @Test
    void rejectsAValueOutsideTheCatalogAsUnknownWithAReason() {
        FieldResult result = resolve("colour", answer("extracted", "\"green\"", "high", false, "colour red"));

        assertThat(result.status()).isEqualTo(FieldStatus.UNKNOWN);
        assertThat(result.value()).isNull();
        assertThat(result.note()).contains("rejected").contains("green");
    }

    @Test
    void rejectsAValueWhoseQuotesAreNotInTheDocument() {
        FieldResult result = resolve("years", answer("extracted", "54", "high", false, "aged fifty-four"));

        assertThat(result.status()).isEqualTo(FieldStatus.UNKNOWN);
        assertThat(result.note()).contains("none of its quotes");
    }

    @Test
    void capsTheConfidenceOfADerivedValue() {
        FieldResult result = resolve("visit_date", answer("extracted", "\"2026-03-16\"", "high", true, "Two weeks later"));

        assertThat(result.confidence()).isEqualTo(Confidence.MEDIUM);
    }

    @Test
    void keepsAConflictWithoutValueButWithItsQuotes() {
        ModelField conflict = new ModelField(List.of("54 years old"), "conflict", null, null, false, "54 or 45");

        FieldResult result = resolve("years", conflict);

        assertThat(result.status()).isEqualTo(FieldStatus.CONFLICT);
        assertThat(result.value()).isNull();
        assertThat(result.note()).isEqualTo("54 or 45");
        assertThat(result.provenance()).hasSize(1);
    }

    @Test
    void treatsAFieldTheModelDidNotAnswerAsUnknownAndSaysWhy() {
        FieldResult result = resolve("years", null);

        assertThat(result.status()).isEqualTo(FieldStatus.UNKNOWN);
        assertThat(result.note()).contains("No model answer");
    }

    @Test
    void rejectsAnInvalidStatus() {
        FieldResult result = resolve("years", answer("guessed", "54", "high", false, "54 years old"));

        assertThat(result.status()).isEqualTo(FieldStatus.UNKNOWN);
        assertThat(result.note()).contains("invalid status");
    }

    private FieldResult resolve(String fieldId, ModelField answer) {
        return resolver.resolve(TestCatalogs.field(catalog, fieldId), answer, locator);
    }

    private ModelField answer(String status, String value, String confidence, boolean derived, String quote) {
        try {
            JsonNode node = json.readTree(value);
            return new ModelField(List.of(quote), status, node, confidence, derived, null);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
