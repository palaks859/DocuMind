# ADR-006: JWT-Based Authentication

- **Status:** Proposed
- **Date:** 2026-10-20

## Context

DocuMind needs authentication for protected API operations and authorization that can be enforced by the backend.

## Decision

Use JWT-based access tokens and refresh tokens as the planned authentication approach, subject to security review and implementation validation.

Passwords will be stored using an appropriate password-hashing mechanism, such as BCrypt.

## Alternatives considered

- JWT access and refresh tokens.
- Server-side session authentication.
- An external identity provider.

## Consequences

- Token expiry, refresh, revocation, and logout behavior must be defined.
- Signing keys and other secrets must remain outside source control.
- The backend must verify tokens and enforce resource permissions.
- Authentication must not replace workspace-level authorization checks.
- Passwords and tokens must not be written to logs.

## Validation

Test invalid, expired, and refreshed tokens; password verification; logout or revocation behavior; and unauthorized workspace access before accepting this decision.
