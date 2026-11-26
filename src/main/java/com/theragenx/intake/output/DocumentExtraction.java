package com.theragenx.intake.output;

import java.util.ArrayList;
import java.util.List;

/** The output for one document, written as {@code out/<documentId>.json}. */
public record DocumentExtraction(
        String documentId,
        String catalogVersion,
        String model,
        List<DocumentFlag> flags,
        List<ExtractedCase> cases) {

    public DocumentExtraction {
        flags = List.copyOf(flags);
        cases = List.copyOf(cases);
    }

    public DocumentExtraction withFlag(DocumentFlag flag) {
        if (flags.contains(flag)) {
            return this;
        }
        List<DocumentFlag> newFlags = new ArrayList<>(flags);
        newFlags.add(flag);
        return new DocumentExtraction(documentId, catalogVersion, model, newFlags, cases);
    }
}
