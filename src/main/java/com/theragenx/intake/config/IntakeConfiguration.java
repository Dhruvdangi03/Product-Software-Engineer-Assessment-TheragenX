package com.theragenx.intake.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.catalog.CatalogLoader;
import com.theragenx.intake.llm.GeminiClient;
import com.theragenx.intake.llm.LlmClient;
import com.theragenx.intake.llm.LlmException;
import com.theragenx.intake.llm.RetryingLlmClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class IntakeConfiguration {

    static final String GEMINI_PROVIDER = "gemini";

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapperFactory.create();
    }

    @Bean
    public Catalog catalog(CatalogLoader catalogLoader, IntakeProperties properties) {
        return catalogLoader.load(properties.catalogPath());
    }

    /** Switching provider is a configuration change: add a client, then a branch here. */
    @Bean
    public LlmClient llmClient(IntakeProperties properties, ObjectMapper objectMapper) {
        IntakeProperties.Llm llm = properties.llm();
        LlmClient provider = switch (llm.provider()) {
            case GEMINI_PROVIDER -> new GeminiClient(llm, HttpClient.newHttpClient(), objectMapper);
            default -> throw new LlmException("Unsupported LLM provider: " + llm.provider());
        };
        return new RetryingLlmClient(provider, llm, RetryingLlmClient.Sleeper.THREAD_SLEEP);
    }
}
