package com.opscopilot.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientBuilderConfigurer;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration that creates the core Spring AI beans: {@link ChatClient} and {@link ChatMemory}.
 *
 * <p>This class is the wiring layer that connects the LLM provider (ChatModel) to the application.
 * It is essential for understanding how Spring AI components fit together.</p>
 *
 * <h3>Architecture overview — how the pieces connect:</h3>
 * <pre>
 * ┌─────────────────────────────────────────────────┐
 * │  ChatClient (this class creates 2 instances)    │
 * │  ┌───────────────────────────────────────────┐  │
 * │  │  ChatModel (created by provider config)   │  │
 * │  │  - OpenAiConfiguration                    │  │
 * │  │  - AnthropicConfiguration                 │  │
 * │  │  - OllamaConfiguration                    │  │
 * │  └───────────────────────────────────────────┘  │
 * │  ┌───────────────────────────────────────────┐  │
 * │  │  Advisors (cross-cutting concerns)        │  │
 * │  │  - MessageChatMemoryAdvisor (memory only) │  │
 * │  └───────────────────────────────────────────┘  │
 * └─────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>Key Spring AI concepts:</h3>
 * <ul>
 *   <li><b>{@link ChatModel}</b> — the interface that represents an LLM provider (OpenAI, Anthropic,
 *       Ollama, etc.). Each provider has its own implementation. The {@code ChatClient} wraps
 *       this interface so your application code doesn't depend on a specific provider.</li>
 *   <li><b>{@link ChatClient}</b> — the main API for building and executing prompts. It sits
 *       above ChatModel and adds features like advisors, structured output, and streaming.</li>
 *   <li><b>{@link ChatMemory}</b> — an interface for storing conversation history. Implementations
 *       can be in-memory (as here) or backed by a database for persistence.</li>
 * </ul>
 *
 * <h3>Two ChatClient instances:</h3>
 * <p>This configuration creates two ChatClient beans:</p>
 * <ul>
 *   <li><b>{@code chatClient}</b> — a basic client with NO memory advisor. Used for simple,
 *       stateless one-shot requests where conversation history is not needed.</li>
 *   <li><b>{@code chatClientWithMemory}</b> — a client with the {@code MessageChatMemoryAdvisor}
 *       enabled. Used for conversational requests where the AI needs to remember previous messages.</li>
 * </ul>
 * <p>Separating them avoids the common pitfall of applying a memory advisor globally: if every
 * request goes through the memory advisor, then stateless requests (without a conversationId)
 * will fail with "conversationId cannot be null". By keeping them separate, each use case
 * gets the right behavior.</p>
 *
 * <h3>Why use Advisors?</h3>
 * <p>Advisors are similar to Spring's AOP (Aspect-Oriented Programming) interceptors. They wrap
 * the prompt execution pipeline, allowing you to modify requests and responses transparently.
 * The {@code MessageChatMemoryAdvisor} is the most commonly used advisor — it gives your chat
 * application the ability to remember previous messages without writing any memory management code.</p>
 *
 * @see ChatClient
 * @see ChatMemory
 * @see ChatModel
 */
@Configuration
public class ChatClientConfiguration {

