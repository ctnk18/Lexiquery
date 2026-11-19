package com.project.lexiquery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "lexiquery")
public record LexiQueryProperties(Target target, Ml ml, Llm llm, Limits limits, Policy policy) {
    public record Target(String url, String username, String password, String schema) {}
    public record Ml(boolean enabled, String baseUrl, double injectionThreshold) {}
    /** provider: "openai" = any OpenAI-compatible API (Groq, OpenRouter, Gemini, Ollama...), "anthropic" = Anthropic API. */
    public record Llm(boolean enabled, String provider, String baseUrl, String apiKey, String model, int maxTokens) {}
    public record Limits(int maxRows, int timeoutSeconds, int requestsPerMinute, int maxRetries) {}
    public record Policy(List<String> viewerTables) {}
}
