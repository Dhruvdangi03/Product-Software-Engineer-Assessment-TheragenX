package com.theragenx.intake.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theragenx.intake.config.JsonMapperFactory;
import com.theragenx.intake.support.TestCatalogs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class FieldValueValidatorTest {

    private final ObjectMapper json = JsonMapperFactory.create();
    private final FieldValueValidator validator = new FieldValueValidator();
    private final Catalog catalog = TestCatalogs.small();

    @ParameterizedTest(name = "{0} = {1} -> valid: {2}")
    @CsvSource(delimiter = '|', textBlock = """
            code_name  | "AB"               | true
            code_name  | "A.B."             | false
            code_name  | 12                 | false
            years      | 54                 | true
            years      | 54.0               | true
            years      | 54.5               | false
            years      | 121                | false
            years      | -1                 | false
            years      | "54"               | false
            weight     | 0.25               | true
            weight     | -3                 | false
            visit_date | "2026-03-02"       | true
            visit_date | "2026-02-30"       | false
            visit_date | "02/03/2026"       | false
            colour     | "red"              | true
            colour     | "green"            | false
            tags       | ["a", "b"]         | true
            tags       | ["none"]           | true
            tags       | ["none", "a"]      | false
            tags       | []                 | false
            tags       | ["c"]              | false
            notes      | ["rash", "itch"]   | true
            notes      | []                 | true
            notes      | [1]                | false
            notes      | "rash"             | false
            """)
    void checksEveryTypeAndRule(String fieldId, String value, boolean expectedValid) throws Exception {
        ValueCheck check = validator.check(TestCatalogs.field(catalog, fieldId), json.readTree(value));

        assertThat(check instanceof ValueCheck.Valid).as(String.valueOf(check)).isEqualTo(expectedValid);
    }

    @Test
    void missingValueIsInvalid() {
        assertThat(validator.check(TestCatalogs.field(catalog, "colour"), null)).isInstanceOf(ValueCheck.Invalid.class);
    }

    @Test
    void normalisesWholeNumbersTextAndLists() throws Exception {
        assertThat(validValue("years", "54.0").isIntegralNumber()).isTrue();
        assertThat(validValue("code_name", "\" AB \"").asText()).isEqualTo("AB");
        assertThat(validValue("notes", "[\" rash \", \"rash\", \"\"]")).hasSize(1);
    }

    private JsonNode validValue(String fieldId, String value) throws Exception {
        return ((ValueCheck.Valid) validator.check(TestCatalogs.field(catalog, fieldId), json.readTree(value))).value();
    }
}
