package com.opscopilot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Declares a class that contributes Spring-managed configuration objects.
@Configuration
public class OpenApiConfiguration {

	// Registers shared metadata used when Springdoc generates /v3/api-docs and Swagger UI.
	@Bean
	OpenAPI opsCopilotOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Ops Copilot API")
				.version("v1")
				.description("API for the Ops Copilot learning project."));
	}
}