# ADR-002: Maven for Backend Build Management

- **Status:** Proposed
- **Date:** 2026-10-20

## Context

The Java backend requires reproducible dependency management, compilation, testing, and packaging.

## Decision

Use Apache Maven as the backend build and dependency-management tool.

## Alternatives considered

- Apache Maven.
- Gradle.

## Consequences

- Dependencies and build plugins will be defined in `pom.xml`.
- Common build and test commands will be documented.
- Dependency versions should be controlled to keep builds reproducible.

## Validation

Create the backend Maven project and verify that a clean build and test run succeed before accepting this decision.
