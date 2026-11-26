package com.opscopilot.config.ai;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientBuilderConfigurer;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for chat clients, conversation memory, tools, and retrieval-augmented generation.
 *
 * <p>This class connects the selected {@link ChatModel}, the auto-configured {@link VectorStore},
 * and cross-cutting advisors to clients suited to distinct application use cases. The pgvector
 * starter supplies the {@link VectorStore}; the embedding model is configured independently of
 * the chat model.</p>
 *
 * <h3>Architecture overview — how the pieces connect:</h3>
 * <pre>
 * Source documents -> split into chunks -> EmbeddingModel -> VectorStore (pgvector)
 * User question -> VectorStore similarity search -> retrieved context -> ChatModel -> answer
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
 *   <li><b>{@link VectorStore}</b> — the application-facing abstraction for storing embedded
 *       document chunks and retrieving similar chunks; this configuration does not depend on pgvector.</li>
 *   <li><b>{@link RetrievalAugmentationAdvisor}</b> — retrieves relevant chunks and adds them to
 *       a prompt before it reaches the chat model.</li>
 * </ul>
 *
 * <h3>ChatClient combinations:</h3>
 * <p>This configuration provides four explicitly qualified client combinations:</p>
 * <ul>
 *   <li><b>{@code chatClient}</b> — stateless chat, without retrieval or memory.</li>
 *   <li><b>{@code chatClientWithMemory}</b> — conversation memory, without retrieval.</li>
 *   <li><b>{@code chatClientWithRag}</b> — knowledge-base retrieval, without conversation memory.</li>
 *   <li><b>{@code chatClientWithMemoryAndRag}</b> — both advisors for conversational RAG.</li>
 * </ul>
 * <p>Keeping these combinations separate makes each behavior opt-in: ordinary chat does not
 * query the knowledge base, and memory is used only when a conversation ID is supplied.</p>
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
 * @see VectorStore
 * @see RetrievalAugmentationAdvisor
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
     * <p>This provider is registered on all four ChatClients so that configured tools are available
     * to plain, memory-enabled, RAG, and memory-plus-RAG requests.</p>
     *
     * @return a ToolCallbackProvider instance (currently empty — add tool methods via {@code toolObjects()})
     */
    @Bean
    public ToolCallbackProvider tools() {
        return MethodToolCallbackProvider.builder()
                .build();
    }
    /**
     * Create the advisor that retrieves relevant documents from the configured vector store.
     *
     * <p>The pgvector starter auto-configures the {@link VectorStore} from the datasource and
     * {@code spring.ai.vectorstore.pgvector.*} settings. Keeping this method dependent on the
     * interface allows the storage implementation to change without changing retrieval wiring.
     * No metadata filter is applied: ingestion sources may use different metadata schemas.</p>
     *
     * @param vectorStore the auto-configured store used for similarity search
     * @return an advisor that adds retrieved context to the prompt, allowing empty results
     */
    @Bean
    BaseAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter
                        .builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }
    /**
     * Create the default stateless ChatClient without memory or retrieval.
     *
     * <p>This client is used for ordinary one-shot requests and streaming. It deliberately
     * does not retrieve knowledge-base documents; use {@code chatClientWithRag} for RAG.</p>
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
     * @param chatModel the active LLM provider bean
     * @param chatClientBuilderConfigurer applies Spring AI customizers and observability
     * @param mcpTools callbacks available to the model
     * @return a stateless client without memory or retrieval advisors
     */
    @Bean
    @Primary
    ChatClient chatClient(ChatModel chatModel,
                          ChatClientBuilderConfigurer chatClientBuilderConfigurer,
                          ToolCallbackProvider mcpTools) {
        return chatClientBuilderConfigurer.configure(ChatClient.builder(chatModel)
                .defaultTools(mcpTools))
                .build();
    }

    /**
     * Create a stateless ChatClient that augments prompts with retrieved knowledge-base context.
     *
     * @param chatModel the active LLM provider bean
     * @param chatClientBuilderConfigurer applies Spring AI customizers and observability
     * @param mcpTools callbacks available to the model
     * @param retrievalAugmentationAdvisor retrieves and supplies relevant document chunks
     * @return a RAG-enabled client without conversation memory
     */
    @Bean
    @Qualifier("chatClientWithRag")
    ChatClient chatClientWithRag(ChatModel chatModel,
                                 ChatClientBuilderConfigurer chatClientBuilderConfigurer,
                                 ToolCallbackProvider mcpTools,
                                 BaseAdvisor retrievalAugmentationAdvisor) {
        return chatClientBuilderConfigurer.configure(ChatClient.builder(chatModel)
                .defaultTools(mcpTools)
                .defaultAdvisors(retrievalAugmentationAdvisor))
                .build();
    }

    /**
     * Create a ChatClient with conversation memory enabled and retrieval disabled.
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
     * <p>Retrieval is deliberately not included here. Use {@code chatClientWithMemoryAndRag}
     * when a conversation also needs knowledge-base context.</p>
     *
     * @param chatModel the active LLM provider bean
     * @param chatClientBuilderConfigurer applies Spring AI customizers and observability
     * @param chatMemory the conversation history storage bean
     * @param mcpTools callbacks available to the model
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

    /**
     * Create a ChatClient with both conversation memory and knowledge-base retrieval enabled.
     *
     * @param chatModel the active LLM provider bean
     * @param chatClientBuilderConfigurer applies Spring AI customizers and observability
     * @param chatMemory the conversation history storage bean
     * @param mcpTools callbacks available to the model
     * @param retrievalAugmentationAdvisor retrieves and supplies relevant document chunks
     * @return a client for conversational RAG requests
     */
    @Bean
    @Qualifier("chatClientWithMemoryAndRag")
    ChatClient chatClientWithMemoryAndRag(ChatModel chatModel,
                                          ChatClientBuilderConfigurer chatClientBuilderConfigurer,
                                          ChatMemory chatMemory,
                                          ToolCallbackProvider mcpTools,
                                          BaseAdvisor retrievalAugmentationAdvisor) {
        return chatClientBuilderConfigurer.configure(ChatClient.builder(chatModel)
                .defaultTools(mcpTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        retrievalAugmentationAdvisor))
                .build();
    }
}