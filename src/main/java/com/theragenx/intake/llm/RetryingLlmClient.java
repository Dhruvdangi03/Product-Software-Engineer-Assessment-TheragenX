package com.theragenx.intake.llm;

import com.theragenx.intake.config.IntakeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Keeps requests inside the provider's limits: spaces requests out to respect the per-minute
 * limit, and retries rate-limit and server errors with exponential backoff, waiting as long
 * as the provider asks when it says. Errors that retrying cannot fix are thrown at once.
 */
public class RetryingLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(RetryingLlmClient.class);

    /** How the client waits. Tests pass one that records the waits instead of sleeping. */
    @FunctionalInterface
    public interface Sleeper {
        Sleeper THREAD_SLEEP = duration -> Thread.sleep(duration.toMillis());

        void sleep(Duration duration) throws InterruptedException;
    }

    private final LlmClient delegate;
    private final IntakeProperties.Llm settings;
    private final Sleeper sleeper;
    private long lastRequestNanos;

    public RetryingLlmClient(LlmClient delegate, IntakeProperties.Llm settings, Sleeper sleeper) {
        this.delegate = delegate;
        this.settings = settings;
        this.sleeper = sleeper;
    }

    @Override
    public String modelName() {
        return delegate.modelName();
    }

    @Override
    public LlmExchange complete(LlmRequest request) {
        for (int attempt = 1; ; attempt++) {
            waitForNextRequestSlot();
            try {
                return delegate.complete(request);
            } catch (LlmException e) {
                if (!e.isRetryable() || attempt >= settings.maxAttempts()) {
                    throw e;
                }
                Duration wait = e.retryAfter().orElse(backoff(attempt));
                log.warn("Attempt {} of {} failed ({}); retrying in {}s",
                        attempt, settings.maxAttempts(), e.getMessage(), wait.toSeconds());
                pause(wait);
            }
        }
    }

    private Duration backoff(int attempt) {
        return settings.initialBackoff().multipliedBy(1L << (attempt - 1));
    }

    private void waitForNextRequestSlot() {
        if (lastRequestNanos != 0) {
            Duration sinceLast = Duration.ofNanos(System.nanoTime() - lastRequestNanos);
            Duration remaining = settings.minIntervalBetweenRequests().minus(sinceLast);
            if (remaining.isPositive()) {
                pause(remaining);
            }
        }
        lastRequestNanos = System.nanoTime();
    }

    private void pause(Duration duration) {
        try {
            sleeper.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted while waiting to retry", false, null, e);
        }
    }
}
