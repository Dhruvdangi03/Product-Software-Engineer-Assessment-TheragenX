package com.theragenx.intake.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.catalog.FieldDefinition;
import com.theragenx.intake.catalog.FieldValueValidator;
import com.theragenx.intake.catalog.ValueCheck;
import com.theragenx.intake.output.DocumentExtraction;
import com.theragenx.intake.output.OutputStore;
import com.theragenx.intake.review.DocumentReview.Decision;
import com.theragenx.intake.review.DocumentReview.FieldReview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

/** Applies a reviewer's decision to one field, after checking that the field exists and an edit is valid. */
@Service
public class ReviewService {

    private final Catalog catalog;
    private final OutputStore outputStore;
    private final ReviewStore reviewStore;
    private final FieldValueValidator validator;
    private final Clock clock;

    @Autowired
    public ReviewService(Catalog catalog, OutputStore outputStore, ReviewStore reviewStore, FieldValueValidator validator) {
        this(catalog, outputStore, reviewStore, validator, Clock.systemUTC());
    }

    ReviewService(Catalog catalog, OutputStore outputStore, ReviewStore reviewStore,
                  FieldValueValidator validator, Clock clock) {
        this.catalog = catalog;
        this.outputStore = outputStore;
        this.reviewStore = reviewStore;
        this.validator = validator;
        this.clock = clock;
    }

    public DocumentReview reviewOf(String documentId) {
        return reviewStore.load(documentId);
    }

    /** Synchronised so two quick clicks cannot overwrite each other's change to the same file. */
    public synchronized DocumentReview apply(String documentId, int caseIndex, String fieldId,
                                             ReviewAction action, JsonNode value) {
        FieldDefinition field = checkTarget(documentId, caseIndex, fieldId);
        DocumentReview review = reviewStore.load(documentId);
        Map<Integer, Map<String, FieldReview>> cases = new TreeMap<>(review.cases());
        Map<String, FieldReview> fields = new TreeMap<>(cases.getOrDefault(caseIndex, Map.of()));

        Instant now = Instant.now(clock);
        switch (action) {
            case ACCEPT -> fields.put(fieldId, new FieldReview(Decision.ACCEPTED, null, now));
            case REJECT -> fields.put(fieldId, new FieldReview(Decision.REJECTED, null, now));
            case EDIT -> fields.put(fieldId, new FieldReview(Decision.EDITED, validEdit(field, value), now));
            case RESET -> fields.remove(fieldId);
        }

        if (fields.isEmpty()) {
            cases.remove(caseIndex);
        } else {
            cases.put(caseIndex, fields);
        }
        DocumentReview updated = new DocumentReview(cases);
        reviewStore.save(documentId, updated);
        return updated;
    }

    private FieldDefinition checkTarget(String documentId, int caseIndex, String fieldId) {
        DocumentExtraction extraction = outputStore.find(documentId)
                .orElseThrow(() -> new ReviewTargetNotFoundException("No extraction for document " + documentId));
        if (caseIndex < 0 || caseIndex >= extraction.cases().size()) {
            throw new ReviewTargetNotFoundException("Document " + documentId + " has no case " + caseIndex);
        }
        return catalog.field(fieldId)
                .orElseThrow(() -> new ReviewTargetNotFoundException("The catalog has no field " + fieldId));
    }

    private JsonNode validEdit(FieldDefinition field, JsonNode value) {
        ValueCheck check = validator.check(field, value);
        if (check instanceof ValueCheck.Invalid invalid) {
            throw new InvalidReviewException("Edited value for " + field.label() + " is not valid: " + invalid.reason());
        }
        return ((ValueCheck.Valid) check).value();
    }
}
