package com.theragenx.intake.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.catalog.FieldDefinition;
import com.theragenx.intake.catalog.FieldValueValidator;
import com.theragenx.intake.config.JsonMapperFactory;
import com.theragenx.intake.output.DocumentExtraction;
import com.theragenx.intake.output.DocumentFlag;
import com.theragenx.intake.output.FieldStatus;
import com.theragenx.intake.support.TestCatalogs;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentExtractorTest {

    private static final String TEXT = "A report about patient AB.";
    private static final String MODEL = "test/model";

    private final ObjectMapper json = JsonMapperFactory.create();
    private final Catalog catalog = TestCatalogs.small();
    private final DocumentExtractor extractor = new DocumentExtractor(new FieldResolver(new FieldValueValidator()), json);

    @Test
    void writesEveryCatalogFieldInCatalogOrderEvenWhenTheModelSkipsSome() {
        ObjectNode answer = answer(true, true, true, true, 1);

        DocumentExtraction result = extract(answer);

        assertThat(result.cases()).singleElement().satisfies(c ->
                assertThat(c.fields().keySet()).containsExactlyElementsOf(catalog.fields().stream().map(FieldDefinition::id).toList()));
        assertThat(result.flags()).isEmpty();
        assertThat(result.model()).isEqualTo(MODEL);
    }

    @Test
    void writesNoCaseWhenAMinimumElementIsMissing() {
        ObjectNode answer = answer(true, true, true, false, 1);

        DocumentExtraction result = extract(answer);

        assertThat(result.cases()).isEmpty();
        assertThat(result.flags()).containsExactly(DocumentFlag.NO_ADVERSE_EVENT);
    }

    @Test
    void flagsMultiplePatientsAndEmbeddedInstructions() {
        ObjectNode answer = answer(true, true, true, true, 2);
        answer.put("contains_embedded_instructions", true);

        DocumentExtraction result = extract(answer);

        assertThat(result.cases()).hasSize(2);
        assertThat(result.cases().get(1).caseIndex()).isEqualTo(1);
        assertThat(result.flags()).containsExactly(DocumentFlag.EMBEDDED_INSTRUCTIONS_IGNORED, DocumentFlag.MULTIPLE_PATIENTS);
    }

    @Test
    void flagsAnAnswerThatIsNotJson() {
        DocumentExtraction result = extractor.extract(catalog, "X", TEXT, MODEL, "not json");

        assertThat(result.cases()).isEmpty();
        assertThat(result.flags()).containsExactly(DocumentFlag.MODEL_RESPONSE_UNREADABLE);
    }

    @Test
    void ignoresFieldsTheCatalogDoesNotHave() {
        ObjectNode answer = answer(true, true, true, true, 1);
        ObjectNode fields = (ObjectNode) answer.get("cases").get(0).get("fields");
        fields.putObject("not_in_catalog").put("status", "extracted").put("value", "x");
        fields.putObject("code_name").put("status", "extracted").put("value", "AB").put("confidence", "high")
                .putArray("quotes").add("patient AB");

        DocumentExtraction result = extract(answer);

        assertThat(result.cases().getFirst().fields()).doesNotContainKey("not_in_catalog");
        assertThat(result.cases().getFirst().fields().get("code_name").status()).isEqualTo(FieldStatus.EXTRACTED);
    }

    private DocumentExtraction extract(ObjectNode answer) {
        return extractor.extract(catalog, "X", TEXT, MODEL, answer.toString());
    }

    private ObjectNode answer(boolean patient, boolean reporter, boolean drug, boolean event, int caseCount) {
        ObjectNode answer = json.createObjectNode();
        answer.put("contains_embedded_instructions", false);
        answer.putObject("minimum_elements")
                .put("identifiable_patient", patient)
                .put("reporter", reporter)
                .put("suspect_drug", drug)
                .put("adverse_event", event);
        ArrayNode cases = answer.putArray("cases");
        for (int i = 0; i < caseCount; i++) {
            ObjectNode modelCase = cases.addObject();
            modelCase.put("label", "Case " + i);
            modelCase.putObject("fields");
        }
        return answer;
    }
}
