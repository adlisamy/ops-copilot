package com.opscopilot.config.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conditional Spring configuration for the Ollama (local LLM) provider.
 *
 * <p>This class is only activated when {@code app.ai.provider=ollama} in the application
 * configuration. It creates an {@link OllamaChatModel} bean that implements the
 * {@link ChatModel} interface, allowing the rest of the application to interact with
 * a locally-running Ollama server without knowing the provider-specific details.</p>
 *
 * <h3>How {@code @ConditionalOnProperty} works:</h3>
 * <p>This Spring Boot annotation makes the entire {@code @Configuration} class conditional.
 * The bean methods inside are only registered if the specified property matches the expected
 * value. This is how the project supports multiple LLM providers:</p>
 * <ul>
 *   <li>{@code app.ai.provider=openai} → {@link OpenAiConfiguration} activates</li>
 *   <li>{@code app.ai.provider=anthropic} → {@link AnthropicConfiguration} activates</li>
 *   <li>{@code app.ai.provider=ollama} → this class activates</li>
 * </ul>
 * <p>Only one provider bean is created at a time, so {@code ChatClientConfiguration} can
 * inject a single {@code ChatModel} without ambiguity.</p>
 *
 * <h3>What is Ollama?</h3>
 * <p>Ollama is an open-source tool that lets you run LLMs locally on your own hardware.
 * Unlike OpenAI or Anthropic, there are no API keys, no per-token costs, and no internet
 * connection required. It's ideal for:</p>
 * <ul>
 *   <li><b>Learning</b> — experiment with LLMs without worrying about API costs</li>
 *   <li><b>Privacy</b> — sensitive data never leaves your machine</li>
 *   <li><b>Development</b> — test your application without depending on external services</li>
 *   <li><b>Air-gapped environments</b> — run LLMs on machines with no internet access</li>
 * </ul>
 *
 * <h3>Getting started with Ollama:</h3>
 * <ol>
 *   <li>Download and install from <a href="https://ollama.com">ollama.com</a></li>
 *   <li>Open a terminal and pull a model: {@code ollama pull qwen3:8b}</li>
 *   <li>Ollama starts automatically and listens on {@code http://localhost:11434}</li>
 *   <li>Verify it works: {@code ollama run qwen3:8b "Hello!"}</li>
 * </ol>
 *
 * <h3>Popular Ollama models:</h3>
 * <ul>
 *   <li><b>qwen3:8b</b> — good balance of speed and quality, ~8GB RAM</li>
 *   <li><b>llama3.1:8b</b> — Meta's open model, widely used, ~8GB RAM</li>
 *   <li><b>mistral:7b</b> — fast and lightweight, ~4GB RAM</li>
 *   <li><b>qwen3:72b</b> — more capable but requires ~40GB RAM</li>
 * </ul>
 *
 * <h3>Ollama-specific architecture:</h3>
 * <p>Unlike OpenAI and Anthropic configurations, Ollama uses two separate builders:</p>
 * <ul>
 *   <li><b>{@link OllamaApi}</b> — handles the connection to the Ollama server (base URL,
 *       authentication). This is the low-level HTTP client.</li>
 *   <li><b>{@link OllamaChatOptions}</b> — configures chat-specific settings (model name,
 *       temperature, etc.). This controls how the LLM generates responses.</li>
 * </ul>
 * <p>This separation reflects Ollama's design: the API server and the model are independent
 * concerns. You can run multiple models on one Ollama server, or connect to a remote
 * Ollama instance on another machine.</p>
 *
 * @see AiProperties
 * @see OpenAiConfiguration
 * @see AnthropicConfiguration
 */
@Configuration
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "ollama")
public class OllamaConfiguration {

    /**
     * Create the Ollama ChatModel bean.
     *
     * <p>This method configures the Ollama client with the server URL and model name
     * from {@link AiProperties}. Unlike cloud providers, Ollama typically doesn't need
     * an API key since it runs locally.</p>
     *
     * <p><b>Thinking mode:</b> Production-level models (qwen3.5:4b and larger) support
     * chain-of-thought reasoning, where the model generates internal reasoning steps before
     * producing the final answer. This improves response quality for complex questions.
     * Thinking is enabled by default here. For faster development iterations with smaller
     * models, you can add {@code .disableThinking()} to skip the reasoning step.</p>
     *
     * <h3>Builder structure explained:</h3>
     * <pre>
     * OllamaChatModel.builder()                    // Start building the ChatModel
     *     .options(OllamaChatOptions.builder()     // Chat-specific settings
     *         .model("qwen3:8b")                   // Which model to use
     *         .build())
     *     .ollamaApi(OllamaApi.builder()           // Connection settings
     *         .baseUrl("http://localhost:11434")   // Where Ollama is running
     *         .build())
     *     .build()                                 // Create the ChatModel instance
     * </pre>
     *
     * <h3>What happens when a prompt is sent:</h3>
     * <ol>
     *   <li>Spring AI formats the messages (system + user) into Ollama's chat API schema</li>
     *   <li>The request is sent to the local Ollama server via HTTP</li>
     *   <li>Ollama loads the model (if not already in memory) and generates a response</li>
     *   <li>{@code OllamaChatModel} parses the response and returns it to Spring AI</li>
     * </ol>
     *
     * <p>Note: The first request after startup may be slower because Ollama needs to load
     * the model into memory. Subsequent requests are faster as the model stays loaded.</p>
     *
     * @param properties the application's AI configuration, containing the Ollama base URL
     *                   and model name
     * @return a configured OllamaChatModel ready to send prompts to the local Ollama server
     */
    @Bean
    ChatModel ollamaChatModel(AiProperties properties) {
        return OllamaChatModel.builder()
                .options(OllamaChatOptions.builder()
                        .model(properties.ollama().model())
                        .disableThinking()
                        .build())
                .ollamaApi(OllamaApi.builder()
                        .baseUrl(properties.ollama().baseUrl())
                        .build())
                .build();
    }
}