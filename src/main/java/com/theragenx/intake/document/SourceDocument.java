package com.theragenx.intake.document;

import java.nio.file.Path;
import java.util.Optional;

/**
 * A document in the documents folder.
 *
 * @param id   the file name without extension, for example D01
 * @param path the file the document was read from
 * @param text the plain text, or {@code null} when only a format we cannot read (a PDF) exists
 */
public record SourceDocument(String id, Path path, String text) {

    public Optional<String> textIfReadable() {
        return Optional.ofNullable(text);
    }
}
