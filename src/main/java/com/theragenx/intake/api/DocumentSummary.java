package com.theragenx.intake.api;

import com.theragenx.intake.output.DocumentFlag;

import java.util.List;

/**
 * One row of the document list.
 *
 * @param processed false when the pipeline has not written an output for this document yet
 */
public record DocumentSummary(
        String id,
        String title,
        boolean processed,
        int caseCount,
        List<DocumentFlag> flags,
        ReviewProgress reviewProgress) {
}
