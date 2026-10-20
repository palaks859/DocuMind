# ADR-005: Large Language Model Provider

- **Status:** Pending
- **Date:** 2026-10-20

## Context

DocuMind needs a language model to generate evidence-grounded answers and potentially assist with structured extraction and compliance checks.

## Decision

The provider and model have not yet been selected. Selection is pending evaluation of cost, availability, privacy, latency, output quality, and integration requirements.

## Options to evaluate

- A hosted LLM API.
- A locally hosted model.
- Another provider compatible with the project's security and operational requirements.

No provider is approved by this record.

## Consequences

- Provider-specific implementation must wait until the decision is made.
- Credentials must be stored outside source control.
- The application should handle timeouts, rate limits, and provider failures.
- Outputs must be validated and grounded in retrieved evidence.
- Evaluation must not assume that generated answers are always correct.

## Follow-up

Document the selected provider, model, configuration, privacy considerations, timeout policy, and fallback behavior in this record when a decision is made.
