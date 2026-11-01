package com.opscopilot.api.chat;

import com.opscopilot.application.AssistantResult;
import com.opscopilot.application.ChatAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ChatController.class)
public class ChatControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ChatAssistant chatAssistant;

	@Test
	void shouldReturnStructuredResponse() throws Exception {
		AssistantResult mockResult = new AssistantResult(
				"Hello from the AI!",
				List.of("What can you do?", "Tell me a joke"),
				"friendly"
		);
		when(chatAssistant.ask(anyString())).thenReturn(mockResult);

		mockMvc.perform(post("/api/v1/chat")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"Hello, World!\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reply").value("Hello from the AI!"))
				.andExpect(jsonPath("$.suggestions").isArray())
				.andExpect(jsonPath("$.suggestions[0]").value("What can you do?"))
				.andExpect(jsonPath("$.suggestions[1]").value("Tell me a joke"));
	}

	@Test
	void shouldReturnEmptySuggestionsWhenNull() throws Exception {
		AssistantResult mockResult = new AssistantResult(
				"Hello!",
				null,
				"neutral"
		);
		when(chatAssistant.ask(anyString())).thenReturn(mockResult);

		mockMvc.perform(post("/api/v1/chat")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"Hi\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reply").value("Hello!"))
				.andExpect(jsonPath("$.suggestions").isArray())
				.andExpect(jsonPath("$.suggestions").isEmpty());
	}

	@Test
	void shouldUseMemoryWhenConversationIdProvided() throws Exception {
		AssistantResult mockResult = new AssistantResult(
				"I remember your name!",
				List.of("What else?"),
				"friendly"
		);
		when(chatAssistant.askWithMemory(anyString(), anyString())).thenReturn(mockResult);

		mockMvc.perform(post("/api/v1/chat")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"What is my name?\",\"conversationId\":\"conv-123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reply").value("I remember your name!"));
	}

}
