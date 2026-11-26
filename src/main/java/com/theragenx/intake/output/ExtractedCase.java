package com.theragenx.intake.output;

import java.util.Map;

/**
 * One patient's case.
 *
 * @param fields one result per catalog field, keyed by field id, in catalog order
 */
public record ExtractedCase(int caseIndex, String label, Map<String, FieldResult> fields) {
}
