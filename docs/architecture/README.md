# Architecture Notes

Add diagrams and short explanations of the system as it evolves.

The intended high-level flow is:

```text
HTTP API -> application use case -> model/retrieval/tool adapters -> response
```

The application must keep external integrations behind the infrastructure layer and must not allow model output to execute arbitrary commands.