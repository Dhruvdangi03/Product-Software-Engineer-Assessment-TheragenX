package com.theragenx.intake.document;

import com.theragenx.intake.config.IntakeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Finds the documents under a folder, including sub-folders such as {@code documents/stretch}.
 * A document is identified by its file name; the {@code .txt} file is the one we read, the
 * {@code .pdf} is kept for reference. A document that exists only as a PDF has no text.
 */
@Component
public class DocumentLibrary {

    private static final String TEXT_EXTENSION = ".txt";
    private static final String PDF_EXTENSION = ".pdf";

    private final Path documentsDir;

    @Autowired
    public DocumentLibrary(IntakeProperties properties) {
        this(properties.documentsDir());
    }

    public DocumentLibrary(Path documentsDir) {
        this.documentsDir = documentsDir;
    }

    public List<SourceDocument> findAll() {
        Map<String, Path> filesById = new TreeMap<>();
        for (Path file : listFiles()) {
            String id = idOf(file);
            Path known = filesById.get(id);
            if (known == null || isText(file)) {
                checkNotDefinedTwice(id, known, file);
                filesById.put(id, file);
            }
        }
        List<SourceDocument> documents = new ArrayList<>();
        filesById.forEach((id, file) -> documents.add(new SourceDocument(id, file, isText(file) ? read(file) : null)));
        return documents;
    }

    public Optional<SourceDocument> find(String documentId) {
        return findAll().stream().filter(document -> document.id().equals(documentId)).findFirst();
    }

    private List<Path> listFiles() {
        try (Stream<Path> files = Files.walk(documentsDir)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> isText(file) || file.toString().endsWith(PDF_EXTENSION))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list documents in " + documentsDir, e);
        }
    }

    private static void checkNotDefinedTwice(String id, Path known, Path file) {
        if (known != null && isText(known)) {
            throw new IllegalStateException("Document id " + id + " exists twice: " + known + " and " + file);
        }
    }

    private static String idOf(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.lastIndexOf('.'));
    }

    private static boolean isText(Path file) {
        return file.toString().endsWith(TEXT_EXTENSION);
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read document " + file, e);
        }
    }
}
