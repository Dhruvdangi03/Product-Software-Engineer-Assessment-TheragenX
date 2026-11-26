package com.theragenx.intake.extraction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.catalog.FieldDefinition;
import com.theragenx.intake.extraction.ModelExtraction.MinimumElements;
import com.theragenx.intake.extraction.ModelExtraction.ModelCase;
import com.theragenx.intake.output.DocumentExtraction;
import com.theragenx.intake.output.DocumentFlag;
import com.theragenx.intake.output.ExtractedCase;
import com.theragenx.intake.output.FieldResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the model's answer for one document into the output for that document:
 * applies the minimum-elements rule, then checks every field of every case against the catalog.
 */
@Component
public class DocumentExtractor {

    private static final Logger log = LoggerFactory.getLogger(DocumentExtractor.class);

    private final FieldResolver fieldResolver;
    private final ObjectMapper objectMapper;

    public DocumentExtractor(FieldResolver fieldResolver, ObjectMapper objectMapper) {
        this.fieldResolver = fieldResolver;
        this.objectMapper = objectMapper;
    }

    /** A document we cannot read (for example an image-only PDF) gets a flag and no cases. */
    public DocumentExtraction unsupported(Catalog catalog, String documentId) {
        return new DocumentExtraction(documentId, catalog.catalogVersion(), null,
                List.of(DocumentFlag.UNSUPPORTED_FORMAT), List.of());
    }

    public DocumentExtraction extract(Catalog catalog, String documentId, String text, String model, String modelAnswer) {
        ModelExtraction answer;
        try {
            answer = objectMapper.readValue(modelAnswer, ModelExtraction.class);
        } catch (JsonProcessingException e) {
            log.error("Model answer for {} is not valid JSON: {}", documentId, e.getOriginalMessage());
            return new DocumentExtraction(documentId, catalog.catalogVersion(), model,
                    List.of(DocumentFlag.MODEL_RESPONSE_UNREADABLE), List.of());
        }

        List<DocumentFlag> flags = new ArrayList<>();
        if (answer.containsEmbeddedInstructions()) {
            flags.add(DocumentFlag.EMBEDDED_INSTRUCTIONS_IGNORED);
        }
        List<DocumentFlag> missingElements = missingMinimumElements(answer.minimumElements());
        flags.addAll(missingElements);

        List<ExtractedCase> cases = missingElements.isEmpty()
                ? resolveCases(catalog, answer.cases(), new QuoteLocator(text))
                : List.of();
        if (cases.size() > 1) {
            flags.add(DocumentFlag.MULTIPLE_PATIENTS);
        }
        return new DocumentExtraction(documentId, catalog.catalogVersion(), model, flags, cases);
    }

    /** A case needs all four: an identifiable patient, a reporter, a suspect drug and an adverse event. */
    private static List<DocumentFlag> missingMinimumElements(MinimumElements elements) {
        if (elements == null) {
            return List.of(DocumentFlag.MODEL_RESPONSE_UNREADABLE);
        }
        List<DocumentFlag> missing = new ArrayList<>();
        if (!elements.identifiablePatient()) {
            missing.add(DocumentFlag.NO_IDENTIFIABLE_PATIENT);
        }
        if (!elements.reporter()) {
            missing.add(DocumentFlag.NO_REPORTER);
        }
        if (!elements.suspectDrug()) {
            missing.add(DocumentFlag.NO_SUSPECT_DRUG);
        }
        if (!elements.adverseEvent()) {
            missing.add(DocumentFlag.NO_ADVERSE_EVENT);
        }
        return missing;
    }

    private List<ExtractedCase> resolveCases(Catalog catalog, List<ModelCase> modelCases, QuoteLocator locator) {
        List<ExtractedCase> cases = new ArrayList<>();
        for (ModelCase modelCase : modelCases) {
            Map<String, FieldResult> fields = new LinkedHashMap<>();
            for (FieldDefinition field : catalog.fields()) {
                fields.put(field.id(), fieldResolver.resolve(field, modelCase.fields().get(field.id()), locator));
            }
            cases.add(new ExtractedCase(cases.size(), modelCase.label(), fields));
        }
        return cases;
    }
}
