package com.theragenx.intake.llm;

import com.theragenx.intake.config.IntakeProperties;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryingLlmClientTest {

    private static final LlmRequest REQUEST = new LlmRequest("system", "user", null);
    private static final LlmExchange ANSWER = new LlmExchange("test/model", null, null, "{}");

    private final List<Duration> waits = new ArrayList<>();
    private final IntakeProperties.Llm settings = new IntakeProperties.Llm(LlmMode.RECORD_ALL, "test", "model",
            URI.create("http://localhost"), "key", 0, 1000, Duration.ofSeconds(1), Duration.ZERO, 3, Duration.ofSeconds(2));

    @Test
    void retriesRetryableErrorsWithDoublingBackoff() {
        ScriptedClient model = new ScriptedClient(retryable(null), retryable(null));

        LlmExchange result = new RetryingLlmClient(model, settings, waits::add).complete(REQUEST);

        assertThat(result).isEqualTo(ANSWER);
        assertThat(waits).containsExactly(Duration.ofSeconds(2), Duration.ofSeconds(4));
    }

    @Test
    void waitsAsLongAsTheProviderAsks() {
        ScriptedClient model = new ScriptedClient(retryable(Duration.ofSeconds(36)));

        new RetryingLlmClient(model, settings, waits::add).complete(REQUEST);

        assertThat(waits).containsExactly(Duration.ofSeconds(36));
    }

    @Test
    void givesUpAfterMaxAttempts() {
        ScriptedClient model = new ScriptedClient(retryable(null), retryable(null), retryable(null));

        assertThatThrownBy(() -> new RetryingLlmClient(model, settings, waits::add).complete(REQUEST))
                .isInstanceOf(LlmException.class);
        assertThat(model.calls).isEqualTo(3);
    }

    @Test
    void doesNotRetryAnErrorRetryingCannotFix() {
        ScriptedClient model = new ScriptedClient(new LlmException("daily quota used up"));

        assertThatThrownBy(() -> new RetryingLlmClient(model, settings, waits::add).complete(REQUEST))
                .hasMessageContaining("daily quota");
        assertThat(model.calls).isEqualTo(1);
    }

    private static LlmException retryable(Duration retryAfter) {
        return new LlmException("busy", true, retryAfter, null);
    }

    /** Throws the given errors in order, then answers. */
    private static final class ScriptedClient implements LlmClient {
        private final List<LlmException> errors;
        private int calls;

        ScriptedClient(LlmException... errors) {
            this.errors = new ArrayList<>(List.of(errors));
        }

        @Override
        public LlmExchange complete(LlmRequest request) {
            calls++;
            if (!errors.isEmpty()) {
                throw errors.removeFirst();
            }
            return ANSWER;
        }

        @Override
        public String modelName() {
            return "test/model";
        }
    }
}
