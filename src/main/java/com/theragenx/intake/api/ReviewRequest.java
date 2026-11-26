package com.theragenx.intake.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.theragenx.intake.review.ReviewAction;

/**
 * Body of {@code PATCH /api/documents/{id}/cases/{caseIndex}/fields/{fieldId}}.
 *
 * @param value the new value, required for {@code edit} and ignored otherwise
 */
public record ReviewRequest(ReviewAction action, JsonNode value) {
}
