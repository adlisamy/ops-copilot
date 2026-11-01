package com.opscopilot.application;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

import reactor.core.publisher.Flux;

/**
 * Core application service that handles all interactions with the AI language model.
 *
 * <p>This class is the bridge between the REST API layer (controllers) and the
 * underlying LLM (Large Language Model) provided by Spring AI. It demonstrates
 * three common patterns for calling an LLM:</p>
 *
 * <ol>
 *   <li><b>Simple one-shot call</b> — send a message and get a structured result back.</li>
 *   <li><b>Conversational call with memory</b> — maintain context across multiple turns
 *       so the AI "remembers" earlier messages in the same conversation.</li>
 *   <li><b>Streaming call</b> — receive the response token-by-token in real time,
 *       useful for showing a "typing" effect to the user.</li>
 * </ol>
 *
 * <h3>Key Spring AI concepts used here:</h3>
 * <ul>
 *   <li><b>{@link ChatClient}</b> — the main entry point for building and executing
 *       prompts. Think of it as a fluent API that assembles a request to the LLM.</li>
 *   <li><b>System prompt</b> — a hidden instruction that sets the AI's role and behavior.
 *       Unlike the user message, the end user doesn't see this, but it strongly influences
 *       the response style and content.</li>
 *   <li><b>{@code .entity()}</b> — tells Spring AI to deserialize the LLM's text response
 *       into a typed Java object (here, {@link AssistantResult}). The LLM is instructed
 *       to output JSON matching the target type's structure.</li>
 *   <li><b>{@link ChatMemory}</b> — stores conversation history so multi-turn dialogues
 *       feel natural. Each conversation is identified by a unique ID.</li>
 *   <li><b>{@link Flux}</b> — a reactive stream from Project Reactor that emits each
 *       token as the LLM generates it, enabling real-time streaming responses.</li>
 * </ul>
 *
 * <h3>How a prompt is built (the fluent builder pattern):</h3>
 * <pre>
 * chatClient.prompt()           // Start building a new prompt
 *     .system(SYSTEM_PROMPT)    // Set the system-level instructions (role-playing)
 *     .user(userMessage)        // Add the user's actual question
 *     .call()                   // Execute the request (blocking)
 *     .entity(MyClass.class)    // Parse the response into a Java type
 * </pre>
 *
 * @see ChatClient
 * @see AssistantResult
 * @see ChatMemory
 */
@Service
public class ChatAssistant {

    /**
     * System-level instruction sent to the LLM with every request.
     *
     * <p>The system prompt defines the AI's persona and expected behavior.
     * In this case, it instructs the model to act as an IT operations assistant,
     * keep answers concise and technical, and always suggest follow-up questions.
     * This is a fundamental concept in prompt engineering — the system prompt
     * shapes every response without the user needing to repeat instructions.</p>
     */
    private static final String SYSTEM_PROMPT = "You are an IT operations assistant. Be concise, technical, and helpful. "
            + "Provide actionable advice and include 2-3 follow-up suggestions when appropriate.";


    /**
     * The Spring AI ChatClient — a fluent builder for constructing LLM requests.
     *
     * <p>This is the basic client WITHOUT memory. It is used for simple one-shot calls
     * and streaming, where no conversation history is needed.</p>
     */
    private final ChatClient chatClient;

    /**
     * A ChatClient with the MessageChatMemoryAdvisor enabled.
     *
     * <p>This client is used only for conversational requests where the AI needs to
     * remember previous messages. The advisor automatically retrieves and stores
     * conversation history based on the provided conversationId.</p>
     */
    private final ChatClient chatClientWithMemory;

    /**
     * Constructor-injection of both ChatClient beans.
     *
     * <p>Spring automatically provides both ChatClient instances when creating this service.
     * The {@code @Primary} annotation on the basic client ensures it is injected into
     * the first parameter, while the {@code @Qualifier} annotation identifies the
     * memory-enabled client for the second parameter.</p>
     *
     * @param chatClient the basic ChatClient without memory, for stateless requests
     * @param chatClientWithMemory the ChatClient with memory advisor, for conversational requests
     */
    public ChatAssistant(ChatClient chatClient,
                         @Qualifier("chatClientWithMemory") ChatClient chatClientWithMemory) {
        this.chatClient = chatClient;
        this.chatClientWithMemory = chatClientWithMemory;
    }

