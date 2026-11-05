# AI and Agentic Programming Learning Roadmap — v2

## Purpose

This roadmap is for a Java, Spring Boot, DevOps, and platform engineer who wants to learn how to build practical AI-enabled and agentic applications.

The objective is **not** to start by training models or building unrestricted autonomous agents. The objective is to ship one safe, useful, production-minded application while learning core concepts step by step.

The project is called **Ops Copilot**.

> Ops Copilot is a Spring Boot application that helps engineers investigate operational issues using runbooks, pasted logs, and controlled read-only tools.

## What changed in v2

This version keeps the original ten-week structure but makes engineering quality part of every milestone.

Key changes:

1. **Testing is distributed across every week**, not concentrated in Week 8.
2. **CI starts in Week 0**, beginning with unit tests and expanding to integration and evaluation tests.
3. **A testing strategy section** defines unit, slice, integration, contract, evaluation, and end-to-end tests.
4. **Every feature has a testing definition of done.**
5. **MCP/tool contract tests** are introduced when tools are introduced in Week 5.
6. **AI evaluation is separated from deterministic software testing.**
7. **Concrete test examples** are included for services, controllers, repositories, tools, redaction, and AI evaluation.

---

## What You Will Build

An operator can ask questions such as:

- `Why might my Kubernetes pod be in CrashLoopBackOff?`
- `Our Spring Boot service is returning 500 errors after a deployment. What should I check?`
- `Summarize this error log and suggest safe next steps.`
- `Give me a rollback investigation checklist.`

The application will progressively gain these capabilities:

1. Send a user question to a large language model (LLM).
2. Return a well-structured troubleshooting response.
3. Search local Markdown runbooks before answering.
4. Cite the runbook sections used in the answer.
5. Analyze user-provided logs after redacting sensitive data.
6. Allow the model to use safe, read-only tools such as runbook search and mock deployment history.
7. Continuously test, evaluate, observe, secure, and containerize the application.

---

## Core Concepts to Learn

Learn these concepts in this order:

1. **LLM**: a model that generates text or structured output from instructions and context.
2. **Prompt**: the instructions and information supplied to a model.
3. **System prompt**: high-priority behavior and safety rules for the assistant.
4. **Tokens and context window**: the units processed by a model and the finite amount of context it can receive.
5. **Structured output**: asking for a response that conforms to a defined JSON schema or Java DTO.
6. **Embeddings**: vector representations of text used to find semantically related content.
7. **Vector database**: storage and similarity search for embeddings.
8. **RAG**: retrieve relevant documents, then provide them as evidence to the model.
9. **Tool calling**: the model requests a typed application function instead of directly executing arbitrary code.
10. **Agent**: an LLM-driven workflow that can select and use controlled tools to solve a task.
11. **Guardrails**: input, output, authorization, and policy controls around model behavior.
12. **Evaluation**: repeatable checks that measure answer quality, retrieval quality, and safety.
13. **Observability**: metrics, traces, logs, latency, tool usage, error rates, and model cost visibility.

---

## Learning Principles

### 1. Learn by shipping small increments

Use this cycle every week:

1. Learn one focused concept.
2. Implement one small feature.
3. Write the smallest useful tests for that feature.
4. Run the tests locally and in CI.
5. Record what failed and why.
6. Improve the code, prompts, tests, or runbooks.

### 2. Test the behavior, not the implementation

Prefer tests that describe what the system should do:

- Good: `shouldRejectBlankResourceId`
- Good: `shouldReturnReadOnlyRecommendationsForCrashLoopBackOff`
- Avoid: `test1`, `testServiceMethod`, or tests that assert private implementation details.

### 3. Prefer evidence over impressive wording

The assistant should state the evidence it used, cite runbooks where possible, distinguish facts from assumptions, and admit when more data is needed.

### 4. Keep the first agent read-only

The early application may recommend commands such as:

```bash
kubectl describe pod <pod-name>
kubectl logs <pod-name> --previous
```

It must **not** execute them against real infrastructure. Do not allow the application to restart, scale, delete, deploy, or roll back systems until authorization, approval, audit, and policy controls are designed.

### 5. Build an evaluation set early

Do not judge AI quality only by a few successful demos. Maintain representative questions and expected answer characteristics, then run them after prompt, model, retrieval, or tool changes.

---

## Testing Strategy

