package com.opscopilot.api.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO (Data Transfer Object) returned by the chat endpoint.
 *
 * <p>This record defines the JSON structure that the server sends back to clients
 * after processing a chat request. Spring Boot automatically serializes this object
 * to JSON using Jackson.</p>
 *
 * <h3>JSON structure:</h3>
 * <pre>
 * {
 *   "reply": "To check disk usage on Linux, run 'df -h'. This shows all mounted filesystems in human-readable format.",
 *   "suggestions": [
 *     "How do I clean up disk space?",
 *     "What does 'df -h' output mean?",
 *     "How can I check CPU usage?"
 *   ]
 * }
 * </pre>
 *
 * <h3>How this relates to AssistantResult:</h3>
 * <p>The {@link com.opscopilot.application.AssistantResult} is the internal type returned by
 * {@link com.opscopilot.application.ChatAssistant} after the LLM responds. This {@code ChatResponse}
 * is the external API type. The controller maps between them:</p>
 * <pre>
 * AssistantResult (internal) → ChatResponse (external API)
 *   .reply              → .reply
 *   .suggestions        → .suggestions
 *   .tone               → (not exposed in API response)
 * </pre>
 * <p>The {@code tone} field is intentionally not included in the API response. It is an
 * internal metadata field that could be used for future features (e.g., UI styling)
 * but is not yet part of the public API.</p>
 *
 * <h3>Why separate internal and external types?</h3>
 * <p>Keeping internal types (AssistantResult) separate from API types (ChatResponse) is
 * a best practice because:</p>
 * <ul>
 *   <li><b>API stability</b> — you can change internal types without breaking the API contract</li>
 *   <li><b>Security</b> — you control exactly what data is exposed to clients</li>
 *   <li><b>Flexibility</b> — the API can evolve independently of the internal logic</li>
 * </ul>
 *
 * <h3>OpenAPI documentation:</h3>
 * <p>The {@code @Schema} annotations from Swagger/OpenAPI generate the API documentation
 * visible in Swagger UI (typically at {@code /swagger-ui.html}). They describe each
 * field's purpose and provide example values for the interactive documentation.</p>
 *
 * @param reply        the AI's text response to the user's question
 * @param suggestions  follow-up questions or related actions suggested by the AI;
 *                     displayed as clickable options in a chat UI to encourage continued interaction
 *
 * @see ChatRequest
 * @see com.opscopilot.application.AssistantResult
 * @see ChatController#prompt(ChatRequest)
 */
@Schema(description = "Chat response from the AI assistant")
public record ChatResponse(
        /**
         * The AI's text response to the user's question.
         *
         * <p>This is the primary content returned by the LLM. The system prompt
         * instructs the model to be concise, technical, and actionable.</p>
         */
        @Schema(description = "The AI's reply", example = "Hello! How can I help you?")
        String reply,

        /**
         * Follow-up questions or related actions suggested by the AI.
         *
         * <p>The system prompt asks the LLM to provide 2-3 follow-up suggestions.
         * In a chat UI, these are typically displayed as clickable buttons or chips
         * that the user can select to continue the conversation without typing.</p>
         *
         * <p>This list is never null — if the LLM fails to provide suggestions,
         * the controller returns an empty list instead.</p>
         */
        @Schema(description = "Suggested follow-up questions", example = "[\"What can you do?\", \"Tell me a joke\"]")
        List<String> suggestions
) {
}
