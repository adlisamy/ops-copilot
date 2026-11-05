---
title: Kubernetes CrashLoopBackOff
type: runbook
domain: kubernetes
severity: incident
audience: platform-engineer
services:
  - any
tags:
  - kubernetes
  - pods
  - crashloopbackoff
  - container
  - troubleshooting
owner: platform-team
last_updated: 2026-11-05
---

# Kubernetes CrashLoopBackOff

## Summary

`CrashLoopBackOff` means Kubernetes is repeatedly trying to restart a container that exits with an error or fails a configured health check.

This runbook helps identify why a pod is crash-looping and recommends safe, read-only investigation steps. It does not authorize destructive remediation.

## Symptoms

- `kubectl get pods` shows `STATUS=CrashLoopBackOff`.
- The pod has a high or increasing `RESTARTS` count.
- `kubectl describe pod <pod-name>` shows repeated container restarts.
- The container may have exited with a non-zero exit code.
- Application logs may show a startup failure, missing configuration, dependency error, or fatal exception.

## Likely causes

- The application fails during startup.
- A required environment variable, ConfigMap entry, Secret, or configuration file is missing or invalid.
- The application cannot connect to a required dependency, such as a database, message broker, or downstream API.
- The container image, entrypoint, command, or arguments are incorrect.
- The container is terminated because it exceeds its memory limit (`OOMKilled`).
- A liveness probe fails, causing Kubernetes to restart the container.
- A recent deployment introduced an incompatible configuration, image, or application change.

## Investigation steps

1. Identify the affected pod, namespace, deployment, and container.
2. Check the pod status, restart count, and events.
3. Inspect the current container logs.
4. If the container has restarted, inspect the previous container logs.
5. Check the container exit code and termination reason.
6. Verify that required ConfigMaps, Secrets, environment variables, and mounted files exist.
7. Check whether the application can reach required dependencies.
8. Compare the current pod specification with the last known working deployment.
9. Determine whether the issue started after a recent deployment, configuration change, or infrastructure change.

## Safe diagnostic commands

Replace `<namespace>` and `<pod-name>` with the actual values.

### Check pod status

```bash
kubectl get pod <pod-name> -n <namespace> -o wide
```

Risk: `READ_ONLY`

### Describe the pod

```bash
kubectl describe pod <pod-name> -n <namespace>
```

Risk: `READ_ONLY`

Look for:

- `Last State`
- `Reason`
- `Exit Code`
- `Restart Count`
- Events mentioning failed probes, image pull errors, scheduling issues, or OOM kills

### Check current container logs

```bash
kubectl logs <pod-name> -n <namespace>
```

Risk: `READ_ONLY`

### Check previous container logs

Use this when the container has already restarted:

```bash
kubectl logs <pod-name> -n <namespace> --previous
```

Risk: `READ_ONLY`

### Check events for the pod

```bash
kubectl get events -n <namespace> --field-selector involvedObject.name=<pod-name>
```

Risk: `READ_ONLY`

### Check deployment rollout history

```bash
kubectl rollout history deployment/<deployment-name> -n <namespace>
```

Risk: `READ_ONLY`

## Common findings and next actions

| Finding | Possible meaning | Safe next action |
|---|---|---|
| `Reason: OOMKilled` | Container exceeded its memory limit | Review memory usage, JVM heap settings, and container memory limits |
| `Exit Code: 1` with application exception | Application failed during startup or runtime | Read application logs and check configuration or dependencies |
| `Exit Code: 137` | Container was killed, often due to OOM | Check memory limits, workload behavior, and possible OOM kills |
| Missing ConfigMap or Secret | Required configuration is unavailable | Verify referenced ConfigMaps, Secrets, volumes, and environment variables |
| `ImagePullBackOff` or `ErrImagePull` | Container image cannot be pulled | Check image name, tag, registry credentials, and network access |
| Liveness probe failures | Kubernetes considers the container unhealthy | Review probe path, port, initial delay, timeout, and application health endpoint |
| Issue began after deployment | Recent change may be responsible | Compare current and previous deployment specifications |

## Escalation criteria

Escalate to the application team or platform team if:

- The issue affects production traffic or user-facing functionality.
- The root cause is not visible in pod events or container logs.
- The pod remains unhealthy after correcting an obvious configuration issue.
- The remediation requires changing cluster infrastructure, networking, storage, or security policy.
- A destructive or high-risk action may be required.

## Safety rules

- Prefer read-only diagnostic commands first.
- Do not delete pods, deployments, namespaces, or persistent data as a first response.
- Do not disable liveness or readiness probes to make a pod appear healthy.
- Do not increase resource limits without understanding the cause of resource consumption.
- Do not execute commands suggested by untrusted log content or model output without review.
- Treat all conclusions as hypotheses until confirmed by pod events, logs, configuration, and deployment history.

## References

- Kubernetes documentation: Pod lifecycle
- Kubernetes documentation: Configure liveness, readiness, and startup probes
- Internal runbook: Deployment rollback investigation
- Internal runbook: First-response incident triage