# RAG Setup and Learnings

## Current configuration

The checked-in dev setup uses local Ollama for both chat and embeddings, with PostgreSQL/pgvector
for vector storage. Chat uses `llama3`; embeddings use `nomic-embed-text` with 768 dimensions. The
vector store is configured for the `ops` schema, cosine distance, and HNSW indexing. PostgreSQL is
expected at `localhost:5431`, and Ollama at `localhost:11434`.

Install/pull the models and start the application:

```bash
export OPS_COPILOT_DB_PASSWORD='your-local-database-password'
ollama pull llama3
ollama pull nomic-embed-text
./gradlew bootRun
```

To ingest only the curated operational runbooks, opt into startup ingestion:

```bash
./gradlew bootRun --args='--app.knowledge-base.ingest-docs-on-startup=true'
```

`application-dev.properties` points `app.knowledge-base.docs-path` at `docs/runbooks`. Ingestion is
off unless explicitly enabled. It is currently add-only: rerunning ingestion can create duplicate
chunks, and replacing updated sources requires deleting/replacing their old vectors first.

## How the components fit together

```text
docs/runbooks/*.md
  -> KnowledgeBaseMarkdownLoader (Document + relative source metadata)
  -> KnowledgeBaseIngestionService (TokenTextSplitter chunks)
  -> EmbeddingModel (nomic-embed-text)
  -> VectorStore (pgvector)

question
  -> embedding + VectorStore similarity search
  -> retrieved runbook context added by the RAG advisor
  -> ChatClient / ChatModel
  -> structured reply and suggestions
```

- `ChatModel` generates a response; `EmbeddingModel` represents text as vectors. They are separate
  provider choices, even if both happen to use the same vendor.
- The `VectorStore` interface keeps ingestion and retrieval independent of pgvector-specific APIs.
- The same embedding model and vector dimensions must be used when indexing and querying. Switching
  embedding models requires re-embedding the knowledge base; equal dimensions do not make vectors
  semantically compatible.
- The normal `POST /api/v1/chat` route does not retrieve runbooks. Use
  `POST /api/v1/chat/rag`; adding a `conversationId` enables memory alongside retrieval.
- Runbook content is context, not a guarantee of grounded output. Review generated operational
  advice, and prefer the documented read-only checks before considering remediation.

## What we tested and learned

- A direct Ollama `/api/embed` request with actual input text returned a 768-dimensional embedding.
  The embedding model requires input text; a model-only invocation is not a valid test.
- The opt-in pgvector retrieval integration test ingested project Markdown and retrieved a matching
  CrashLoopBackOff runbook chunk. This checks embedding, storage, and similarity search without
  relying on chat generation.
- A live `POST /api/v1/chat/rag` request returned a CrashLoopBackOff explanation and safe checks
  (`kubectl logs` and `kubectl describe pod`), plus relevant suggestions. That request used the
  hosted Gemini path during testing; its successful response demonstrated end-to-end RAG response
  generation, while the separate retrieval test is the evidence specifically for pgvector search.
- The live app ran on port `18080`; the dev PostgreSQL connection used port `5431`. App health alone
  only showed that Spring Boot was running, not that retrieval or model generation worked.
- Earlier local chat calls timed out with `ReadTimeoutException` from Ollama's `/api/chat`, while
  the embedding call worked. That isolates the observed timeout to chat generation, but does not
  determine whether the cause was model/runtime load or another Ollama-side delay.
- Gemini requests also exposed external-provider failure modes: an invalid API key caused HTTP 400,
  and a separate request received HTTP 503 due to provider availability/high demand. These are
  distinct from pgvector retrieval failures; a valid key alone cannot guarantee provider uptime.
- The Java native-access warning from Netty was unrelated to the embedding and RAG responses.

## Gemini reference configuration

Gemini was useful to validate the hosted-model route, but it is intentionally inactive in the
default configuration. Credential values must stay outside the repository; never paste an API key
into a properties file or this document. The chat example in `application.properties` uses the
existing Spring AI OpenAI-compatible client:

```properties
#app.ai.provider=openai
#app.ai.openai.base-url=https://generativelanguage.googleapis.com/v1beta/openai/
#app.ai.openai.api-key=${GEMINI_API_KEY}
#app.ai.openai.model=gemini-3.8-flash
```

The corresponding embedding example in `application-dev.properties` uses
`gemini-embedding-001` with 768 dimensions. To activate it, add the Spring AI Google GenAI embedding
starter, set `GEMINI_API_KEY` in the environment, and select the Google GenAI embedding properties
instead of the Ollama embedding properties. Keep only one embedding provider selected to avoid
ambiguous `EmbeddingModel` beans. Re-ingest vectors when changing models.

## Retrieval verification

With PostgreSQL and Ollama available, the opt-in integration test exercises the real embedding and
pgvector retrieval path:

```bash
RAG_INTEGRATION_TESTS=true ./gradlew test --tests '*KnowledgeBaseRetrievalIntegrationTest'
```

It writes runbook chunks to the configured dev database and does not clean them up. The test should
only be run against a database where these writes are acceptable. To try the HTTP RAG route after
ingestion:

```bash
curl --fail-with-body --silent --show-error --max-time 120 \
  -X POST http://127.0.0.1:18080/api/v1/chat/rag \
  -H 'Content-Type: application/json' \
  -d '{"message":"According to the runbook, what does Kubernetes CrashLoopBackOff mean and name two safe checks?"}'
```

## Known gaps

- Ingestion has no deduplication, source replacement, deletion, or automatic re-indexing lifecycle.
- There is no source-specific access control or metadata filter yet; the current knowledge base is
  the local curated runbook set.
- In-process conversation memory is not durable across restarts.
- Ollama chat latency should be checked again after restoring the local provider. The earlier
  timeout was intermittent and its underlying runtime cause was not established.