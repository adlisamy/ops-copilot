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

## Choosing a Model

The application supports three AI providers. Edit `src/main/resources/application.properties` to switch between them.

### Option 1: Ollama (local, free, privacy-first)

**Best for:** learning, development, offline use, data privacy.

Runs models on your own machine. No API keys or internet connection required after the model is downloaded.

**Available small models for CPU-only systems:**

| Model | Size | RAM needed | Speed (CPU) | Quality |
| --- | --- | --- | --- | --- |
| `qwen3:0.6b` | 523 MB | ~2 GB | ~35 tok/s | Basic |
| `qwen3:1.7b` | 1 GB | ~3 GB | ~15 tok/s | Good |
| `qwen3:4b` | 2.6 GB | ~4 GB | ~8 tok/s | Better |
| `gemma3:1b` | 806 MB | ~2 GB | ~19 tok/s | Basic |

**To switch models:**

```bash
# Pull a smaller model
ollama pull qwen3:0.6b

# Update application.properties
# app.ai.ollama.model=qwen3:0.6b
```

**Default:** `qwen3:0.6b` (fastest on CPU, ~35 tokens/second).

### Option 2: OpenAI (cloud, requires API key)

**Best for:** production quality, reliable performance, no local hardware requirements.

**Cost:** ~$0.15-0.30 per 1000 requests (gpt-4o-mini).

**To enable:**

1. Get an API key from [platform.openai.com](https://platform.openai.com/api-keys)
2. Uncomment the OpenAI section in `application.properties`:
   ```properties
   app.ai.provider=openai
   app.ai.openai.apiKey=${OPENAI_API_KEY:your-api-key-here}
   app.ai.openai.model=gpt-4o-mini
   ```
3. Set the environment variable:
   ```bash
   export OPENAI_API_KEY=sk-proj-your-key-here
   ```
4. Restart the application

**Recommended models:** `gpt-4o-mini` (fast and cheap), `gpt-4o` (higher quality).

### Option 3: Anthropic (cloud, requires API key)

**Best for:** high-quality responses, complex reasoning tasks.

**Cost:** ~$3-15 per million tokens (Claude Sonnet).

**To enable:**

1. Get an API key from [console.anthropic.com](https://console.anthropic.com/settings/keys)
2. Uncomment the Anthropic section in `application.properties`:
   ```properties
   app.ai.provider=anthropic
   app.ai.anthropic.apiKey=${ANTHROPIC_API_KEY:your-api-key-here}
   app.ai.anthropic.model=claude-sonnet-4-20250514
   ```
3. Set the environment variable:
   ```bash
   export ANTHROPIC_API_KEY=sk-ant-your-key-here
   ```
4. Restart the application

### Comparison

| Factor | Ollama (local) | OpenAI | Anthropic |
| --- | --- | --- | --- |
| Cost | Free | Pay per use | Pay per use |
| Speed | Depends on hardware | Fast (~1s) | Fast (~1s) |
| Privacy | 100% local | Data sent to cloud | Data sent to cloud |
| Quality | Good (4B+) | Excellent | Excellent |
| Setup | Pull model | API key | API key |
| Internet | Not required | Required | Required |

## Validate

```bash
./gradlew check
```

## Safety baseline

This project will begin with read-only guidance and mock integrations. It must not execute arbitrary model-generated commands or make state-changing production actions without explicit future authorization and approval controls.