package com.opscopilot.api.status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Marks this class as an HTTP API controller; returned values are serialized as JSON.
@RestController
// Defines the shared URL and JSON response type for every handler in this controller.
@RequestMapping(path = "/api/v1/status", produces = MediaType.APPLICATION_JSON_VALUE)
// Groups this endpoint under "Status" in the generated Swagger UI.
@Tag(name = "Status", description = "Application status endpoints")
public class ApiStatusController {

	// Adds a concise operation description to the generated OpenAPI document.
	@Operation(summary = "Get the API status")
	// Maps HTTP GET /api/v1/status to this method.
	@GetMapping
	public ApiStatusResponse getStatus() {
		return new ApiStatusResponse("ops-copilot", "UP");
	}

	// A record is a compact immutable DTO; Spring serializes its components to JSON properties.
	@Schema(description = "Minimal public status returned by the Ops Copilot API")
	public record ApiStatusResponse(
			@Schema(example = "ops-copilot") String application,
			@Schema(example = "UP") String status) {
	}
}