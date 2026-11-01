package com.opscopilot.api.chat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO (Data Transfer Object) for the chat endpoint.
 *
 * <p>This record defines the JSON structure that clients send to the POST /api/v1/chat endpoint.
 * Spring Boot automatically deserializes the JSON body into this object using Jackson.</p>
 *
 * <h3>JSON structure:</h3>
 * <pre>
 * {
 *   "message": "How do I check disk usage on Linux?",
 *   "conversationId": "conv-123"   // optional
 * }
 * </pre>
 *
 * <h3>Why use a Java record for DTOs?</h3>
 * <p>Records are ideal for DTOs because they are immutable, concise, and automatically
 * provide accessor methods. Jackson (Spring's JSON library) maps JSON properties to
 * record constructor parameters by name, so no additional annotations are needed.</p>
 *
 * <h3>Conversation ID usage:</h3>
 * <p>The {@code conversationId} is optional. When provided, it enables multi-turn
 * conversations where the LLM remembers previous messages. The client is responsible
 * for generating and managing these IDs — a UUID is a good choice:</p>
 * <pre>
 * // First message — start a new conversation
 * {"message": "Hello", "conversationId": "550e8400-e29b-41d4-a716-446655440000"}
 *
 * // Follow-up — same conversation ID continues the thread
 * {"message": "Can you explain that?", "conversationId": "550e8400-e29b-41d4-a716-446655440000"}
 *
 * // New conversation — different ID starts fresh
 * {"message": "What is Docker?", "conversationId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8"}
 * </pre>
 *
 * <h3>OpenAPI documentation:</h3>
 * <p>The {@code @Schema} annotations from Swagger/OpenAPI generate the API documentation
 * visible in Swagger UI (typically at {@code /swagger-ui.html}). They describe each
 * field's purpose and provide example values for the interactive documentation.</p>
 *
 * @param message        the user's question or message to send to the AI assistant (required)
 * @param conversationId optional unique identifier for a conversation thread;
 *                       omit or set to null for a stateless, one-off request
 *
 * @see ChatResponse
 * @see ChatController#prompt(ChatRequest)
 */
@Schema(description = "Chat request containing the user's message")
public record ChatRequest(
        /**
         * The user's question or message to send to the AI assistant.
         *
         * <p>This is the only required field. It can be a question, a command,
         * or any text input that the LLM should process.</p>
         */
        @Schema(description = "The user's message", example = "How are you?")
        String message,

        /**
         * Optional conversation identifier for multi-turn dialogue.
         *
         * <p>When provided, the LLM receives the conversation history associated
         * with this ID, enabling context-aware follow-up responses. When omitted,
         * each request is treated as an independent, stateless interaction.</p>
         *
         * <p>The server stores conversation history in memory with a sliding window
         * of 20 messages. History is lost when the application restarts.</p>
         */
        @Schema(description = "Optional conversation ID for multi-turn conversations", example = "conv-123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String conversationId
) {
}
