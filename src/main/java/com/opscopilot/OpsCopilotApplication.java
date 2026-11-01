package com.opscopilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Enables component scanning, auto-configuration, and Java-based configuration for this application.
@SpringBootApplication
public class OpsCopilotApplication {

	// Java entry point; Spring Boot creates and starts the application context from this class.
	static void main(String[] args) {
		SpringApplication.run(OpsCopilotApplication.class, args);
	}

}