    /**
     * Create the ChatMemory bean — stores conversation history in memory.
     *
     * <p>{@code MessageWindowChatMemory} is the simplest implementation: it keeps a fixed
     * number of recent messages per conversation. When the limit is reached, the oldest
     * messages are discarded (sliding window strategy).</p>
     *
     * <h3>Configuration details:</h3>
     * <ul>
     *   <li><b>maxMessages(20)</b> — keeps the last 20 messages (10 user + 10 assistant turns)
     *       per conversation. This balances context quality with token cost.</li>
     *   <li><b>In-memory storage</b> — messages are stored in a {@code ConcurrentHashMap}.
     *       They are lost when the application restarts. For production, consider a
     *       database-backed implementation like {@code RedisChatMemory}.</li>
     * </ul>
     *
     * <h3>Alternative ChatMemory implementations:</h3>
     * <ul>
     *   <li>{@code RedisChatMemory} — persists conversations in Redis for durability</li>
     *   <li>Custom implementations — you can implement {@code ChatMemory} interface to use
     *       any storage backend (PostgreSQL, MongoDB, etc.)</li>
     * </ul>
     *
     * @return a ChatMemory instance configured to keep the last 20 messages per conversation
     */
    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }
    /**
     * Create the ToolCallbackProvider bean — registers callable tools for the LLM.
     *
     * <p>{@code MethodToolCallbackProvider} exposes Java methods as tools that the LLM can
     * invoke during a conversation. This enables function calling, where the model can
     * request the application to perform actions (fetch data, run commands, etc.) and
     * use the results to formulate its response.</p>
     *
     * <h3>How tool calling works:</h3>
     * <ol>
     *   <li>The LLM detects that a tool should be called based on the prompt or system instructions</li>
     *   <li>Spring AI invokes the corresponding Java method with the arguments provided by the model</li>
     *   <li>The method's return value is fed back to the LLM as context for the final response</li>
     * </ol>
     *
     * <p>This provider is registered on both {@link #chatClient(ChatModel, ChatClientBuilderConfigurer, ToolCallbackProvider)}
     * and {@link #chatClientWithMemory(ChatModel, ChatClientBuilderConfigurer, ChatMemory, ToolCallbackProvider)} so that
     * tools are available for both stateless and conversational requests.</p>
     *
     * @return a ToolCallbackProvider instance (currently empty — add tool methods via {@code toolObjects()})
     */
    @Bean
    public ToolCallbackProvider tools() {
        return MethodToolCallbackProvider.builder()
                .build();
    }
    /**
     * Create the default ChatClient bean — a basic client with NO memory advisor.
     *
     * <p>This client is used for simple, stateless one-shot requests and streaming.
     * It has no conversation memory, so each request is independent.</p>
     *
     * <p>Marked as {@code @Primary} so it is the default ChatClient injected when
     * no specific qualifier is used.</p>
     *
     * <h3>Why ChatClient instead of ChatModel directly?</h3>
     * <p>ChatModel is the low-level interface for sending messages to an LLM. ChatClient wraps
     * it with a fluent, high-level API that makes common patterns (system prompts, structured
     * output, streaming, memory) much easier to use. It is the recommended entry point in
     * Spring AI — you should rarely need to use ChatModel directly.</p>
     *
     * @param chatModel the LLM provider bean (OpenAI, Anthropic, or Ollama) — only one is
     *                  active based on the {@code app.ai.provider} configuration property
     * @return a basic ChatClient without memory, ready for stateless prompts
     */
    @Bean
    @Primary
    ChatClient chatClient(ChatModel chatModel,
                          ChatClientBuilderConfigurer chatClientBuilderConfigurer,
                          ToolCallbackProvider mcpTools) {
        return chatClientBuilderConfigurer.configure(ChatClient
                .builder(chatModel)
                .defaultTools(mcpTools)
        ).build();
    }

    /**
     * Create a ChatClient bean with conversation memory enabled.
     *
     * <p>This client includes the {@link MessageChatMemoryAdvisor} that automatically
     * manages conversation history. It is used only for multi-turn conversational requests
     * where a {@code conversationId} is provided.</p>
     *
     * <h3>How the memory advisor works:</h3>
     * <ol>
     *   <li>When a prompt is sent with a {@code conversationId} parameter, the advisor
     *       retrieves stored messages for that conversation from {@link ChatMemory}</li>
     *   <li>It prepends the conversation history to the prompt before sending it to the LLM</li>
     *   <li>After the LLM responds, both the user message and assistant reply are stored
     *       in memory for future turns</li>
     * </ol>
     *
     * <p>By separating this from the default {@link #chatClient(ChatModel,ChatClientBuilderConfigurer)}, we avoid the
     * common pitfall where stateless requests fail because no conversationId is provided.</p>
     *
     * @param chatModel  the LLM provider be
     * @param chatClientBuilderConfigurer ChatClientBuilderConfigurer.configure(builder) applies all registered ChatClientBuilderCustomizer beans and wires the same observability behavior normally provided by Spring AI auto-configuration.
     * @param chatMemory the conversation history storage bean
     * @return a ChatClient with memory advisor enabled for conversational requests
     */
    @Bean
    @Qualifier("chatClientWithMemory")
    ChatClient chatClientWithMemory(ChatModel chatModel,
                                    ChatClientBuilderConfigurer chatClientBuilderConfigurer,
                                    ChatMemory chatMemory,
                                    ToolCallbackProvider mcpTools) {
        return chatClientBuilderConfigurer.configure(
                ChatClient.builder(chatModel)
                        .defaultTools(mcpTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                    )

                ).build();
    }
}