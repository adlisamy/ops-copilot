# AI and Agentic Programming Learning Roadmap

## Purpose

This roadmap is for a Java, Spring Boot, DevOps, and platform engineer who wants to learn how to build practical AI-enabled and agentic applications.

The objective is **not** to start by training models or building unrestricted autonomous agents. The objective is to ship one safe, useful, production-minded application while learning the core concepts step by step.

The project in this repository will be called **Ops Copilot**.

> Ops Copilot is a Spring Boot application that helps engineers investigate operational issues by using runbooks, pasted logs, and controlled read-only tools.

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
4. Cite the runbook sections used for the answer.
5. Analyze user-provided logs after redacting sensitive data.
6. Allow the model to use safe, read-only tools such as runbook search and mock deployment history.
7. Add tests, evaluation scenarios, observability, containers, and security controls.

---

## Why This Is a Good Project for Your Background

Your existing skills are an advantage:

| Existing experience | How it applies to AI systems |
| --- | --- |
| Java and Spring Boot | Build reliable APIs, validation, configuration, tests, and integrations. |
| DevOps and platform engineering | Deploy, monitor, secure, scale, and operate AI-enabled services. |
| Kubernetes and cloud operations | Create credible runbooks and useful real-world troubleshooting workflows. |
| CI/CD and automation | Build safe human-approved workflows rather than unsafe autonomous automation. |

Many developers can call an LLM API. Fewer can build an AI service that is secure, observable, evaluated, cost-aware, and safe to connect to operational systems. That is where you can differentiate yourself.

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
8. **RAG (Retrieval-Augmented Generation)**: retrieve relevant documents, then provide them as evidence to the model.
9. **Tool calling**: the model requests a typed application function instead of directly executing arbitrary code.
10. **Agent**: an LLM-driven workflow that can select and use controlled tools to solve a task.
11. **Guardrails**: input, output, authorization, and policy controls around model behavior.
12. **Evaluation**: repeatable tests that measure answer quality, retrieval quality, and safety.
13. **Observability**: metrics, traces, logs, latency, tool usage, error rates, and model cost visibility.

---

## Learning Principles

### 1. Learn by shipping small increments

Use this cycle every week:

1. Learn one focused concept.
2. Implement one small feature using it.
3. Test it with realistic questions.
4. Record what failed and why.
5. Improve the code, prompts, tests, or runbooks.

### 2. Prefer evidence over impressive wording

The assistant should state the evidence it used, cite runbooks where possible, distinguish facts from assumptions, and admit when more data is needed.

### 3. Keep the first agent read-only

The early application may recommend commands such as:

```bash
kubectl describe pod <pod-name>
kubectl logs <pod-name> --previous
```

It must **not** execute them against real infrastructure. Do not allow the application to restart, scale, delete, deploy, or roll back systems until you have designed authorization, approval, audit, and policy controls.

### 4. Build an evaluation set early

Do not judge AI quality only by a few successful demos. Maintain representative questions and expected answer characteristics, then run them after prompt, model, retrieval, or tool changes.

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

Use technologies that align with the existing repository where possible.

| Area | Suggested direction |
| --- | --- |
| Language | Java; this project is already configured with a Java 25 toolchain. |
| Application framework | Spring Boot; this repository is already a Gradle Spring Boot project. |
| Build tool | Gradle Kotlin DSL, as already configured. |
| AI integration | A Spring-friendly AI integration library, added only when the basic application API exists. |
| Model access | Start with a hosted model API; compare with a local model later if privacy or cost is a learning goal. |
| Vector storage | Start simple; evolve to PostgreSQL with the pgvector extension for realistic RAG practice. |
| Documentation format | Markdown runbooks committed under `docs/runbooks/`. |
| Testing | JUnit 5, Spring Boot test support, and Testcontainers when PostgreSQL is added. |
| Local runtime | Docker Compose for the application and PostgreSQL. |
| Observability | Spring Boot Actuator first, then structured logging and tracing. |

Avoid adding every dependency at the beginning. Add dependencies only when a week’s milestone requires them.

---

## Repository Direction

