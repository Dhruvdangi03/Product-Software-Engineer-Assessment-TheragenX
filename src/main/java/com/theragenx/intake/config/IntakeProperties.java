package com.theragenx.intake.config;

import com.theragenx.intake.llm.LlmMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** All settings of the application, bound from the {@code intake} block of application.yaml. */
@ConfigurationProperties("intake")
public record IntakeProperties(
        Path catalogPath,
        Path documentsDir,
        Path outputDir,
        Path fixturesDir,
        Path reviewsDir,
        Path schemaDir,
        Llm llm,
        Duplicates duplicates) {

    /**
     * @param mode                       replay from fixtures, or call the model and record
     * @param provider                   which {@code LlmClient} implementation to use
     * @param apiKey                     read from the environment, never from a file in the repository
     * @param minIntervalBetweenRequests spacing between requests, to stay under the provider's per-minute limit
     * @param maxAttempts                attempts per request, including the first, for rate limits and server errors
     * @param initialBackoff             wait before the first retry; doubles on every further retry
     */
    public record Llm(
            LlmMode mode,
            String provider,
            String model,
            URI baseUrl,
            String apiKey,
            double temperature,
            int maxOutputTokens,
            Duration requestTimeout,
            Duration minIntervalBetweenRequests,
            int maxAttempts,
            Duration initialBackoff) {
    }

    /**
     * Rules for deciding that two cases from different documents describe the same patient and reaction.
     *
     * @param identitySections         catalog sections that identify the patient: every field compared there must match
     * @param ignoredSections          catalog sections that describe the report, which naturally differ between reporters
     * @param minIdentityFieldsCompared how many identity fields must be present in both cases to compare at all
     * @param minMatchRatio             share of the other compared fields that must match
     */
    public record Duplicates(
            List<String> identitySections,
            List<String> ignoredSections,
            int minIdentityFieldsCompared,
            double minMatchRatio) {
    }
}
