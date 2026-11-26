package com.theragenx.intake.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/** Reads the catalog file and checks that it is consistent before anything uses it. */
@Component
public class CatalogLoader {

    private final ObjectMapper objectMapper;

    public CatalogLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Catalog load(Path path) {
        Catalog catalog;
        try {
            catalog = objectMapper.readValue(path.toFile(), Catalog.class);
        } catch (IOException e) {
            throw new CatalogException("Cannot read catalog " + path + ": " + e.getMessage(), e);
        }
        checkConsistency(catalog);
        return catalog;
    }

    private void checkConsistency(Catalog catalog) {
        if (catalog.fields().isEmpty()) {
            throw new CatalogException("Catalog defines no fields");
        }
        Set<String> sectionIds = catalog.sections().stream().map(Section::id).collect(Collectors.toSet());
        Set<String> fieldIds = new HashSet<>();
        for (FieldDefinition field : catalog.fields()) {
            if (!fieldIds.add(field.id())) {
                throw new CatalogException("Field id defined twice: " + field.id());
            }
            if (!sectionIds.contains(field.section())) {
                throw new CatalogException("Field " + field.id() + " refers to unknown section " + field.section());
            }
            if (field.type().hasOptions() && field.options().isEmpty()) {
                throw new CatalogException("Field " + field.id() + " is " + field.type().code() + " but has no options");
            }
            checkPattern(field);
        }
    }

    private void checkPattern(FieldDefinition field) {
        String pattern = field.validation().pattern();
        if (pattern == null) {
            return;
        }
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new CatalogException("Field " + field.id() + " has an invalid pattern: " + pattern, e);
        }
    }
}
