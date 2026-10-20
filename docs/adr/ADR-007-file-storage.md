# ADR-007: Local File Storage

- **Status:** Proposed
- **Date:** 2026-10-20

## Context

DocuMind needs to store uploaded source files independently of their extracted text, metadata, and searchable chunks.

## Decision

Use local file storage for the initial development environment. Keep storage operations behind a service interface where practical so a different storage backend can be introduced later.

## Alternatives considered

- Local filesystem storage.
- Object storage.
- A database-based storage approach.

## Consequences

- The application must define a safe storage location and file-naming strategy.
- Uploaded files must not be exposed through unrestricted public paths.
- File metadata and authorization remain managed by the backend.
- Backup, deletion, and retention behavior must be defined.
- Production deployment may require a different storage backend.

## Validation

Test upload, retrieval, deletion, access control, invalid files, and restart behavior before accepting this decision.