    /**
     * Send a single question to the LLM and receive a structured response.
     *
     * <p>This is the simplest way to interact with an LLM: one message in, one message out.
     * There is no conversation history — each call is independent and stateless.</p>
     *
     * <h3>What happens step by step:</h3>
     * <ol>
     *   <li>{@code chatClient.prompt()} — starts a new prompt builder</li>
     *   <li>{@code .system(SYSTEM_PROMPT)} — sets the AI's role and behavior instructions</li>
     *   <li>{@code .user(userMessage)} — adds the user's question as the input</li>
     *   <li>{@code .call()} — sends the request to the LLM and waits for the full response</li>
     *   <li>{@code .entity(AssistantResult.class, ...)} — parses the LLM's JSON response
     *       into a typed {@link AssistantResult} object, validating the schema</li>
     * </ol>
     *
     * <p>The {@code .entity()} method is powerful: it tells the LLM to format its output
     * as JSON matching the structure of {@link AssistantResult} (reply, suggestions, tone),
     * then automatically deserializes it. This is called "structured output" in the LLM world.</p>
     *
     * @param userMessage the user's question or request
     * @return a structured {@link AssistantResult} containing the reply, follow-up suggestions, and tone
     */
    public AssistantResult ask(String userMessage) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .entity(AssistantResult.class, ChatClient.EntityParamSpec::validateSchema);
    }

    /**
     * Send a question with conversation memory so the LLM remembers previous messages.
     *
     * <p>Unlike {@link #ask(String)}, this method maintains context across multiple turns.
     * When you pass the same {@code conversationId}, the LLM receives the full history
     * of that conversation, enabling natural multi-turn dialogue.</p>
     *
     * <h3>How conversation memory works:</h3>
     * <ol>
     *   <li>The {@code conversationId} is passed to the {@code MessageChatMemoryAdvisor}
     *       via {@code .advisors(advisor -> advisor.param(...))}</li>
     *   <li>The advisor looks up stored messages for that conversation ID
     *       from the {@link ChatMemory} bean</li>
     *   <li>It prepends the conversation history to the current prompt before sending it
     *       to the LLM</li>
     *   <li>After the LLM responds, both the user message and the assistant's reply
     *       are stored in memory for future turns</li>
     * </ol>
     *
     * <p>In this project, {@code MessageWindowChatMemory} is used — it keeps the last 20
     * messages per conversation. When the limit is reached, the oldest messages are dropped.
     * This is a simple but effective strategy for managing context window limits.</p>
     *
     * @param userMessage   the user's current message in the conversation
     * @param conversationId a unique identifier for this conversation thread;
     *                       using the same ID continues the conversation,
     *                       a new ID starts a fresh one
     * @return a structured {@link AssistantResult} with the reply, suggestions, and tone
     */
    public AssistantResult askWithMemory(String userMessage, String conversationId) {
        return chatClientWithMemory.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(AssistantResult.class, ChatClient.EntityParamSpec::validateSchema);
    }

    /**
     * Stream the LLM's response token by token for real-time display.
     *
     * <p>Instead of waiting for the full response, this method returns a reactive
     * {@link Flux} that emits each chunk of text as the LLM generates it. This is
     * how chat applications like ChatGPT show responses appearing word-by-word.</p>
     *
     * <h3>How streaming works:</h3>
     * <ol>
     *   <li>{@code .stream()} — tells Spring AI to use Server-Sent Events (SSE)
     *       instead of a single blocking call</li>
     *   <li>{@code .content()} — extracts just the text content from each event</li>
     *   <li>The returned {@code Flux<String>} emits each token chunk as it arrives</li>
     * </ol>
     *
     * <p>The controller layer maps this Flux to an SSE endpoint, so the browser
     * or client receives tokens in real time without waiting for the full response.</p>
     *
     * <p>Note: streaming returns raw text, not a structured {@link AssistantResult}.
     * If you need structured output, use {@link #ask(String)} or {@link #askWithMemory(String, String)} instead.</p>
     *
     * @param userMessage the user's question or request
     * @return a reactive stream emitting text chunks as the LLM generates them
     */
    public Flux<String> askStreaming(String userMessage) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .stream()
                .content();
    }
}