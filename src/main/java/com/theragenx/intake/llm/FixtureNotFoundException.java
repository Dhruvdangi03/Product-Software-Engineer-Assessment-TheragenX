package com.theragenx.intake.llm;

/** Replay needed a recorded exchange that does not exist. */
public class FixtureNotFoundException extends RuntimeException {

    public FixtureNotFoundException(String documentId, String location) {
        super("No recorded model exchange for document " + documentId + " at " + location
                + ". Record it first: run the pipeline with intake.llm.mode=record-missing and an API key.");
    }
}