Suggested future structure:

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
│           └── evaluation/
├── docker-compose.yml
├── Dockerfile
├── .env.example
└── README.md
```

---

# Ten-Week Roadmap

The schedule assumes about **five to eight hours per week**. If you have more time, do not rush through phases; use the extra time to test, document, and understand the work.

## Week 0 — Foundations and Project Setup

### Learn

- The difference between an LLM application, RAG, tool calling, and an agent.
- Why model output can be wrong, incomplete, or unsafe.
- Why secrets and logs require careful handling.

### Build

- Create a basic Spring Boot application entry point if it does not already exist.
- Add a health endpoint using Spring Boot Actuator.
- Define a versioned API prefix: `/api/v1`.
- Create a placeholder `POST /api/v1/chat` endpoint.
- Add a `README.md` with local startup instructions.
- Add `.env.example`; never commit real keys.

### Definition of done

- The application starts locally.
- A health endpoint responds successfully.
- The chat endpoint returns a clearly marked placeholder response.

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
- Unit-test the service with a mocked model client.

### Definition of done

- A valid question produces a model answer.
- Invalid input produces a `400` response.
- Missing model configuration produces an understandable startup or service error.

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

### Definition of done

- Responses deserialize into a validated DTO.
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

### Definition of done

- At least five runbooks exist.
- Every runbook contains practical, safe troubleshooting steps.
- Commands use placeholders and are clearly labelled as read-only, cautionary, or destructive.

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

### Definition of done

- A question about high CPU retrieves the high-CPU runbook.
- A question about `CrashLoopBackOff` retrieves the Kubernetes runbook.
- Responses include source metadata instead of pretending unsupported facts are verified.

---

## Week 5 — Safe Read-Only Tools and Agent Workflows

### Learn

- Tool/function calling.
- Typed tool schemas.
- Tool input validation.
- Tool result handling.
- Agent loops and maximum call limits.

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

### Definition of done

- The assistant can use mock deployment history to inform an answer.
- Tool calls are observable in logs.
- Invalid tool arguments are rejected safely.

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

### Definition of done

- The endpoint produces an incident summary and recommended checks.
- Automated tests demonstrate that known secret patterns are redacted.
- The response explains that its conclusions are hypotheses unless confirmed by evidence.

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

### Definition of done

- A short threat model exists in `docs/`.
- Security tests cover basic prompt-injection and unsafe-command scenarios.
- The service has explicit behavior when a user requests destructive actions.

---

## Week 8 — Testing and AI Evaluation

### Learn

- Difference between unit tests, integration tests, and AI evaluations.
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

Test at least:

- Input validation.
- Redaction behavior.
- Retrieval result relevance.
- Tool argument validation.
- Response schema validation.
- Safety rules for suggested actions.
- Error behavior for unavailable model or database integrations.

### Definition of done

- There is a repeatable evaluation suite.
- Regression tests identify when retrieval or safety behavior gets worse.
- The README explains how to run the tests.

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
- A basic CI workflow that builds and runs tests.
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

---

## Week 10 — Portfolio Polish

### Build

Create or improve:

- `README.md` with architecture, setup, API examples, and screenshots.
- An architecture diagram.
- A threat model.
- A list of sample prompts.
- Evaluation scenarios and results.
- Known limitations.
- A short demo video or terminal walkthrough.

### Definition of done

- A new developer can start the project locally from the README.
- The README explains RAG, tools, safety controls, and evaluation.
- The repository demonstrates engineering discipline rather than only a chatbot demo.

---

# Daily and Weekly Study Routine

For a weekday session of around 60 to 90 minutes:

| Time | Activity |
| --- | --- |
| 15 minutes | Read official documentation or a focused concept explanation. |
| 35–45 minutes | Implement one small, testable improvement. |
| 10–15 minutes | Try three to five realistic prompts or test scenarios. |
| 10 minutes | Write notes: what worked, failed, and what you learned. |
| 5 minutes | Make a focused commit with a meaningful message. |

For a weekend session of two to three hours:

1. Refactor only the code touched that week.
2. Add or improve tests.
3. Update documentation.
4. Run the full validation suite.
5. Review your learning notes and select the next week’s smallest milestone.

---

# Efficient Learning Strategy

## Use one primary source per topic

Avoid collecting many courses. For each concept:

1. Start with official framework or provider documentation.
2. Build a minimal working version in this repository.
3. Use one practical tutorial only when blocked.
4. Explain what you built in your own project documentation.

If you cannot explain the feature through your code and README, you have not learned it deeply enough.

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

These topics may be valuable later, but they are not the shortest path to becoming effective at building AI systems.

---

# Milestones

## Milestone 1 — AI Application Developer

You can:

- Call an LLM from a Spring Boot API.
- Manage configuration and secrets safely.
- Create system prompts and response DTOs.
- Handle provider errors and timeouts.

## Milestone 2 — RAG Developer

You can:

- Chunk and embed documents.
- Store and retrieve relevant content.
- Ground model answers in retrieved context.
- Show citations and explain evidence.

## Milestone 3 — Agentic Application Developer

You can:

- Define safe typed tools.
- Validate tool inputs and outputs.
- Coordinate model and tool interactions.
- Prevent unsafe actions by default.
- Make uncertainty visible to users.

## Milestone 4 — Production AI / Platform Engineer

You can:

- Containerize and deploy an AI-enabled service.
- Monitor model calls, retrieval, tools, errors, and latency.
- Evaluate quality and safety over time.
- Apply least privilege, auditing, and approval workflows.

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
- Automated tests and AI evaluation scenarios.
- Security controls and a threat model.
- Docker-based local deployment.
- Metrics, logs, and traces.
- Clear documentation of limitations and future work.

The most important outcome is practical confidence: you will understand how to design, build, test, secure, and operate an AI-enabled system using the engineering skills you already have.