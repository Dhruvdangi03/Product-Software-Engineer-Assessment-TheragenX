package com.theragenx.intake.extraction;

import com.theragenx.intake.output.Provenance;

import java.util.Optional;

/**
 * Finds a quote in a document and returns its exact position.
 * <p>
 * We never trust positions from the model: models count characters badly. The model gives the
 * quote text, and this class searches for it. The search ignores case, runs of whitespace and
 * typographic dashes and quotes, the same differences the checker forgives, and maps the match
 * back to positions in the original text. The returned quote is the document's own text.
 * <p>
 * Positions are Unicode code points, as the output schema requires. Java strings count UTF-16
 * units, which differ for characters outside the Basic Multilingual Plane, so we convert.
 */
public final class QuoteLocator {

    /** Shorter quotes are too ambiguous to support a value; the checker rejects them too. */
    static final int MIN_QUOTE_LENGTH = 3;

    private final String text;
    private final String normalisedText;
    /** For each character of {@code normalisedText}, the index of the character it came from in {@code text}. */
    private final int[] originalIndex;

    public QuoteLocator(String text) {
        this.text = text;
        StringBuilder normalised = new StringBuilder(text.length());
        int[] indexes = new int[text.length()];
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                boolean previousIsSpace = !normalised.isEmpty() && normalised.charAt(normalised.length() - 1) == ' ';
                if (previousIsSpace) {
                    continue;
                }
                c = ' ';
            }
            indexes[normalised.length()] = i;
            normalised.append(fold(c));
        }
        this.normalisedText = normalised.toString();
        this.originalIndex = indexes;
    }

    /** The first place the quote occurs, or empty when it does not occur or is too short. */
    public Optional<Provenance> locate(String quote) {
        String needle = normalise(quote);
        if (needle.length() < MIN_QUOTE_LENGTH) {
            return Optional.empty();
        }
        int found = normalisedText.indexOf(needle);
        if (found < 0) {
            return Optional.empty();
        }
        int start = originalIndex[found];
        int end = originalIndex[found + needle.length() - 1] + 1;
        return Optional.of(new Provenance(
                text.substring(start, end),
                text.codePointCount(0, start),
                text.codePointCount(0, end)));
    }

    private static String normalise(String quote) {
        StringBuilder normalised = new StringBuilder(quote.length());
        for (char c : quote.strip().toCharArray()) {
            if (Character.isWhitespace(c)) {
                if (normalised.charAt(normalised.length() - 1) != ' ') {
                    normalised.append(' ');
                }
            } else {
                normalised.append(fold(c));
            }
        }
        return normalised.toString();
    }

    /** One character in, one character out, so positions stay aligned with the original text. */
    private static char fold(char c) {
        return switch (c) {
            case '–', '—' -> '-';
            case '‘', '’' -> '\'';
            case '“', '”' -> '"';
            default -> Character.toLowerCase(c);
        };
    }
}
