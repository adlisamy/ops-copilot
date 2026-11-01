package com.opscopilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Enables component scanning, auto-configuration, and Java-based configuration for this application.
@SpringBootApplication(excludeName = {
        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration",
        "org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration",
        "org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration"
})
@ConfigurationPropertiesScan
public class OpsCopilotApplication {

    // Java entry point; Spring Boot creates and starts the application context from this class.
    static void main(String[] args) {
        SpringApplication.run(OpsCopilotApplication.class, args);
    }

}
