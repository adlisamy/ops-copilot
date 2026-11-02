package com.opscopilot.config.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Conditional Spring configuration for the OpenAI LLM provider.
 *
 * <p>This class is only activated when {@code app.ai.provider=openai} in the application
 * configuration. It creates an {@link OpenAiChatModel} bean that implements the
 * {@link ChatModel} interface, allowing the rest of the application to interact with
 * OpenAI's API without knowing the provider-specific details.</p>
 *
 * <h3>How {@code @ConditionalOnProperty} works:</h3>
 * <p>This Spring Boot annotation makes the entire {@code @Configuration} class conditional.
 * The bean methods inside are only registered if the specified property matches the expected
 * value. This is how the project supports multiple LLM providers:</p>
 * <ul>
 *   <li>{@code app.ai.provider=openai} → this class activates</li>
 *   <li>{@code app.ai.provider=anthropic} → {@link AnthropicConfiguration} activates</li>
 *   <li>{@code app.ai.provider=ollama} → {@link OllamaConfiguration} activates</li>
 * </ul>
 * <p>Only one provider bean is created at a time, so {@code ChatClientConfiguration} can
 * inject a single {@code ChatModel} without ambiguity.</p>
 *
 * <h3>OpenAI-specific details:</h3>
 * <p>OpenAI uses a REST API at {@code api.openai.com}. The {@code OpenAiChatModel} handles
 * authentication, request formatting, and response parsing. Key options:</p>
 * <ul>
 *   <li><b>apiKey</b> — required; authenticates your requests and tracks usage for billing</li>
 *   <li><b>baseUrl</b> — optional; defaults to OpenAI's API; useful for proxies or
 *       OpenAI-compatible endpoints (e.g., Azure OpenAI, local vLLM servers)</li>
 *   <li><b>model</b> — the model name (e.g., "gpt-4o", "gpt-4o-mini"); determines
 *       capability and cost</li>
 * </ul>
 *
 * <h3>Cost considerations:</h3>
 * <p>OpenAI charges per token (input and output). GPT-4o is more capable but more expensive
 * than GPT-4o-mini. For learning and development, GPT-4o-mini is a cost-effective choice.</p>
 *
 * @see AiProperties
 * @see AnthropicConfiguration
 * @see OllamaConfiguration
 */
@Configuration
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiConfiguration {

    /**
     * Create the OpenAI ChatModel bean.
     *
     * <p>This method uses the builder pattern to configure the OpenAI client with credentials
     * and model settings from {@link AiProperties}. The returned bean implements {@link ChatModel},
     * the Spring AI interface that abstracts away provider-specific details.</p>
     *
     * <h3>What happens when a prompt is sent:</h3>
     * <ol>
     *   <li>Spring AI formats the messages (system + user) into OpenAI's API schema</li>
     *   <li>The request is sent to OpenAI's API with the API key for authentication</li>
     *   <li>OpenAI processes the request and returns the generated response</li>
     *   <li>{@code OpenAiChatModel} parses the response and returns it to Spring AI</li>
     * </ol>
     *
     * @param properties the application's AI configuration, containing the OpenAI API key,
     *                   base URL, and model name
     * @return a configured OpenAiChatModel ready to send prompts to OpenAI's API
     */
    @Bean
    ChatModel openAiChatModel(AiProperties properties) {
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .apiKey(properties.openai().apiKey())
                        .baseUrl(properties.openai().baseUrl())
                        .model(properties.openai().model())
                        .build())
                .build();
    }
}