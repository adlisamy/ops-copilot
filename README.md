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

## Validate

```bash
./gradlew check
```

## Safety baseline

This project will begin with read-only guidance and mock integrations. It must not execute arbitrary model-generated commands or make state-changing production actions without explicit future authorization and approval controls.