package com.theragenx.intake.api;

import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.document.DocumentLibrary;
import com.theragenx.intake.document.DocumentTitle;
import com.theragenx.intake.document.SourceDocument;
import com.theragenx.intake.duplicates.DuplicateDetector;
import com.theragenx.intake.output.DocumentExtraction;
import com.theragenx.intake.output.DuplicateReport;
import com.theragenx.intake.output.OutputStore;
import com.theragenx.intake.review.DocumentReview;
import com.theragenx.intake.review.ReviewService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** Combines documents, committed outputs and reviews into what the API serves. Reads files on every call; there is no cache to go stale. */
@Service
public class DocumentService {

    private final Catalog catalog;
    private final DocumentLibrary documents;
    private final OutputStore outputStore;
    private final ReviewService reviewService;
    private final DuplicateDetector duplicateDetector;

    public DocumentService(Catalog catalog, DocumentLibrary documents, OutputStore outputStore,
                           ReviewService reviewService, DuplicateDetector duplicateDetector) {
        this.catalog = catalog;
        this.documents = documents;
        this.outputStore = outputStore;
        this.reviewService = reviewService;
        this.duplicateDetector = duplicateDetector;
    }

    public List<DocumentSummary> list() {
        return documents.findAll().stream().map(this::summarise).toList();
    }

    public DocumentDetail detail(String documentId) {
        SourceDocument document = documents.find(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        Optional<DocumentExtraction> extraction = outputStore.find(documentId);
        DocumentReview review = reviewService.reviewOf(documentId);
        return new DocumentDetail(document.id(), DocumentTitle.of(document), document.text(),
                extraction.orElse(null), review, progress(extraction, review));
    }

    public List<DuplicateView> duplicates() {
        return outputStore.findDuplicates().groups().stream()
                .map(this::toView)
                .flatMap(Optional::stream)
                .toList();
    }

    private DocumentSummary summarise(SourceDocument document) {
        Optional<DocumentExtraction> extraction = outputStore.find(document.id());
        DocumentReview review = reviewService.reviewOf(document.id());
        return new DocumentSummary(
                document.id(),
                DocumentTitle.of(document),
                extraction.isPresent(),
                extraction.map(e -> e.cases().size()).orElse(0),
                extraction.map(DocumentExtraction::flags).orElse(List.of()),
                progress(extraction, review));
    }

    private ReviewProgress progress(Optional<DocumentExtraction> extraction, DocumentReview review) {
        int total = extraction.map(e -> e.cases().size() * catalog.fields().size()).orElse(0);
        return new ReviewProgress(Math.min(review.reviewedFieldCount(), total), total);
    }

    /** Compares the two documents of a group again, so the view follows the current catalog. */
    private Optional<DuplicateView> toView(DuplicateReport.Group group) {
        if (group.documentIds().size() != 2) {
            return Optional.empty();
        }
        Optional<DocumentExtraction> a = outputStore.find(group.documentIds().get(0));
        Optional<DocumentExtraction> b = outputStore.find(group.documentIds().get(1));
        if (a.isEmpty() || b.isEmpty()) {
            return Optional.empty();
        }
        return duplicateDetector.bestMatch(catalog, a.get(), b.get())
                .map(match -> new DuplicateView(group.documentIds(),
                        List.of(match.caseIndexA(), match.caseIndexB()),
                        group.confidence(), group.reason(), match.comparison().fields()));
    }
}
