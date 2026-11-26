package com.theragenx.intake.catalog;

/**
 * The validation rules a catalog field may carry. Every key is optional.
 *
 * @param pattern regular expression a string value must match in full
 * @param min     smallest allowed number, inclusive
 * @param max     largest allowed number, inclusive
 */
public record Validation(String pattern, Double min, Double max) {

    public static final Validation NONE = new Validation(null, null, null);
}
