package com.theragenx.intake.llm;

/** Where the pipeline gets model answers from. */
public enum LlmMode {

    /** Only from recorded fixtures. No network and no API key needed. */
    REPLAY,

    /** Call the model for documents that have no fixture yet, and record the answer. Resumes an interrupted run. */
    RECORD_MISSING,

    /** Call the model for every document and overwrite the fixtures, for example after a prompt change. */
    RECORD_ALL
}