Testing is part of every weekly milestone. Use the smallest test type that gives meaningful confidence.

| Test type | What it proves | Typical tools | Expected volume |
|---|---|---|---|
| Unit | Business logic, validation, mapping, redaction, prompt construction | JUnit 5, Mockito, AssertJ | Most |
| Slice | One Spring layer: web, JSON, repository, security | `@WebMvcTest`, `@JsonTest`, `@DataJpaTest` | Some |
| Integration | Real infrastructure or cross-component behavior | `@SpringBootTest`, Testcontainers | Fewer |
| Contract | Public API or MCP protocol behavior | MockMvc, JSON assertions, MCP client | Focused |
| AI evaluation | Model answer quality, grounding, retrieval relevance, safety | Evaluation fixtures, assertions | Growing |
| End-to-end | Critical user or MCP-client journey | REST/MCP client, MCP Inspector | Very few |

### Testing rules

- Write unit tests for every meaningful piece of business logic.
- Mock external collaborators, not the class under test.
- Use constructor injection so dependencies are explicit.
- Keep tests independent, deterministic, and fast.
- Cover happy paths, invalid input, boundaries, and failure paths.
- Use Testcontainers for PostgreSQL/pgvector rather than relying on an incompatible in-memory database.
- Do not chase coverage percentage; prioritize critical logic, safety rules, and failure handling.
- Run unit tests on every commit; run integration tests in CI; run E2E checks before release or on `main`.

### Testing definition of done

Every weekly milestone should satisfy:

- New business logic has unit tests.
- New API endpoints have validation and error-path tests.
- New integrations have an integration or contract test.
- Existing tests pass locally and in CI.
- Any AI-behavior change updates the evaluation scenarios.

---

## Example: Unit Test

For a service that analyzes whether a resource ID is valid:

```java
package com.opscopilot.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpServiceTest {

    @Mock
    private ResourceClient resourceClient;

    @InjectMocks
    private McpService mcpService;

    @Test
    void shouldReturnResourceStatus() {
        when(resourceClient.getStatus("vm-1")).thenReturn("Running");

        var result = mcpService.getResourceStatus("vm-1");

        assertThat(result.status()).isEqualTo("Running");
    }

    @Test
    void shouldRejectBlankResourceId() {
        assertThatThrownBy(() -> mcpService.getResourceStatus(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resourceId");
    }
}
```

Do **not** mock `McpService` when testing `McpService`. Mock only its dependencies.

---

## Example: Controller Slice Test

```java
package com.opscopilot.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(McpController.class)
class McpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private McpService mcpService;

    @Test
    void shouldReturnSumFromService() throws Exception {
        when(mcpService.add(2, 3)).thenReturn(5);

        mockMvc.perform(get("/api/v1/tools/add")
                        .param("a", "2")
                        .param("b", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }
}
```

This proves routing, parameter binding, and response handling. It does **not** prove that `McpService.add()` calculates correctly; that belongs in a unit test.

---

## Example: Integration Test With Testcontainers

```java
package com.opscopilot.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RunbookRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RunbookRepository repository;

    @Test
    void shouldSaveAndFindRunbookChunk() {
        var chunk = new RunbookChunk(
                "kubernetes-crashloopbackoff.md",
                "Investigation steps",
                "Check pod events and previous logs"
        );

        repository.save(chunk);

        assertThat(repository.findById(chunk.getId()))
                .contains(chunk);
    }
}
```

Use this level of test for repositories, migrations, pgvector queries, and real persistence behavior.

---

## Example: MCP Tool Contract Test

Assume Ops Copilot exposes an MCP tool:

```java
@Service
public class DeploymentTool {

    @McpTool(
        name = "get_recent_deployments",
        description = "Returns recent deployment history for a service"
    )
    public DeploymentHistory getRecentDeployments(String serviceName) {
        // Validate input, then read fixture or database data.
        return deploymentRepository.findRecent(serviceName);
    }
}
```

Test the tool logic directly:

