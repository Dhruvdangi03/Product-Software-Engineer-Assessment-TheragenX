package com.theragenx.intake.api;

import com.theragenx.intake.output.DocumentExtraction;
import com.theragenx.intake.review.DocumentReview;

/**
 * Everything the review screen needs for one document.
 *
 * @param text       the document text that provenance positions refer to; null for a document without text
 * @param extraction the pipeline output; null when the document has not been processed
 * @param review     the reviewer's decisions so far
 */
public record DocumentDetail(
        String id,
        String title,
        String text,
        DocumentExtraction extraction,
        DocumentReview review,
        ReviewProgress reviewProgress) {
}
