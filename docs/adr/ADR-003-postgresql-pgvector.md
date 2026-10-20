# ADR-003: PostgreSQL and pgvector

- **Status:** Proposed
- **Date:** 2026-10-20

## Context

DocuMind must persist users, workspaces, documents, conversations, extracted fields, audit events, and document embeddings.

## Decision

Use PostgreSQL for relational data and pgvector for storing and retrieving embedding vectors, subject to validating the required PostgreSQL and pgvector versions.

## Alternatives considered

- PostgreSQL with pgvector.
- A relational database with a separate vector database.
- Separate databases for application metadata and vector search.

## Consequences

- Relational entities can use foreign keys and transactions.
- Vector search can be integrated with document and workspace metadata.
- Database migrations and indexes must be managed explicitly.
- Queries must enforce workspace authorization.
- Vector dimensions must match the configured embedding model.

## Validation

Verify the PostgreSQL and pgvector versions, run a migration that creates a vector column, and test both vector retrieval and workspace isolation before accepting this decision.