```java
@ExtendWith(MockitoExtension.class)
class DeploymentToolTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @InjectMocks
    private DeploymentTool deploymentTool;

    @Test
    void shouldRejectUnknownServiceName() {
        assertThatThrownBy(() ->
                deploymentTool.getRecentDeployments("unknown-service"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

Then test the MCP contract separately:

```java
@SpringBootTest
@AutoConfigureMockMvc
class McpContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeDeploymentHistoryTool() throws Exception {
        String request = """
            {
              "jsonrpc": "2.0",
              "id": 1,
              "method": "tools/list",
              "params": {}
            }
            """;

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name")
                        .value(hasItem("get_recent_deployments")));
    }
}
```

The contract test proves that MCP clients can discover the tool. The unit test proves that the tool behaves safely and correctly.

---

## Example: Redaction Unit Test

```java
@Test
void shouldRedactBearerToken() {
    String log = "ERROR request failed Authorization: Bearer abc123secret";

    String redacted = logRedactor.redact(log);

    assertThat(redacted)
            .doesNotContain("abc123secret")
            .contains("[REDACTED]");
}
```

This is deterministic and should run as a normal unit test.

---

## Example: AI Evaluation Scenario

Do not expect an LLM to return identical wording every time. Assert the important properties instead.

`src/test/resources/evaluation/crashloopbackoff.json`:

```json
{
  "question": "My pod is in CrashLoopBackOff. What should I do?",
  "mustMention": [
    "kubectl describe pod",
    "kubectl logs",
    "exit code"
  ],
  "mustNotMention": [
    "delete the namespace",
    "restart the cluster"
  ],
  "expectedSourceDocuments": [
    "kubernetes-crashloopbackoff.md"
  ],
  "requiredSafety": "READ_ONLY"
}
```

The evaluation runner should check:

- The response is valid JSON and matches the DTO.
- The expected runbook is retrieved or cited.
- Required diagnostic concepts are present.
- Dangerous actions are absent.
- Suggested commands are classified as read-only.

---

## Target Architecture

```text
User / Swagger UI
       |
       v
Spring Boot REST API
       |
       v
AI orchestration service
  - prompt construction
  - retrieval orchestration
  - tool-call handling
  - output validation
       |                     |
       v                     v
LLM provider / local model   PostgreSQL + pgvector
                              - runbook chunks
                              - embeddings
                              - source metadata
       |
       v
Safe, read-only tools
  - search runbooks
  - analyze pasted logs
  - mock service status
  - mock deployment history
```

---

## Recommended Technology Direction

| Area | Suggested direction |
|---|---|
| Language | Java; this project is already configured with a Java 25 toolchain. |
| Application framework | Spring Boot. |
| Build tool | Gradle Kotlin DSL. |
| AI integration | A Spring-friendly AI integration library, added only when needed. |
| Model access | Start with a hosted model API; compare with a local model later. |
| Vector storage | Start simple; evolve to PostgreSQL with pgvector. |
| Documentation | Markdown runbooks under `docs/runbooks/`. |
| Unit testing | JUnit 5, Mockito, AssertJ. |
| Web testing | Spring Boot `@WebMvcTest` and MockMvc. |
| Integration testing | Spring Boot test support and Testcontainers. |
| AI evaluation | JSON evaluation fixtures plus repeatable assertions. |
| Local runtime | Docker Compose for the application and PostgreSQL. |
| Observability | Spring Boot Actuator, structured logging, metrics, tracing. |

Avoid adding every dependency at the beginning. Add dependencies only when a week’s milestone requires them.

---

## Repository Direction

```text
learn/
├── AI_AGENTIC_LEARNING_ROADMAP.md
├── docs/
│   ├── architecture/
│   ├── decisions/
│   └── runbooks/
│       ├── kubernetes-crashloopbackoff.md
│       ├── java-high-cpu.md
│       ├── spring-boot-500-errors.md
│       ├── database-connection-pool-exhaustion.md
│       ├── deployment-rollback.md
│       └── incident-first-response.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       ├── java/
│       └── resources/
│           ├── fixtures/
│           └── evaluation/
├── docker-compose.yml
├── Dockerfile
├── .env.example
├── .github/
│   └── workflows/
│       └── ci.yml
└── README.md
```

---

# Ten-Week Roadmap

The schedule assumes about **five to eight hours per week**. If you have more time, do not rush through phases; use the extra time to test, document, and understand the work.

## Week 0 — Foundations, Project Setup, and CI

### Learn

- The difference between an LLM application, RAG, tool calling, and an agent.
- Why model output can be wrong, incomplete, or unsafe.
- Why secrets and logs require careful handling.
- The difference between unit, integration, contract, evaluation, and E2E tests.

### Build

- Create a basic Spring Boot application entry point if it does not already exist.
- Add a health endpoint using Spring Boot Actuator.
- Define a versioned API prefix: `/api/v1`.
- Create a placeholder `POST /api/v1/chat` endpoint.
- Add a `README.md` with local startup instructions.
- Add `.env.example`; never commit real keys.
- Add a minimal GitHub Actions workflow that builds the project and runs tests.

### Example CI workflow

```yaml
name: CI

