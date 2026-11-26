package com.theragenx.intake.api;

import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.review.DocumentReview;
import com.theragenx.intake.review.InvalidReviewException;
import com.theragenx.intake.review.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The REST API of the review UI. The UI talks to the backend only through these endpoints. */
@RestController
@RequestMapping("/api")
public class IntakeController {

    private final Catalog catalog;
    private final DocumentService documentService;
    private final ReviewService reviewService;

    public IntakeController(Catalog catalog, DocumentService documentService, ReviewService reviewService) {
        this.catalog = catalog;
        this.documentService = documentService;
        this.reviewService = reviewService;
    }

    @GetMapping("/catalog")
    public Catalog catalog() {
        return catalog;
    }

    @GetMapping("/documents")
    public List<DocumentSummary> documents() {
        return documentService.list();
    }

    @GetMapping("/documents/{id}")
    public DocumentDetail document(@PathVariable String id) {
        return documentService.detail(id);
    }

    @PatchMapping("/documents/{id}/cases/{caseIndex}/fields/{fieldId}")
    public DocumentReview reviewField(@PathVariable String id, @PathVariable int caseIndex,
                                      @PathVariable String fieldId, @RequestBody ReviewRequest request) {
        if (request.action() == null) {
            throw new InvalidReviewException("The request needs an action: accept, reject, edit or reset");
        }
        return reviewService.apply(id, caseIndex, fieldId, request.action(), request.value());
    }

    @GetMapping("/duplicates")
    public List<DuplicateView> duplicates() {
        return documentService.duplicates();
    }
}
