package com.opscopilot.api.chat;

import com.opscopilot.application.AssistantResult;
import com.opscopilot.application.ChatAssistant;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * REST controller that exposes the AI chat functionality as HTTP endpoints.
 *
 * <p>This is the entry point for all external requests. It sits at the API layer
 * and delegates to {@link ChatAssistant} for the actual LLM interaction. This
 * separation of concerns is a key Spring Boot pattern: controllers handle HTTP
 * (requests/responses), while services handle business logic.</p>
 *
 * <h3>Endpoints provided:</h3>
 * <table>
 *   <tr>
 *     <th>Method</th>
 *     <th>Path</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>POST</td>
 *     <td>/api/v1/chat</td>
 *     <td>Send a message and receive a structured JSON response (with optional conversation memory)</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/v1/chat/stream?message=...</td>
 *     <td>Stream a response token-by-token using Server-Sent Events (SSE)</td>
 *   </tr>
 * </table>
 *
 * <h3>How Spring MVC processes a request:</h3>
 * <ol>
 *   <li>Spring receives the HTTP request and matches the URL to {@code @RequestMapping}</li>
 *   <li>It deserializes the JSON body into a {@link ChatRequest} object</li>
 *   <li>The matched handler method is called with the deserialized parameters</li>
 *   <li>The return value ({@link ChatResponse} or {@link Flux}) is serialized back to JSON/SSE</li>
 * </ol>
 *
 * <h3>Example curl commands:</h3>
 * <pre>
 * # Simple chat (no memory)
 * curl -X POST http://localhost:18080/api/v1/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message": "How do I check disk usage?"}'
 *
 * # Chat with conversation memory
 * curl -X POST http://localhost:18080/api/v1/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message": "How do I check disk usage?", "conversationId": "conv-1"}'
 *
 * # Streaming response
 * curl -N http://localhost:18080/api/v1/chat/stream?message="Explain%20Docker"
 * </pre>
 *
 * <h3>Key Spring annotations used:</h3>
 * <ul>
 *   <li><b>{@code @RestController}</b> — combines {@code @Controller} and {@code @ResponseBody};
 *       every method returns data (JSON), not a view template</li>
 *   <li><b>{@code @RequestMapping}</b> — sets the base path for all endpoints in this class</li>
 *   <li><b>{@code @PostMapping}</b> — maps HTTP POST requests to a method</li>
 *   <li><b>{@code @GetMapping}</b> — maps HTTP GET requests to a method</li>
 *   <li><b>{@code @RequestBody}</b> — deserializes the JSON request body into a Java object</li>
 *   <li><b>{@code @RequestParam}</b> — extracts a query parameter from the URL</li>
 * </ul>
 *
 * @see ChatAssistant
 * @see ChatRequest
 * @see ChatResponse
 */
@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "Chat endpoints")
public class ChatController {
    private final ChatAssistant chatAssistant;

    /**
     * Constructor-injection of the ChatAssistant service.
     *
     * <p>Spring automatically provides the ChatAssistant bean. This keeps the controller
     * thin — it only handles HTTP concerns, while ChatAssistant handles LLM interaction.</p>
     *
     * @param chatAssistant the service that communicates with the LLM
     */
    public ChatController(ChatAssistant chatAssistant) {
        this.chatAssistant = chatAssistant;
    }

    /**
     * Handle a chat request and return a structured response.
     *
     * <p>This is the main chat endpoint. It supports two modes:</p>
     * <ol>
     *   <li><b>Stateless mode</b> — if no {@code conversationId} is provided, each request
     *       is independent. The LLM has no memory of previous messages.</li>
     *   <li><b>Conversational mode</b> — if a {@code conversationId} is provided, the LLM
     *       receives the conversation history, enabling multi-turn dialogue.</li>
     * </ol>
     *
     * <h3>Request/Response flow:</h3>
     * <pre>
     * Client                          Server                          LLM
     *   |--- POST /api/v1/chat -------->|                               |
     *   |    (ChatRequest JSON)         |--- askWithMemory() ---------->|
     *   |                               |    (prompt + history)         |
     *   |                               |<-- (AssistantResult JSON) ----|
     *   |<-- 200 OK (ChatResponse) -----|                               |
     * </pre>
     *
     * <h3>Conversation ID strategy:</h3>
     * <p>The client generates and manages conversation IDs. A simple UUID works well:
     * <pre>
     * {"message": "Hello", "conversationId": "550e8400-e29b-41d4-a716-446655440000"}
     * </pre>
     * Using the same ID continues the conversation; a new ID starts a fresh one.
     * The server stores conversation history in memory (via {@code MessageWindowChatMemory}).</p>
     *
     * @param request the chat request containing the user's message and optional conversation ID
     * @return a {@link ChatResponse} with the AI's reply and follow-up suggestions
     */
    @Operation(summary = "Send a chat request and receive a structured response")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse prompt(@RequestBody ChatRequest request) {
        AssistantResult result;
        if (request.conversationId() != null && !request.conversationId().isBlank()) {
            result = chatAssistant.askWithMemory(request.message(), request.conversationId());
        } else {
            result = chatAssistant.ask(request.message());
        }
        return new ChatResponse(result.reply(), result.suggestions() != null ? result.suggestions() : List.of());
    }

    /**
     * Stream a chat response in real time using Server-Sent Events (SSE).
     *
     * <p>Unlike the POST endpoint that waits for the full response, this endpoint
     * returns tokens as they are generated by the LLM. The client receives a stream
     * of text chunks, enabling a "typing effect" in the UI.</p>
     *
     * <h3>How Server-Sent Events (SSE) works:</h3>
     * <ol>
 *   <li>The client opens a GET request to {@code /api/v1/chat/stream}</li>
 *   <li>The server keeps the HTTP connection open and sends data chunks as they arrive</li>
 *   <li>Each chunk is formatted as an SSE event: {@code data: token\n\n}</li>
 *   <li>When the LLM finishes, the server closes the connection</li>
 * </ol>
 *
 * <h3>Why use {@code Flux<String>}?</h3>
 * <p>{@code Flux} is a reactive stream from Project Reactor. Spring WebFlux automatically
 * maps a {@code Flux<String>} return type to SSE. Each element emitted by the Flux
 * becomes an SSE event sent to the client. This is non-blocking — the server doesn't
 * hold a thread while waiting for the next token.</p>
 *
 * <h3>Client-side consumption:</h3>
 * <pre>
 * // JavaScript EventSource API
 * const eventSource = new EventSource('/api/v1/chat/stream?message=Hello');
 * eventSource.onmessage = (event) => {
 *     console.log(event.data);  // Each token chunk
 * };
 * </pre>
 *
 * @param message the user's question or request (passed as a query parameter)
 * @return a reactive stream of text chunks emitted as SSE events
 */
    @Operation(summary = "Stream a chat response in real-time using Server-Sent Events")
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String message) {
        return chatAssistant.askStreaming(message);
    }
}
