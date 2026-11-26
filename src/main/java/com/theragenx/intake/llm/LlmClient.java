package com.theragenx.intake.llm;

/**
 * A language model that answers one request with JSON. The pipeline only knows this interface,
 * so the provider is chosen in configuration (see {@code IntakeConfiguration#llmClient}).
 */
public interface LlmClient {

    /** Sends the request and returns the exchange as sent and received, ready to be recorded. */
    LlmExchange complete(LlmRequest request);

    /** Provider and model, written as {@code provider/model}, for example {@code gemini/gemini-2.5-flash}. */
    String modelName();
}
