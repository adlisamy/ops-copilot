# Ops Copilot

Ops Copilot is a learning project for building a safe, production-minded AI assistant with Java and Spring Boot. It will help engineers investigate operational issues using curated runbooks, user-provided logs, and controlled read-only tools.

## Current status

**Week 0 — API foundation.** The repository exposes a minimal status endpoint, Actuator health endpoint, OpenAPI document, and Swagger UI. No LLM, vector database, external operational integration, or automatic infrastructure action is implemented.

## Module layout

```text
src/main/java/com/opscopilot/
├── api/             HTTP controllers, DTOs, and exception handlers
├── application/     use cases and workflow orchestration
├── config/          Spring configuration and application properties
├── domain/          framework-independent models and interfaces
├── infrastructure/  model, retrieval, and external-system adapters
└── shared/          cross-cutting validation, errors, and observability
```

## Documentation

- [Learning roadmap](AI_AGENTIC_LEARNING_ROADMAP.md)
- [Project documentation](docs/README.md)
- [Architecture notes](docs/architecture/README.md)
- [Runbook guidance](docs/runbooks/README.md)

## Run locally

Requirements: a Java 25-compatible JDK available to Gradle.

```bash
./gradlew bootRun
```

When the application is running locally:

| Resource | Local URL |
| --- | --- |
| API status | `http://localhost:8080/api/v1/status` |
| Health | `http://localhost:8080/actuator/health` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |

## API Examples

### Simple chat (no memory)

Sends a single question and receives a structured JSON response. Each request is independent — the AI has no memory of previous messages.

```bash
curl -X POST http://localhost:18080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "How do I check disk usage on Linux?"}'
```

**Response:**
```json
{
  "reply": "Run 'df -h' to check disk usage in human-readable format...",
  "suggestions": [
    "How do I clean up disk space?",
    "What does 'df -h' output mean?"
  ]
}
```

### Chat with conversation memory

Pass a `conversationId` to maintain context across multiple turns. Use the same ID to continue the conversation.

```bash
# First message — starts a new conversation
curl -X POST http://localhost:18080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "My server is running slow", "conversationId": "conv-1"}'

# Follow-up — continues the same conversation
curl -X POST http://localhost:18080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What should I check first?", "conversationId": "conv-1"}'
```

The AI will remember the context from the first message when answering the follow-up.

### Streaming response

Receive the response token-by-token in real time using Server-Sent Events (SSE). Use `-N` to disable curl's buffering so you see output as it arrives.

```bash
curl -N http://localhost:18080/api/v1/chat/stream?message="Explain%20Docker%20containers"
```

Output appears word-by-word as the LLM generates it:

```
data: Docker
data:  containers
data:  are
data:  lightweight
data: ...
```

### Status endpoint

Quickly verify the API is running:

```bash
curl http://localhost:18080/api/v1/status
```

## Validate

```bash
./gradlew check
```

## Safety baseline

This project will begin with read-only guidance and mock integrations. It must not execute arbitrary model-generated commands or make state-changing production actions without explicit future authorization and approval controls.