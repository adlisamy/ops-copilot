# Ops Copilot

Ops Copilot is a learning project for building a safe, production-minded AI assistant with Java and Spring Boot. It will help engineers investigate operational issues using curated runbooks, user-provided logs, and controlled read-only tools.

## Current status

**LLM and RAG foundation.** The API supports configurable chat providers, optional conversation memory, and an explicit knowledge-base chat route backed by Spring AI's `VectorStore` abstraction and pgvector. Runbook loading is intentionally separate from chat requests; no operational-system integration or automatic infrastructure action is implemented.

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
- [RAG setup and learnings](docs/rag-setup-and-learnings.md)

## Run locally

Requirements: a Java 25-compatible JDK available to Gradle, PostgreSQL with pgvector at
`localhost:5431` with an `ops_vector_db` database, and Ollama at `localhost:11434` with the
configured chat and embedding models available. The dev profile uses Ollama for chat and
embeddings; see [Retrieval-augmented chat](#retrieval-augmented-chat).

```bash
export OPS_COPILOT_DB_PASSWORD='your-local-database-password'
ollama pull llama3
ollama pull nomic-embed-text
./gradlew bootRun
```

When the application is running locally:

| Resource | Local URL |
| --- | --- |
| API status | `http://localhost:18080/api/v1/status` |
| Health | `http://localhost:18080/actuator/health` |
| OpenAPI JSON | `http://localhost:18080/v3/api-docs` |
| Swagger UI | `http://localhost:18080/swagger-ui/index.html` |

## Retrieval-augmented chat

The `dev` profile uses PostgreSQL/pgvector for document vectors, Ollama's `llama3` model for chat,
and `nomic-embed-text` for embeddings. Start PostgreSQL with pgvector support and a database named
`ops_vector_db`, and ensure both Ollama models are available as shown above.

The `nomic-embed-text` output is configured to 768 dimensions, matching
`spring.ai.vectorstore.pgvector.dimensions` in `application-dev.properties`. The same embedding
model must be used for ingestion and search. Spring AI's pgvector starter creates the configured
`ops` schema and vector table at startup when schema initialization is enabled. Vectors from a
different embedding model are not compatible, even when their dimensions match; replace and
re-ingest stored documents when changing embedding models.

To load the curated runbooks from `docs/runbooks/` into pgvector, start the application with the
one-time startup ingestion option:

```bash
./gradlew bootRun --args='--app.knowledge-base.ingest-docs-on-startup=true'
```

The dev-only loader recursively reads `.md` files from `docs/runbooks/`, attaches their relative
paths as `source` metadata, splits the documents, and stores the chunks through
`KnowledgeBaseIngestionService` and the `VectorStore` abstraction. The option is off by default.
Ingestion is add-only, so don't repeat it until duplicate/replacement handling is implemented;
rerunning can add duplicate chunks.

To verify real retrieval against the configured PostgreSQL and Ollama services, run:

```bash
RAG_INTEGRATION_TESTS=true ./gradlew test --tests '*KnowledgeBaseRetrievalIntegrationTest'
```

The test embeds the runbooks, searches for CrashLoopBackOff troubleshooting information, and asserts
that a matching runbook chunk is returned. It uses the active dev database and leaves the ingested
chunks there. After ingestion, send questions that should use the knowledge base to
`POST /api/v1/chat/rag`. Supplying a `conversationId` enables memory in addition to retrieval; the
ordinary `POST /api/v1/chat` route remains independent of the knowledge base.

```bash
curl -X POST http://localhost:18080/api/v1/chat/rag \
  -H "Content-Type: application/json" \
  -d '{"message": "How do I restart the service?"}'
```

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

The application supports local and hosted AI providers. The checked-in configuration uses Ollama
for chat and embeddings. Chat and embeddings are configured independently; the Gemini properties
below and in `application-dev.properties` are commented reference settings.

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

**Configured chat model:** `llama3` in `application.properties`.

### Option 2: OpenAI-compatible provider (cloud, requires API key)

**Best for:** production quality, reliable performance, no local hardware requirements.

**Cost:** ~$0.15-0.30 per 1000 requests (gpt-4o-mini).

**To enable:**

1. Get an API key from [platform.openai.com](https://platform.openai.com/api-keys)
2. Set the OpenAI-compatible provider properties in `application.properties`:
   ```properties
   app.ai.provider=openai
   app.ai.openai.base-url=https://api.openai.com/v1
   app.ai.openai.api-key=${OPENAI_API_KEY}
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

### Gemini API (optional reference; inactive by default)

The successful end-to-end RAG check used Gemini for hosted chat and Google GenAI for embeddings.
The example settings are commented out so the default setup remains local Ollama. For Gemini chat,
uncomment the OpenAI-compatible properties in `application.properties`, set
`app.ai.provider=openai`, and export a valid `GEMINI_API_KEY` outside the repository:

```bash
export GEMINI_API_KEY='your-api-key'
./gradlew bootRun
```

For Gemini embeddings, add the Spring AI Google GenAI embedding starter and uncomment its settings
in `application-dev.properties`; the example uses 768 dimensions. Changing embedding models
requires replacing and re-ingesting stored vectors. Hosted requests depend on valid credentials,
model availability, quotas, and provider service availability. See
[RAG setup and learnings](docs/rag-setup-and-learnings.md) for what was verified and the observed
failure modes.

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