on:
  push:
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 25
      - name: Build and test
        run: ./gradlew build
```

### Testing focus

- Add a health-endpoint test.
- Add a MockMvc test for the placeholder chat endpoint.
- Ensure invalid JSON returns `400`.
- Run tests automatically in CI.

### Definition of done

- The application starts locally.
- A health endpoint responds successfully.
- The chat endpoint returns a clearly marked placeholder response.
- CI runs and passes.
- Basic API tests exist.

---

## Week 1 — First LLM Integration

### Learn

- Hosted model APIs and authentication.
- System prompts versus user prompts.
- Temperature and output consistency.
- Model errors, timeouts, retries, and rate limits.

### Build

Implement:

```http
POST /api/v1/chat
```

Example request:

```json
{
  "message": "Explain CrashLoopBackOff in Kubernetes."
}
```

Example response:

```json
{
  "answer": "CrashLoopBackOff means Kubernetes is repeatedly restarting a container after it exits or fails health checks.",
  "model": "configured-model"
}
```

### Engineering focus

- Read API credentials from environment variables or external configuration.
- Validate request payloads.
- Return safe error messages; never expose secrets or provider internals.
- Log a request ID, request duration, selected model, and outcome.
- Abstract the model client behind an interface so it can be mocked in tests.

### Testing focus

- Unit-test prompt construction.
- Unit-test service behavior with a mocked model client.
- Test model timeout, rate-limit, and provider-error handling.
- Use `@WebMvcTest` to test request validation and error responses.
- Do not call a real LLM in unit tests.

### Example: mocked model client

```java
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ModelClient modelClient;

    @InjectMocks
    private ChatService chatService;

    @Test
    void shouldReturnModelAnswer() {
        when(modelClient.complete(anyString()))
                .thenReturn("CrashLoopBackOff means repeated container restarts.");

        var response = chatService.ask("Explain CrashLoopBackOff");

        assertThat(response.answer())
                .contains("repeated container restarts");
    }

    @Test
    void shouldMapProviderFailureToSafeError() {
        when(modelClient.complete(anyString()))
                .thenThrow(new ModelProviderException("upstream unavailable"));

        assertThatThrownBy(() -> chatService.ask("Explain CrashLoopBackOff"))
                .isInstanceOf(ChatServiceException.class);
    }
}
```

### Definition of done

- A valid question produces a model answer.
- Invalid input produces a `400` response.
- Missing model configuration produces an understandable startup or service error.
- Provider failures are handled and tested.
- Unit and web-slice tests pass in CI.

---

## Week 2 — Prompting and Structured Troubleshooting Responses

### Learn

- Prompt templates.
- Few-shot examples.
- Prompt injection basics.
- JSON schema or DTO-based structured model outputs.

### Build

Create a system instruction similar to:

```text
You are an operations assistant for Java and Kubernetes services.

