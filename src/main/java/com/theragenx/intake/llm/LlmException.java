package com.theragenx.intake.llm;

import java.time.Duration;
import java.util.Optional;

/** A model request failed. {@link #isRetryable()} tells whether trying again can help. */
public class LlmException extends RuntimeException {

    private final boolean retryable;
    private final Duration retryAfter;

    public LlmException(String message) {
        this(message, false, null, null);
    }

    public LlmException(String message, boolean retryable, Duration retryAfter, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
        this.retryAfter = retryAfter;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /** The wait the provider asked for, when it said. */
    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}
