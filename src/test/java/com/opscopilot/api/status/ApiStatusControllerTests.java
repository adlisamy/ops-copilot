package com.opscopilot.api.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Starts only Spring MVC components needed to test this controller, not the full application.
@WebMvcTest(ApiStatusController.class)
class ApiStatusControllerTests {

	@Autowired
	// Performs HTTP-like requests in memory, so this test does not need a running server.
	private MockMvc mockMvc;

	@Test
	void returnsThePublicApiStatus() throws Exception {
		mockMvc.perform(get("/api/v1/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.application").value("ops-copilot"))
				.andExpect(jsonPath("$.status").value("UP"));
	}
}