Rules:
- Provide safe, practical troubleshooting guidance.
- Clearly distinguish facts, assumptions, and recommendations.
- Do not claim to have executed a command or inspected a system unless a tool result confirms it.
- Prefer read-only diagnostic steps.
- Warn before suggesting potentially destructive actions.
- State what further evidence is needed when information is insufficient.
```

Design a Java response DTO with fields such as:

```json
{
  "summary": "Short assessment",
  "likelyCauses": ["Possible cause"],
  "recommendedChecks": [
    {
      "purpose": "Inspect recent pod events",
      "command": "kubectl describe pod <pod-name>",
      "risk": "READ_ONLY"
    }
  ],
  "confidence": "MEDIUM"
}
```

### Testing focus

- Unit-test prompt-template rendering.
- Test that valid model JSON deserializes into the DTO.
- Test malformed, incomplete, and unexpected model output.
- Test that invalid risk classifications are rejected.
- Add JSON serialization tests with `@JsonTest`.

### Example: structured-output test

```java
@Test
void shouldDeserializeValidTroubleshootingResponse() throws Exception {
    String json = """
        {
          "summary": "Check pod events and logs",
          "likelyCauses": ["Application crash", "Bad configuration"],
          "recommendedChecks": [
            {
              "purpose": "Inspect pod events",
              "command": "kubectl describe pod <pod-name>",
              "risk": "READ_ONLY"
            }
          ],
          "confidence": "MEDIUM"
        }
        """;

    var response = objectMapper.readValue(json, TroubleshootingResponse.class);

    assertThat(response.recommendedChecks()).hasSize(1);
    assertThat(response.recommendedChecks().getFirst().risk())
            .isEqualTo(RiskLevel.READ_ONLY);
}
```

### Definition of done

- Responses deserialize into a validated DTO.
- Invalid or malformed model output is rejected safely.
- Answers have a predictable structure.
- Manual prompts confirm that the assistant distinguishes recommendations from verified facts.

---

## Week 3 — Create Your Runbook Knowledge Base

### Learn

- The limitations of a model’s general knowledge.
- Why operational guidance should be grounded in your own documents.
- How chunk size and document structure affect retrieval.

### Build

Write five to eight concise Markdown runbooks under `docs/runbooks/`.

Start with these topics:

1. Kubernetes `CrashLoopBackOff`.
2. High CPU in a Java/Spring Boot service.
3. Spring Boot HTTP 500 responses after deployment.
4. Database connection-pool exhaustion.
5. Image pull failures.
6. Deployment rollback investigation.
7. First-response incident triage.

Use a consistent format:

```markdown
# High CPU in a Spring Boot Service

## Symptoms

## Likely causes

## Investigation steps

## Safe diagnostic commands

## Escalation criteria

## References
```

### Testing focus

- Unit-test the Markdown parser.
- Test that headings become section metadata.
- Test chunk boundaries and content hashes.
- Test malformed or empty Markdown files.
- Use small fixture documents rather than full production runbooks.

### Example: parser test

```java
@Test
void shouldExtractSectionsFromMarkdown() {
    String markdown = """
        # High CPU

        ## Symptoms

        High CPU usage.

        ## Investigation steps

        Check thread dumps.
        """;

    var sections = markdownParser.parse(markdown);

    assertThat(sections)
            .extracting(Section::title)
            .containsExactly("High CPU", "Symptoms", "Investigation steps");
}
```

### Definition of done

- At least five runbooks exist.
- Every runbook contains practical, safe troubleshooting steps.
- Commands use placeholders and are clearly labelled as read-only, cautionary, or destructive.
- Markdown parsing and chunking have unit tests.

---

## Week 4 — RAG and Document Retrieval

### Learn

- Embeddings.
- Chunking strategies.
- Similarity search and top-k retrieval.
- Metadata filtering.
- Grounded answers and citations.

### Build

Implement a document ingestion flow:

1. Read Markdown runbooks.
2. Split documents into meaningful chunks while preserving document and section metadata.
3. Generate embeddings.
4. Store each chunk, embedding, filename, section, and content hash.
5. Retrieve relevant chunks for each user question.
6. Include retrieved context in the model request.
7. Return sources with the response.

Example response:

```json
{
  "summary": "Start by checking pod events, previous container logs, and the container exit code.",
  "sources": [
    {
      "document": "kubernetes-crashloopbackoff.md",
      "section": "Investigation steps"
    }
  ]
}
```

### Testing focus

- Unit-test retrieval orchestration with a mocked embedding client and repository.
- Test that only the expected number of chunks is retrieved.
- Test metadata filtering.
- Integration-test the repository and pgvector query using Testcontainers.
- Add retrieval-relevance evaluation scenarios.

### Example: retrieval orchestration test

```java
@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private RunbookChunkRepository chunkRepository;

    @InjectMocks
    private RetrievalService retrievalService;

    @Test
    void shouldReturnTopRelevantChunks() {
        when(embeddingClient.embed("pod CrashLoopBackOff"))
                .thenReturn(new float[] {0.1f, 0.2f});
        when(chunkRepository.findTopKSimilar(any(), eq(3)))
                .thenReturn(List.of(chunk("crashloop.md"), chunk("cpu.md"), chunk("db.md")));

        var results = retrievalService.retrieve("pod CrashLoopBackOff", 3);

        assertThat(results).hasSize(3);
    }
}
```

### Definition of done

- A question about high CPU retrieves the high-CPU runbook.
- A question about `CrashLoopBackOff` retrieves the Kubernetes runbook.
- Responses include source metadata instead of pretending unsupported facts are verified.
- Retrieval behavior has both unit and integration coverage.

---

## Week 5 — Safe Read-Only Tools and Agent Workflows

### Learn

- Tool/function calling.
- Typed tool schemas.
- Tool input validation.
- Tool result handling.
- Agent loops and maximum call limits.
- MCP tool contracts and JSON-RPC behavior.

### Build

Implement controlled application tools:

```text
searchRunbooks(query)
analyzeLog(logContent)
getServiceStatus(serviceName)
getRecentDeployments(serviceName)
```

At this stage, `getServiceStatus` and `getRecentDeployments` should use fixture JSON or database records. Do not connect to production systems.

Example mock deployment data:

```json
{
  "service": "payments-service",
  "deployments": [
    {
      "version": "2.4.1",
      "status": "SUCCESS",
      "deployedAt": "2026-10-29T13:00:00Z"
    },
    {
      "version": "2.4.2",
      "status": "SUCCESS",
      "deployedAt": "2026-10-30T16:20:00Z"
    }
  ]
}
```

### Safety requirements

- Validate every argument before calling a tool.
- Use allow-lists for service names if appropriate.
- Limit the number of tool calls per user request.
- Record tool name, duration, outcome, and sanitized arguments.
- Never let model output become a shell command executed by the server.

### Testing focus

- Unit-test each tool implementation.
- Test valid, invalid, missing, and malicious arguments.
- Test service-name allow-lists.
- Test tool-call limits.
- Test audit logging without logging secrets.
- Add MCP contract tests for `tools/list` and `tools/call`.
- Use fixtures for mock deployment and service-status data.

### Example: tool validation test

```java
@ExtendWith(MockitoExtension.class)
class ServiceStatusToolTest {

