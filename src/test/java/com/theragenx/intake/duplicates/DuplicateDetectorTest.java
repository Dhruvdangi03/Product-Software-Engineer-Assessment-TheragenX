package com.theragenx.intake.duplicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.config.IntakeProperties;
import com.theragenx.intake.output.Confidence;
import com.theragenx.intake.output.DocumentExtraction;
import com.theragenx.intake.output.DuplicateReport;
import com.theragenx.intake.output.ExtractedCase;
import com.theragenx.intake.output.FieldResult;
import com.theragenx.intake.output.Provenance;
import com.theragenx.intake.support.TestCatalogs;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateDetectorTest {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private final Catalog catalog = TestCatalogs.small();
    /** In the test catalog, "person" identifies the patient and "visit" holds the evidence. */
    private final DuplicateDetector detector = new DuplicateDetector(
            new IntakeProperties.Duplicates(List.of("person"), List.of(), 2, 0.5));

    @Test
    void findsTwoReportsOfTheSamePatientAndEvent() {
        DocumentExtraction a = document("A", person("AB", 81), visit("2026-02-13", "red"));
        DocumentExtraction b = document("B", person("AB", 81), visit("2026-02-13", "blue"));

        DuplicateReport report = detector.detect(catalog, List.of(a, b));

        assertThat(report.groups()).singleElement().satisfies(group -> {
            assertThat(group.documentIds()).containsExactly("A", "B");
            assertThat(group.confidence()).isEqualTo(Confidence.MEDIUM);
        });
    }

    @Test
    void differentPatientsWithTheSameEventAreNotDuplicates() {
        DocumentExtraction a = document("A", person("AB", 54), visit("2026-02-13", "red"));
        DocumentExtraction b = document("B", person("CD", 54), visit("2026-02-13", "red"));

        assertThat(detector.detect(catalog, List.of(a, b)).groups()).isEmpty();
    }

    @Test
    void tooLittleIdentityToCompareIsNotADuplicate() {
        Map<String, FieldResult> onlyAge = new LinkedHashMap<>();
        onlyAge.put("years", extracted(JSON.numberNode(54)));
        DocumentExtraction a = document("A", onlyAge, visit("2026-02-13", "red"));
        DocumentExtraction b = document("B", onlyAge, visit("2026-02-13", "red"));

        assertThat(detector.detect(catalog, List.of(a, b)).groups()).isEmpty();
    }

    @Test
    void comparesListsAsSetsIgnoringCase() {
        assertThat(DuplicateDetector.sameValue(JSON.arrayNode().add("Rash").add("itch"), JSON.arrayNode().add("itch").add("rash"))).isTrue();
        assertThat(DuplicateDetector.sameValue(JSON.numberNode(0.25), JSON.numberNode(0.250))).isTrue();
    }

    private static Map<String, FieldResult> person(String code, int years) {
        Map<String, FieldResult> fields = new LinkedHashMap<>();
        fields.put("code_name", extracted(JSON.textNode(code)));
        fields.put("years", extracted(JSON.numberNode(years)));
        return fields;
    }

    private static Map<String, FieldResult> visit(String date, String colour) {
        Map<String, FieldResult> fields = new LinkedHashMap<>();
        fields.put("visit_date", extracted(JSON.textNode(date)));
        fields.put("colour", extracted(JSON.textNode(colour)));
        fields.put("weight", FieldResult.unknown(null));
        return fields;
    }

    private static DocumentExtraction document(String id, Map<String, FieldResult> person, Map<String, FieldResult> visit) {
        Map<String, FieldResult> fields = new LinkedHashMap<>(person);
        fields.putAll(visit);
        return new DocumentExtraction(id, "test-1", "test/model", List.of(), List.of(new ExtractedCase(0, id, fields)));
    }

    private static FieldResult extracted(JsonNode value) {
        return FieldResult.extracted(value, Confidence.HIGH, List.of(new Provenance("quote", 0, 5)), null);
    }
}
