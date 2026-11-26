package com.theragenx.intake.duplicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.catalog.FieldDefinition;
import com.theragenx.intake.config.IntakeProperties;
import com.theragenx.intake.duplicates.CaseComparison.FieldComparison;
import com.theragenx.intake.duplicates.CaseComparison.Outcome;
import com.theragenx.intake.output.Confidence;
import com.theragenx.intake.output.DocumentExtraction;
import com.theragenx.intake.output.DuplicateReport;
import com.theragenx.intake.output.ExtractedCase;
import com.theragenx.intake.output.FieldResult;
import com.theragenx.intake.output.FieldStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Finds documents that describe the same case: the same patient, drug and reaction, reported twice.
 * <p>
 * The rule works on catalog sections, set in configuration, never on field ids:
 * <ol>
 *   <li>every field compared in the identity sections (the patient) must be the same, and enough
 *       of them must be present in both cases to compare at all;</li>
 *   <li>of the remaining fields, outside the ignored sections (report details, which differ between
 *       two reporters by nature), a minimum share must be the same.</li>
 * </ol>
 * Two different patients on the same drug with the same reaction fail rule 1.
 */
@Component
public class DuplicateDetector {

    private static final double HIGH_CONFIDENCE_RATIO = 0.8;

    private final IntakeProperties.Duplicates rules;

    @Autowired
    public DuplicateDetector(IntakeProperties properties) {
        this(properties.duplicates());
    }

    public DuplicateDetector(IntakeProperties.Duplicates rules) {
        this.rules = rules;
    }

    /** A pair of cases judged to be the same case. */
    public record CaseMatch(int caseIndexA, int caseIndexB, CaseComparison comparison, double matchRatio) {
    }

    public DuplicateReport detect(Catalog catalog, List<DocumentExtraction> extractions) {
        List<DuplicateReport.Group> groups = new ArrayList<>();
        for (int i = 0; i < extractions.size(); i++) {
            for (int j = i + 1; j < extractions.size(); j++) {
                DocumentExtraction a = extractions.get(i);
                DocumentExtraction b = extractions.get(j);
                bestMatch(catalog, a, b).ifPresent(match -> groups.add(group(a, b, match)));
            }
        }
        return new DuplicateReport(groups);
    }

    /** The most similar pair of cases between two documents, if any pair counts as a duplicate. */
    public Optional<CaseMatch> bestMatch(Catalog catalog, DocumentExtraction a, DocumentExtraction b) {
        Optional<CaseMatch> best = Optional.empty();
        for (ExtractedCase caseA : a.cases()) {
            for (ExtractedCase caseB : b.cases()) {
                CaseComparison comparison = compare(catalog, caseA, caseB);
                Optional<Double> ratio = duplicateRatio(comparison);
                if (ratio.isPresent() && (best.isEmpty() || ratio.get() > best.get().matchRatio())) {
                    best = Optional.of(new CaseMatch(caseA.caseIndex(), caseB.caseIndex(), comparison, ratio.get()));
                }
            }
        }
        return best;
    }

    public CaseComparison compare(Catalog catalog, ExtractedCase a, ExtractedCase b) {
        List<FieldComparison> fields = new ArrayList<>();
        for (FieldDefinition field : catalog.fields()) {
            Outcome outcome = compareField(a.fields().get(field.id()), b.fields().get(field.id()));
            fields.add(new FieldComparison(field.id(), field.label(), field.section(), outcome));
        }
        return new CaseComparison(fields);
    }

    /**
     * The fields of a comparison that decide the verdict.
     *
     * @param identity compared fields in the identity sections
     * @param evidence compared fields outside the identity and ignored sections
     */
    private record Evidence(List<FieldComparison> identity, List<FieldComparison> evidence) {

        double matchRatio() {
            return evidence.isEmpty() ? 0 : (double) countSame(evidence) / evidence.size();
        }
    }

    private Evidence evidenceOf(CaseComparison comparison) {
        List<String> notEvidence = Stream.concat(rules.identitySections().stream(), rules.ignoredSections().stream()).toList();
        return new Evidence(
                compared(comparison.inSections(rules.identitySections())),
                compared(comparison.outsideSections(notEvidence)));
    }

    /** The share of matching evidence fields when the pair is a duplicate, otherwise empty. */
    private Optional<Double> duplicateRatio(CaseComparison comparison) {
        Evidence evidence = evidenceOf(comparison);
        boolean samePatient = evidence.identity().size() >= rules.minIdentityFieldsCompared()
                && evidence.identity().stream().allMatch(field -> field.outcome() == Outcome.SAME);
        boolean sameEvent = !evidence.evidence().isEmpty() && evidence.matchRatio() >= rules.minMatchRatio();
        return samePatient && sameEvent ? Optional.of(evidence.matchRatio()) : Optional.empty();
    }

    private DuplicateReport.Group group(DocumentExtraction a, DocumentExtraction b, CaseMatch match) {
        Evidence evidence = evidenceOf(match.comparison());
        List<FieldComparison> matching = evidence.evidence().stream().filter(field -> field.outcome() == Outcome.SAME).toList();
        String reason = "Same patient (" + labels(evidence.identity()) + "); "
                + matching.size() + " of " + evidence.evidence().size() + " other fields match ("
                + labels(matching) + ").";
        Confidence confidence = match.matchRatio() >= HIGH_CONFIDENCE_RATIO ? Confidence.HIGH : Confidence.MEDIUM;
        return new DuplicateReport.Group(List.of(a.documentId(), b.documentId()), confidence, reason);
    }

    private static Outcome compareField(FieldResult a, FieldResult b) {
        if (!isExtracted(a) || !isExtracted(b)) {
            return Outcome.NOT_COMPARED;
        }
        return sameValue(a.value(), b.value()) ? Outcome.SAME : Outcome.DIFFERENT;
    }

    private static boolean isExtracted(FieldResult result) {
        return result != null && result.status() == FieldStatus.EXTRACTED;
    }

    /** Numbers compare by value, lists as sets, text ignoring case. */
    static boolean sameValue(JsonNode a, JsonNode b) {
        if (a.isNumber() && b.isNumber()) {
            return Double.compare(a.asDouble(), b.asDouble()) == 0;
        }
        if (a.isArray() && b.isArray()) {
            return textSet(a).equals(textSet(b));
        }
        return normalise(a.asText()).equals(normalise(b.asText()));
    }

    private static Set<String> textSet(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false)
                .map(item -> normalise(item.asText()))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static String normalise(String text) {
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private static List<FieldComparison> compared(List<FieldComparison> fields) {
        return fields.stream().filter(field -> field.outcome() != Outcome.NOT_COMPARED).toList();
    }

    private static long countSame(List<FieldComparison> fields) {
        return fields.stream().filter(field -> field.outcome() == Outcome.SAME).count();
    }

    private static String labels(List<FieldComparison> fields) {
        return fields.stream().map(FieldComparison::label).collect(Collectors.joining(", "));
    }
}
