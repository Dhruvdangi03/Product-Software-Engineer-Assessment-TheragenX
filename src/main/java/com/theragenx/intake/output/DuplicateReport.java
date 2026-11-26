package com.theragenx.intake.output;

import java.util.List;

/** The content of {@code out/duplicates.json}, in the format of {@code schema/duplicates.schema.json}. */
public record DuplicateReport(List<Group> groups) {

    public static final DuplicateReport EMPTY = new DuplicateReport(List.of());

    public DuplicateReport {
        groups = List.copyOf(groups);
    }

    /** Documents that describe the same case. */
    public record Group(List<String> documentIds, Confidence confidence, String reason) {
    }
}
