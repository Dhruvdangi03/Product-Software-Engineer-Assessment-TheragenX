package com.theragenx.intake.output;

/**
 * Where a value comes from in the document's .txt file.
 *
 * @param quote the exact text of the document between {@code start} and {@code end}
 * @param start offset of the first character, in Unicode code points, inclusive
 * @param end   offset after the last character, in Unicode code points, exclusive
 */
public record Provenance(String quote, int start, int end) {
}
