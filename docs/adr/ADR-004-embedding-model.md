# ADR-004: Embedding Model

- **Status:** Proposed
- **Date:** 2026-10-20

## Context

DocuMind needs vector embeddings to represent document chunks and user queries for semantic retrieval.

## Decision

Use `BAAI/bge-small-en-v1.5` as the initial candidate embedding model. Its planned output dimension is 384.

## Alternatives considered

- `sentence-transformers/all-MiniLM-L6-v2` (384 dimensions; shorter model input limit).
- Other sentence-transformer embedding models.
- A hosted embedding API.

## Consequences

- Document chunks and queries must use compatible model versions.
- The vector column must use the expected 384 dimensions if this model is retained.
- Retrieval quality and latency must be evaluated against the project dataset.
- Changing the embedding model may require re-embedding existing document chunks.

## Validation

Confirm the selected model and version, verify output dimensions, and measure retrieval quality before accepting this decision.
