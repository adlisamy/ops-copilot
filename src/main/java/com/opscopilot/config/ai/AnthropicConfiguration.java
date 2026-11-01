package com.opscopilot.config.ai;


import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conditional Spring configuration for the Anthropic (Claude) LLM provider.
 *
 * <p>This class is only activated when {@code app.ai.provider=anthropic} in the application
 * configuration. It creates an {@link AnthropicChatModel} bean that implements the
 * {@link ChatModel} interface, allowing the rest of the application to interact with
 * Anthropic's Claude models without knowing the provider-specific details.</p>
 *
 * <h3>How {@code @ConditionalOnProperty} works:</h3>
 * <p>This Spring Boot annotation makes the entire {@code @Configuration} class conditional.
 * The bean methods inside are only registered if the specified property matches the expected
 * value. This is how the project supports multiple LLM providers:</p>
 * <ul>
 *   <li>{@code app.ai.provider=openai} → {@link OpenAiConfiguration} activates</li>
 *   <li>{@code app.ai.provider=anthropic} → this class activates</li>
 *   <li>{@code app.ai.provider=ollama} → {@link OllamaConfiguration} activates</li>
 * </ul>
 * <p>Only one provider bean is created at a time, so {@code ChatClientConfiguration} can
 * inject a single {@code ChatModel} without ambiguity.</p>
 *
 * <h3>Anthropic-specific details:</h3>
 * <p>Anthropic provides Claude models via REST API at {@code api.anthropic.com}. Claude is
 * known for strong reasoning, long context windows (up to 200K tokens), and careful,
 * safety-oriented responses. Key options:</p>
 * <ul>
 *   <li><b>apiKey</b> — required; authenticates your requests and tracks usage for billing</li>
 *   <li><b>model</b> — the model name (e.g., "claude-sonnet-4-20250514", "claude-opus-4-20250514");
 *       Sonnet is a balanced choice, Opus is the most capable but expensive</li>
 * </ul>
 *
 * <h3>Model selection guide:</h3>
 * <ul>
 *   <li><b>Claude Haiku</b> — fastest and cheapest, good for simple tasks</li>
 *   <li><b>Claude Sonnet</b> — balanced speed and capability, recommended for most use cases</li>
 *   <li><b>Claude Opus</b> — most capable, best for complex reasoning, but slowest and most expensive</li>
 * </ul>
 *
 * @see AiProperties
 * @see OpenAiConfiguration
 * @see OllamaConfiguration
 */
@Configuration
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "anthropic")
public class AnthropicConfiguration {

    /**
     * Create the Anthropic ChatModel bean.
     *
     * <p>This method uses the builder pattern to configure the Anthropic client with credentials
     * and model settings from {@link AiProperties}. The returned bean implements {@link ChatModel},
     * the Spring AI interface that abstracts away provider-specific details.</p>
     *
     * <h3>What happens when a prompt is sent:</h3>
     * <ol>
     *   <li>Spring AI formats the messages (system + user) into Anthropic's API schema</li>
     *   <li>The request is sent to Anthropic's API with the API key for authentication</li>
     *   <li>Anthropic's Claude model processes the request and returns the generated response</li>
     *   <li>{@code AnthropicChatModel} parses the response and returns it to Spring AI</li>
     * </ol>
     *
     * @param properties the application's AI configuration, containing the Anthropic API key
     *                   and model name
     * @return a configured AnthropicChatModel ready to send prompts to Anthropic's API
     */
    @Bean
    ChatModel anthropicChatModel(AiProperties properties) {
        return AnthropicChatModel.builder()
                .options(AnthropicChatOptions.builder()
                        .apiKey(properties.anthropic().apiKey())
                        .model(properties.anthropic().model())
                        .build())
                .build();
    }
}