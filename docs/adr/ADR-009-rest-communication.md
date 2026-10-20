# ADR-009: REST Communication Between Services

- **Status:** Proposed
- **Date:** 2026-10-20

## Context

DocuMind separates the frontend, Java backend API, and Python AI service. These components need documented interfaces for their interactions.

## Decision

Use REST APIs over HTTP for communication between the frontend and backend and for backend-to-AI-service requests.

Internal AI endpoints are planned for ingestion, querying, extraction, compliance checks, and health checks. Internal endpoints must not be exposed as unrestricted public operations.

## Alternatives considered

- REST over HTTP.
- Remote procedure call frameworks.
- Message-based communication for selected workflows.

## Consequences

- API contracts, validation, and error formats must be documented.
- Internal service communication must use an appropriate authentication mechanism.
- Timeouts and dependency failures must be handled explicitly.
- Long-running document processing should use the asynchronous workflow described in ADR-008.
- API versions and backward compatibility must be considered.

## Validation

Test service authentication, request validation, error responses, timeout handling, and access restrictions before accepting this decision.
