# ADR-010: Chat History Privacy

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

DocuMind supports workspace-based document Q&A. Chat history can contain sensitive questions, answers, and document citations. Workspace membership or an administrative role should not automatically grant access to another member's chat history.

## Decision

- In the MVP, chat sessions, messages, and citations are accessible only to the session owner.
- Administrators cannot access another member's chat history by default.
- Removing a member revokes their workspace access. Retained chat sessions and messages are not automatically deleted, but become inaccessible to other members.
- Retained chat data remains subject to the separately defined retention and deletion policy.
- Any administrative access override is a Later feature and must require explicit authorization and audit logging.
- Conversation-history listing and revisiting are Later features. This does not prevent a user from submitting a question and receiving an answer in the current Q&A flow.

## Consequences

- APIs must enforce session ownership for reading session metadata, messages, and citations.
- Workspace role checks alone are insufficient to authorize chat-history access.
- Retention and deletion behavior must be documented separately from access revocation.
- Administrative access must not be added implicitly.

## Scope

This decision applies to the MVP chat-history privacy model. It does not change the status of structured extraction, compliance checklists, or analytics features.
