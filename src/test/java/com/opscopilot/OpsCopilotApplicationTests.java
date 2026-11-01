package com.opscopilot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Starts the full Spring application context to catch wiring and configuration problems.
@SpringBootTest
@ActiveProfiles("test")
class OpsCopilotApplicationTests {

	@Test
	void contextLoads() {
	}

}