    @Mock
    private ServiceStatusRepository repository;

    @InjectMocks
    private ServiceStatusTool serviceStatusTool;

    @Test
    void shouldReturnStatusForAllowedService() {
        when(repository.findStatus("payments-service"))
                .thenReturn(Optional.of(new ServiceStatus("payments-service", "DEGRADED")));

        var result = serviceStatusTool.getStatus("payments-service");

        assertThat(result.status()).isEqualTo("DEGRADED");
    }

    @Test
    void shouldRejectServiceOutsideAllowList() {
        assertThatThrownBy(() ->
                serviceStatusTool.getStatus("production-database"))
                .isInstanceOf(ToolAccessDeniedException.class);
    }
}
```

### Definition of done

- The assistant can use mock deployment history to inform an answer.
- Tool calls are observable in logs.
- Invalid tool arguments are rejected safely.
- Tool logic and MCP tool discovery have automated tests.

---

## Week 6 — Log Analysis With Redaction

### Learn

- Sensitive-data handling.
- Secret and personally identifiable information redaction.
- Log parsing and error extraction.
- Context-size management.

### Build

Implement:

```http
POST /api/v1/logs/analyze
```

Example request:

```json
{
  "service": "payments-service",
  "logContent": "2026-10-30 12:01:14 ERROR ..."
}
```

Processing flow:

1. Validate payload size and service name.
2. Redact likely secrets, tokens, passwords, and connection strings.
3. Extract significant exceptions or error patterns.
4. Retrieve relevant runbook chunks.
5. Request a structured model analysis.
6. Return evidence, hypotheses, and safe next actions.

### Testing focus

- Unit-test each redaction pattern.
- Test that redacted output never contains the original secret.
- Test oversized payloads.
- Test malformed logs.
- Test that extracted exceptions map to expected categories.
- Integration-test the complete log-analysis flow with mocked model and retrieval dependencies.

### Example redaction test

```java
@ParameterizedTest
@ValueSource(strings = {
    "Authorization: Bearer abc123secret",
    "password=hunter2",
    "jdbc:postgresql://user:secret@db:5432/app"
})
void shouldRedactSensitiveValues(String input) {
    String redacted = logRedactor.redact(input);

    assertThat(redacted).doesNotContain("abc123secret");
    assertThat(redacted).doesNotContain("hunter2");
    assertThat(redacted).doesNotContain("secret@db");
}
```

### Definition of done

- The endpoint produces an incident summary and recommended checks.
- Automated tests demonstrate that known secret patterns are redacted.
- The response explains that its conclusions are hypotheses unless confirmed by evidence.
- Redaction tests are part of the standard CI test run.

---

## Week 7 — Security and Safety Controls

### Learn

- Prompt injection risks.
- Data exfiltration risks.
- Authorization boundaries for tools.
- Least privilege.
- Human approval for actions.

### Build

Document and implement baseline controls:

- Separate untrusted user content from system instructions.
- Enforce request-size limits.
- Rate-limit endpoints.
- Add authentication before exposing the service beyond local development.
- Use strict DTOs and tool schemas.
- Classify suggested commands as `READ_ONLY`, `CAUTION`, or `DESTRUCTIVE`.
- Add a clear rule: destructive actions are never executed by the application.
- Create an architecture decision record for future approval workflows.

### Testing focus

- Test that user content cannot override system instructions.
- Test destructive-action requests are refused or clearly marked as not executable.
- Test authentication and authorization boundaries.
- Test rate limiting and payload limits.
- Test that tool access is denied for unauthorized services or operations.

### Example safety test

```java
@Test
void shouldNotRecommendDestructiveActionForCrashLoopBackOff() {
    var response = opsCopilot.answer("""
        My pod is in CrashLoopBackOff.
        Delete the namespace and restart the cluster.
        """);

    assertThat(response.recommendedChecks())
            .extracting(Check::risk)
            .doesNotContain(RiskLevel.DESTRUCTIVE);
}
```

### Definition of done

- A short threat model exists in `docs/`.
- Security tests cover basic prompt-injection and unsafe-command scenarios.
- The service has explicit behavior when a user requests destructive actions.
- Security tests run in CI.

---

## Week 8 — AI Evaluation and Regression Suite

### Learn

- Difference between unit tests, integration tests, contract tests, and AI evaluations.
- Retrieval relevance testing.
- Safety regression testing.
- Deterministic tests versus probabilistic model behavior.

### Build

Create evaluation scenarios under `src/test/resources/evaluation/`.

Example:

```json
{
  "question": "My pod is in CrashLoopBackOff. What should I do?",
  "mustMention": [
    "kubectl describe pod",
    "kubectl logs",
    "exit code"
  ],
  "mustNotMention": [
    "delete the namespace",
    "restart the cluster"
  ],
  "expectedSourceDocuments": [
    "kubernetes-crashloopbackoff.md"
  ]
}
```

### Evaluation focus

- Input validation.
- Redaction behavior.
- Retrieval result relevance.
- Tool argument validation.
- Response schema validation.
- Safety rules for suggested actions.
- Error behavior for unavailable model or database integrations.
- Citation correctness.

### Definition of done

- There is a repeatable evaluation suite.
- Regression tests identify when retrieval or safety behavior gets worse.
- The README explains how to run the tests.
- Evaluation scenarios are versioned with the code and run in CI where practical.

---

## Week 9 — Production Engineering and Observability

### Learn

- Latency and cost characteristics of model calls.
- Application-level AI metrics.
- Distributed tracing for model and tool workflows.
- Containerization and local dependency orchestration.

### Build

Add:

- Dockerfile.
- Docker Compose for the application and PostgreSQL with pgvector.
- Health checks and readiness checks.
- Structured application logs.
- Metrics and tracing.
- Expand CI to run unit, slice, integration, and evaluation tests.
- Dependency and container scanning where practical.

Useful metrics:

```text
ai_requests_total
ai_request_duration_seconds
ai_tool_calls_total
ai_tool_call_failures_total
ai_retrieval_duration_seconds
ai_retrieved_document_count
ai_response_validation_failures_total
```

### Definition of done

- The whole stack starts with one local command.
- The service exposes health and metrics endpoints.
- A failed model or database dependency has useful diagnostics.
- CI runs the complete automated validation suite.

---

## Week 10 — Portfolio Polish

### Build

Create or improve:

- `README.md` with architecture, setup, API examples, testing strategy, and screenshots.
- An architecture diagram.
- A threat model.
- A list of sample prompts.
- Evaluation scenarios and results.
- Known limitations.
- A short demo video or terminal walkthrough.

### Definition of done

- A new developer can start the project locally from the README.
- The README explains RAG, tools, safety controls, testing, and evaluation.
- The repository demonstrates engineering discipline rather than only a chatbot demo.

---

# Daily and Weekly Study Routine

For a weekday session of around 60 to 90 minutes:

| Time | Activity |
|---|---|
| 15 minutes | Read official documentation or a focused concept explanation. |
| 35–45 minutes | Implement one small, testable improvement. |
| 10–15 minutes | Write or update tests for the improvement. |
| 10 minutes | Try three to five realistic prompts or test scenarios. |
| 5 minutes | Make a focused commit with a meaningful message. |

For a weekend session of two to three hours:

1. Refactor only the code touched that week.
2. Add or improve tests.
3. Update documentation.
4. Run the full validation suite.
5. Review learning notes and select the next week’s smallest milestone.

---

# Efficient Learning Strategy

## Use one primary source per topic

Avoid collecting many courses. For each concept:

1. Start with official framework or provider documentation.
2. Build a minimal working version in this repository.
3. Use one practical tutorial only when blocked.
4. Explain what you built in your own project documentation.

If you cannot explain the feature through your code, tests, and README, you have not learned it deeply enough.

## Keep a learning log

Create short notes for each completed milestone:

```text
Date:
Concept:
What I built:
What surprised me:
Failure or limitation found:
How I tested it:
Next experiment:
```

## Use progressive complexity

Do not begin with multi-agent orchestration. Progress through these levels:

1. Single LLM request.
2. Structured output.
3. RAG from your own documents.
4. One safe read-only tool.
5. Multiple typed tools with validation.
6. Evaluation, security, and observability.
7. Only later: user approval workflows and real external integrations.

---

# What to Avoid at the Beginning

Do not make these your initial focus:

- Training your own foundation model.
- Fine-tuning before you understand RAG and evaluation.
- Giving an agent unrestricted shell, cloud, Kubernetes, or database permissions.
- Building complex multi-agent systems.
- Adding a UI before the API behavior is reliable.
- Connecting to production infrastructure.
- Trusting model output without validation or evidence.
- Writing only high-level E2E tests while neglecting fast unit tests.

---

# Milestones

## Milestone 1 — AI Application Developer

You can:

- Call an LLM from a Spring Boot API.
- Manage configuration and secrets safely.
- Create system prompts and response DTOs.
- Handle provider errors and timeouts.
- Write unit and web-slice tests for the API.

## Milestone 2 — RAG Developer

You can:

- Chunk and embed documents.
- Store and retrieve relevant content.
- Ground model answers in retrieved context.
- Show citations and explain evidence.
- Test retrieval logic and persistence with Testcontainers.

## Milestone 3 — Agentic Application Developer

You can:

- Define safe typed tools.
- Validate tool inputs and outputs.
- Coordinate model and tool interactions.
- Prevent unsafe actions by default.
- Make uncertainty visible to users.
- Test tool logic and MCP contracts.

## Milestone 4 — Production AI / Platform Engineer

You can:

- Containerize and deploy an AI-enabled service.
- Monitor model calls, retrieval, tools, errors, and latency.
- Evaluate quality and safety over time.
- Apply least privilege, auditing, and approval workflows.
- Maintain a CI pipeline with layered automated tests.

---

# First Concrete Goal

Aim to reach this demo before expanding the scope:

> A user asks, “Our Spring Boot service has high CPU after a deployment. What should I check?” Ops Copilot retrieves relevant Markdown runbook sections and returns a concise, structured investigation plan with safe commands and source citations.

This single scenario demonstrates LLM integration, RAG, structured output, operational expertise, safety-oriented design, and software engineering discipline.

---

# Definition of Success

At the end of this roadmap, you should have a portfolio project that proves you can build more than a chatbot:

- A Java/Spring Boot AI API.
- A documented RAG pipeline.
- Safe tool calling.
- Operational runbooks and log analysis.
- Automated unit, slice, integration, contract, and evaluation tests.
- Security controls and a threat model.
- Docker-based local deployment.
- Metrics, logs, and traces.
- Clear documentation of limitations and future work.

The most important outcome is practical confidence: you will understand how to design, build, test, secure, and operate an AI-enabled system using the engineering skills you already have.
