package com.theragenx.intake.document;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** A short human title for a document: its subject line when it has one, otherwise its first line. */
public final class DocumentTitle {

    private static final List<String> SUBJECT_PREFIXES = List.of("subject:", "re:");

    private DocumentTitle() {
    }

    public static String of(SourceDocument document) {
        return document.textIfReadable()
                .map(DocumentTitle::fromText)
                .orElse(document.path().getFileName().toString());
    }

    static String fromText(String text) {
        List<String> lines = text.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
        return subjectLine(lines).orElse(lines.isEmpty() ? "" : lines.getFirst());
    }

    private static Optional<String> subjectLine(List<String> lines) {
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            for (String prefix : SUBJECT_PREFIXES) {
                if (lower.startsWith(prefix)) {
                    return Optional.of(line.substring(prefix.length()).strip());
                }
            }
        }
        return Optional.empty();
    }
}
