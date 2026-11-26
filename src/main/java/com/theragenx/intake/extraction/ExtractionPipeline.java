package com.theragenx.intake.extraction;

import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.config.IntakeProperties;
import com.theragenx.intake.document.DocumentLibrary;
import com.theragenx.intake.document.SourceDocument;
import com.theragenx.intake.duplicates.DuplicateDetector;
import com.theragenx.intake.llm.Fixture;
import com.theragenx.intake.llm.FixtureStore;
import com.theragenx.intake.llm.LlmClient;
import com.theragenx.intake.llm.LlmExchange;
import com.theragenx.intake.llm.LlmMode;
import com.theragenx.intake.output.DocumentExtraction;
import com.theragenx.intake.output.DocumentFlag;
import com.theragenx.intake.output.DuplicateReport;
import com.theragenx.intake.output.OutputStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs the whole pipeline: every document in the documents folder, one model request each,
 * then duplicate detection, then one output file per document plus {@code duplicates.json}.
 * <p>
 * Recording and replay share every step after the model call, which is why replay reproduces
 * the recorded output exactly.
 */
@Component
public class ExtractionPipeline {

    private static final Logger log = LoggerFactory.getLogger(ExtractionPipeline.class);

    private final Catalog catalog;
    private final DocumentLibrary documents;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final FixtureStore fixtures;
    private final DocumentExtractor extractor;
    private final DuplicateDetector duplicateDetector;
    private final OutputStore outputStore;
    private final LlmMode mode;

    public ExtractionPipeline(Catalog catalog, DocumentLibrary documents, PromptBuilder promptBuilder,
                              LlmClient llmClient, FixtureStore fixtures, DocumentExtractor extractor,
                              DuplicateDetector duplicateDetector, OutputStore outputStore,
                              IntakeProperties properties) {
        this.catalog = catalog;
        this.documents = documents;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.fixtures = fixtures;
        this.extractor = extractor;
        this.duplicateDetector = duplicateDetector;
        this.outputStore = outputStore;
        this.mode = properties.llm().mode();
    }

    public List<DocumentExtraction> run() {
        log.info("Extracting with catalog {} in {} mode", catalog.catalogVersion(), mode);
        List<DocumentExtraction> extractions = documents.findAll().stream().map(this::extract).toList();

        DuplicateReport duplicates = duplicateDetector.detect(catalog, extractions);
        List<DocumentExtraction> flagged = markDuplicates(extractions, duplicates);

        flagged.forEach(outputStore::write);
        outputStore.writeDuplicates(duplicates);
        log.info("Wrote {} documents and {} duplicate group(s)", flagged.size(), duplicates.groups().size());
        return flagged;
    }

    private DocumentExtraction extract(SourceDocument document) {
        if (document.textIfReadable().isEmpty()) {
            log.info("{}: no text file, flagged as unsupported format", document.id());
            return extractor.unsupported(catalog, document.id());
        }
        String text = document.text();
        LlmExchange exchange = modelAnswerFor(document, text);
        DocumentExtraction extraction = extractor.extract(catalog, document.id(), text, exchange.model(), exchange.responseText());
        log.info("{}: {} case(s), flags {}", document.id(), extraction.cases().size(), extraction.flags());
        return extraction;
    }

    private LlmExchange modelAnswerFor(SourceDocument document, String text) {
        return switch (mode) {
            case REPLAY -> fixtures.load(document.id()).exchange();
            case RECORD_MISSING -> fixtures.find(document.id())
                    .map(Fixture::exchange)
                    .orElseGet(() -> askModelAndRecord(document, text));
            case RECORD_ALL -> askModelAndRecord(document, text);
        };
    }

    private LlmExchange askModelAndRecord(SourceDocument document, String text) {
        log.info("{}: asking {}", document.id(), llmClient.modelName());
        LlmExchange exchange = llmClient.complete(promptBuilder.build(catalog, document, text));
        fixtures.save(new Fixture(document.id(), Instant.now(), exchange));
        return exchange;
    }

    private static List<DocumentExtraction> markDuplicates(List<DocumentExtraction> extractions, DuplicateReport duplicates) {
        Set<String> duplicateIds = duplicates.groups().stream()
                .flatMap(group -> group.documentIds().stream())
                .collect(Collectors.toSet());
        return extractions.stream()
                .map(extraction -> duplicateIds.contains(extraction.documentId())
                        ? extraction.withFlag(DocumentFlag.POSSIBLE_DUPLICATE)
                        : extraction)
                .toList();
    }
}
