package com.theragenx.intake.api;

import com.theragenx.intake.duplicates.CaseComparison.FieldComparison;
import com.theragenx.intake.output.Confidence;

import java.util.List;

/**
 * A duplicate pair with the field-by-field comparison of its two matching cases,
 * for the side-by-side view.
 */
public record DuplicateView(
        List<String> documentIds,
        List<Integer> caseIndexes,
        Confidence confidence,
        String reason,
        List<FieldComparison> fields) {
}
