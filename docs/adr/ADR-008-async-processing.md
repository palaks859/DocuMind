# ADR-008: Asynchronous Document Processing

- **Status:** Proposed
- **Date:** 2026-10-20

## Context

Document extraction, chunking, embedding generation, and indexing may take longer than a normal API request should remain open.

## Decision

Use an asynchronous processing workflow for document ingestion. The API will record the document and processing status, then arrange for processing to occur outside the upload request.

The specific queue or job mechanism remains to be selected during implementation planning.

## Alternatives considered

- Synchronous processing within the upload request.
- Background jobs managed by the application.
- A dedicated message broker and worker.

## Consequences

- The system must represent pending, processing, successful, and failed states.
- Processing operations should be retryable and safe against unintended duplicate indexing.
- Failures must be recorded and diagnosable.
- The chosen mechanism must account for concurrent processing and application restarts.
- Queue technology should not be assumed until selected explicitly.

## Validation

Test status transitions, retry behavior, duplicate prevention, failure recovery, and concurrent processing before accepting this decision.
