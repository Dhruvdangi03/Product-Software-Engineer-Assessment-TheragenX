package com.theragenx.intake.extraction;

import com.theragenx.intake.output.Provenance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteLocatorTest {

    @Test
    void findsAnExactQuote() {
        String text = "Mrs M.K., a 54-year-old woman";

        Provenance found = new QuoteLocator(text).locate("54-year-old").orElseThrow();

        assertThat(found).isEqualTo(new Provenance("54-year-old", 12, 23));
    }

    @Test
    void ignoresCaseAndRunsOfWhitespaceAndReturnsTheDocumentsOwnText() {
        String text = "She took the first\n   dose on 2 March 2026.";

        Provenance found = new QuoteLocator(text).locate("she took the FIRST dose").orElseThrow();

        assertThat(found.quote()).isEqualTo("She took the first\n   dose");
        assertThat(text.substring(found.start(), found.end())).isEqualTo(found.quote());
    }

    @Test
    void treatsTypographicDashesAsHyphens() {
        Provenance found = new QuoteLocator("piperacillin–tazobactam was started").locate("piperacillin-tazobactam").orElseThrow();

        assertThat(found.start()).isZero();
        assertThat(found.end()).isEqualTo(23);
    }

    @Test
    void countsPositionsInCodePointsNotUtf16Units() {
        String text = "😀 rash on the trunk";

        Provenance found = new QuoteLocator(text).locate("rash").orElseThrow();

        assertThat(found.start()).isEqualTo(2);
        assertThat(found.end()).isEqualTo(6);
    }

    @Test
    void rejectsQuotesThatAreNotInTheDocumentOrTooShort() {
        QuoteLocator locator = new QuoteLocator("The rash resolved.");

        assertThat(locator.locate("the rash disappeared")).isEmpty();
        assertThat(locator.locate("ra")).isEmpty();
        assertThat(locator.locate("   ")).isEmpty();
    }
}
