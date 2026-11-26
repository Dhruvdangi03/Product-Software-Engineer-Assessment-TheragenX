package com.theragenx.intake.extraction;

import com.theragenx.intake.catalog.FieldDefinition;
import com.theragenx.intake.catalog.FieldValueValidator;
import com.theragenx.intake.catalog.ValueCheck;
import com.theragenx.intake.extraction.ModelExtraction.ModelField;
import com.theragenx.intake.output.Confidence;
import com.theragenx.intake.output.FieldResult;
import com.theragenx.intake.output.FieldStatus;
import com.theragenx.intake.output.Provenance;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Turns the model's answer for one field into the output result, checking it on the way.
 * <p>
 * The rule throughout: a wrong value costs more than an empty one. Whenever an answer fails a
 * check (not a catalog value, no quote found in the document, no confidence), the field becomes
 * {@code unknown} and the note says why, so the reviewer sees what was rejected.
 */
@Component
public class FieldResolver {

    /** A value the model computed (from a relative date, a year of birth, a conversion) is never high confidence. */
    static final Confidence MAX_CONFIDENCE_FOR_DERIVED = Confidence.MEDIUM;

    private final FieldValueValidator validator;

    public FieldResolver(FieldValueValidator validator) {
        this.validator = validator;
    }

    public FieldResult resolve(FieldDefinition field, ModelField answer, QuoteLocator locator) {
        if (answer == null) {
            return FieldResult.unknown("No model answer for this field (for example, a field added to the catalog after the answer was recorded).");
        }
        Optional<FieldStatus> status = FieldStatus.parse(answer.status());
        if (status.isEmpty()) {
            return FieldResult.unknown(withModelNote("Model gave an invalid status '" + answer.status() + "'.", answer));
        }
        Quotes quotes = Quotes.locate(answer.quotes(), locator);
        return switch (status.get()) {
            case UNKNOWN -> FieldResult.unknown(answer.note());
            case CONFLICT -> FieldResult.conflict(conflictNote(answer), quotes.found());
            case EXTRACTED -> resolveExtracted(field, answer, quotes);
        };
    }

    private FieldResult resolveExtracted(FieldDefinition field, ModelField answer, Quotes quotes) {
        ValueCheck check = validator.check(field, answer.value());
        if (check instanceof ValueCheck.Invalid invalid) {
            return FieldResult.unknown(withModelNote("Model value " + answer.value() + " rejected: " + invalid.reason() + ".", answer));
        }
        Optional<Confidence> confidence = Confidence.parse(answer.confidence());
        if (confidence.isEmpty()) {
            return FieldResult.unknown(withModelNote("Model value " + answer.value() + " rejected: no valid confidence.", answer));
        }
        if (quotes.found().isEmpty()) {
            return FieldResult.unknown(withModelNote("Model value " + answer.value() + " rejected: none of its quotes "
                    + answer.quotes() + " was found in the document.", answer));
        }
        Confidence finalConfidence = answer.derived() ? confidence.get().atMost(MAX_CONFIDENCE_FOR_DERIVED) : confidence.get();
        String note = joinNotes(answer.note(), quotes.missingNote());
        return FieldResult.extracted(((ValueCheck.Valid) check).value(), finalConfidence, quotes.found(), note);
    }

    private static String conflictNote(ModelField answer) {
        return answer.note() == null || answer.note().isBlank()
                ? "The document states incompatible values."
                : answer.note();
    }

    private static String withModelNote(String reason, ModelField answer) {
        return joinNotes(reason, answer.note() == null ? null : "Model note: " + answer.note());
    }

    private static String joinNotes(String first, String second) {
        if (first == null || first.isBlank()) {
            return second;
        }
        return second == null || second.isBlank() ? first : first + " " + second;
    }

    /** The model's quotes split into those found in the document and those that were not. */
    private record Quotes(List<Provenance> found, List<String> missing) {

        static Quotes locate(List<String> quotes, QuoteLocator locator) {
            Set<Provenance> found = new LinkedHashSet<>();
            List<String> missing = new ArrayList<>();
            for (String quote : quotes) {
                locator.locate(quote).ifPresentOrElse(found::add, () -> missing.add(quote));
            }
            return new Quotes(List.copyOf(found), missing);
        }

        String missingNote() {
            return missing.isEmpty() ? null : "Quotes not found in the document and left out: " + missing + ".";
        }
    }
}
