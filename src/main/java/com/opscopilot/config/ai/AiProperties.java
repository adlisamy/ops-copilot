package com.opscopilot.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for AI provider settings.
 *
 * <p>This record binds YAML/properties files to Java objects using Spring Boot's
 * {@code @ConfigurationProperties} mechanism. All properties are prefixed with
 * {@code app.ai} in the configuration file.</p>
 *
 * <h3>How {@code @ConfigurationProperties} works:</h3>
 * <p>Spring Boot reads {@code application.yml} (or {@code application.properties}) and
 * maps nested keys to this record's fields. For example, this YAML:</p>
 * <pre>
 * app:
 *   ai:
 *     provider: ollama
 *     ollama:
 *       baseUrl: http://localhost:11434
 *       model: qwen3:8b
 * </pre>
 * <p>...will populate {@code provider = "ollama"}, {@code ollama.baseUrl = "http://localhost:11434"},
 * and {@code ollama.model = "qwen3:8b"}.</p>
 *
 * <h3>Multi-provider design:</h3>
 * <p>This class supports three LLM providers (OpenAI, Anthropic, Ollama). The {@code provider}
 * field determines which one is active. Each provider configuration class uses
 * {@code @ConditionalOnProperty} to only create its bean when its name matches the
 * {@code app.ai.provider} value. This means you can switch providers by changing one
 * configuration line, without modifying any code.</p>
 *
 * <h3>Example configurations for each provider:</h3>
 * <pre>
 * # OpenAI (requires API key)
 * app:
 *   ai:
 *     provider: openai
 *     openai:
 *       apiKey: sk-...
 *       model: gpt-4o
 *
 * # Anthropic (requires API key)
 * app:
 *   ai:
 *     provider: anthropic
 *     anthropic:
 *       apiKey: sk-ant-...
 *       model: claude-sonnet-4-20250514
 *
 * # Ollama (local, no API key needed)
 * app:
 *   ai:
 *     provider: ollama
 *     ollama:
 *       baseUrl: http://localhost:11434
 *       model: qwen3:8b
 * </pre>
 *
 * <h3>Security note:</h3>
 * <p>API keys should never be committed to version control. Use Spring Boot's profile-specific
 * configuration (e.g., {@code application-local.yml}) or environment variables to keep
 * secrets out of the repository.</p>
 *
 * @param provider  the active LLM provider — must be one of: "openai", "anthropic", or "ollama"
 * @param openai    configuration for the OpenAI provider (only used when provider="openai")
 * @param anthropic configuration for the Anthropic provider (only used when provider="anthropic")
 * @param ollama    configuration for the Ollama provider (only used when provider="ollama")
 *
 * @see OpenAiConfiguration
 * @see AnthropicConfiguration
 * @see OllamaConfiguration
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        /**
         * The active LLM provider name.
         *
         * <p>Must be one of: "openai", "anthropic", or "ollama". This value is read by
         * {@code @ConditionalOnProperty} in each provider configuration class to determine
         * which ChatModel bean to create.</p>
         */
        String provider,

        /** Configuration for OpenAI — API key, optional base URL, and model name. */
        OpenAi openai,

        /** Configuration for Anthropic — API key and model name. */
        Anthropic anthropic,

        /** Configuration for Ollama — optional API key, base URL, and model name. */
        Ollama ollama
) {

    /**
     * OpenAI-specific configuration properties.
     *
     * <p>OpenAI is a cloud-based LLM provider (chat.openai.com). It requires an API key
     * and charges per token. Popular models include GPT-4o, GPT-4o-mini, and o1.</p>
     *
     * @param apiKey  the OpenAI API key (required) — obtain from platform.openai.com
     * @param baseUrl optional custom base URL — useful for proxies or OpenAI-compatible
     *                endpoints (e.g., Azure OpenAI); defaults to OpenAI's official API
     * @param model   the model name to use (e.g., "gpt-4o", "gpt-4o-mini")
     */
    public record OpenAi(
            String apiKey,
            String baseUrl,
            String model
    ) {
    }

    /**
     * Anthropic-specific configuration properties.
     *
     * <p>Anthropic is a cloud-based LLM provider (anthropic.com) known for Claude models.
     * It requires an API key and charges per token. Claude is known for strong reasoning
     * and long context windows.</p>
     *
     * @param apiKey the Anthropic API key (required) — obtain from console.anthropic.com
     * @param model  the model name to use (e.g., "claude-sonnet-4-20250514", "claude-opus-4-20250514")
     */
    public record Anthropic(
            String apiKey,
            String model
    ) {
    }

    /**
     * Ollama-specific configuration properties.
     *
     * <p>Ollama is a local LLM runner that lets you run open-source models on your own
     * hardware. It's free, private, and great for development or air-gapped environments.
     * No API key is needed for local instances.</p>
     *
     * <h3>Getting started with Ollama:</h3>
     * <ol>
     *   <li>Install Ollama from <a href="https://ollama.com">ollama.com</a></li>
     *   <li>Pull a model: {@code ollama pull qwen3:8b}</li>
     *   <li>Ollama runs on {@code http://localhost:11434} by default</li>
     * </ol>
     *
     * @param apiKey  optional API key — typically not needed for local Ollama
     * @param baseUrl the Ollama server URL (default: {@code http://localhost:11434})
     * @param model   the model name as known to Ollama (e.g., "qwen3:8b", "llama3.1:8b")
     */
    public record Ollama(
            String apiKey,
            String baseUrl,
            String model
    ) {
    }
}