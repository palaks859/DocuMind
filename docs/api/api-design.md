# DocuMind - REST API Design

**Status:** Proposed; not implemented
**API prefix:** `/api/v1`
**Style:** REST over HTTPS with JSON responses
**Backend proposal:** Java and Spring Boot
**Authentication proposal:** Short-lived access tokens; exact token flow remains to be decided

This document defines the proposed API contract for the MVP. Endpoint details may change during implementation, but changes must remain consistent with the requirements, database design, authorization model, and architecture decisions.

## 1. API Conventions

- All versioned endpoints use `/api/v1`.
- JSON request and response bodies use `application/json`, except file uploads and downloads.
- File uploads use `multipart/form-data`.
- Timestamps use ISO 8601 format in UTC.
- IDs use UUID strings.
- List endpoints support pagination where appropriate.
- Protected endpoints require authentication and workspace-level authorization.
- The server must not trust a workspace ID, document ID, or role supplied by a client without checking access.
- API secrets, passwords, access tokens, and raw credentials must never appear in logs.
- The API must not expose internal storage paths, stack traces, database details, or secrets.

### Pagination

List endpoints use:

`?page=0&size=20&sort=createdAt,desc`

Proposed defaults: `page=0`, `size=20`; maximum `size=100`.

Example response:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

### Standard error response

Use a consistent error format:

```json
{
  "timestamp": "2026-10-20T10:30:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "The request contains invalid fields.",
  "path": "/api/v1/example",
  "fieldErrors": [
    {
      "field": "name",
      "message": "Name is required."
    }
  ],
  "requestId": "server-generated-request-id"
}
```

`fieldErrors` may be omitted when not relevant. Error messages must not reveal sensitive implementation details.

## 2. Authentication

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/auth/register` | Create an account | Public |
| POST | `/auth/login` | Authenticate and obtain tokens or a session | Public |
| POST | `/auth/refresh` | Renew authentication, if refresh tokens are selected | Authenticated refresh flow |
| POST | `/auth/logout` | End the session or revoke refresh credentials | Authenticated |

Example registration request:

```json
{
  "email": "user@example.com",
  "password": "user-supplied-password",
  "displayName": "Example User"
}
```

Security requirements:

- Hash passwords using an established password-hashing algorithm.
- Validate email format and enforce password policy.
- Do not return password hashes.
- Use generic login failure messages to reduce account enumeration.
- Rate-limit registration, login, and token-related endpoints.
- Finalize access-token storage, refresh-token rotation, expiry, and revocation in the security design.

## 3. Workspaces and Membership

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/workspaces` | Create a workspace | Authenticated |
| GET | `/workspaces` | List workspaces accessible to the user | Authenticated |
| GET | `/workspaces/{workspaceId}` | Get workspace details | Workspace member |
| PATCH | `/workspaces/{workspaceId}` | Update workspace metadata | Admin |
| GET | `/workspaces/{workspaceId}/members` | List workspace members | Authorized member |
| POST | `/workspaces/{workspaceId}/members` | Add a member | Admin |
| PATCH | `/workspaces/{workspaceId}/members/{userId}` | Change a member role | Admin |
| DELETE | `/workspaces/{workspaceId}/members/{userId}` | Remove a member | Admin, subject to membership safeguards |

Supported roles are `ADMIN`, `REVIEWER`, and `VIEWER`.

Workspace creation and membership changes must enforce role and ownership rules. Prevent removal or demotion of the last required administrator.

## 4. Documents and Processing

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/workspaces/{workspaceId}/documents` | Upload a document | Admin or Reviewer |
| GET | `/workspaces/{workspaceId}/documents` | List workspace documents | Workspace member |
| GET | `/workspaces/{workspaceId}/documents/{documentId}` | Get metadata and processing status | Workspace member |
| GET | `/workspaces/{workspaceId}/documents/{documentId}/download` | Download the original file | Authorized member |
| DELETE | `/workspaces/{workspaceId}/documents/{documentId}` | Request document deletion | Admin |
| POST | `/workspaces/{workspaceId}/documents/{documentId}/reprocess` | Retry processing | Admin or Reviewer |

### Upload contract

Request: `multipart/form-data` with a `file` field.

The server must validate the actual file type, extension, file size, and upload contents. Enforce configurable limits and reject unsupported or unsafe files.

Proposed response: `202 Accepted`.

```json
{
  "id": "document-uuid",
  "workspaceId": "workspace-uuid",
  "filename": "contract.pdf",
  "status": "PENDING",
  "createdAt": "2026-10-20T10:30:00Z"
}
```

### Processing lifecycle

Proposed document states:

`PENDING -> PROCESSING -> READY`

Processing failures transition to `FAILED`. A retry returns the document to a processing state according to the final retry policy.

Clients poll the document detail endpoint for status. Processing should not require a long-running synchronous HTTP request.

Do not expose storage keys or internal filesystem locations in API responses. File retrieval must use an authorization-checked endpoint or a securely issued temporary download URL.

## 5. Chat and Source Citations

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/workspaces/{workspaceId}/chat/sessions` | Create a chat session | Workspace member; session history is owner-only |
| GET | `/workspaces/{workspaceId}/chat/sessions` | List the user's sessions | Session owner only; history listing is Later |
| GET | `/workspaces/{workspaceId}/chat/sessions/{sessionId}` | Get session metadata | Session owner only |
| DELETE | `/workspaces/{workspaceId}/chat/sessions/{sessionId}` | Delete a session | Session owner only |
| GET | `/workspaces/{workspaceId}/chat/sessions/{sessionId}/messages` | Retrieve messages and citations | Session owner only |
| POST | `/workspaces/{workspaceId}/chat/sessions/{sessionId}/messages` | Submit a question | Session owner only |

Example question request:

```json
{
  "content": "What is the termination notice period?",
  "documentIds": ["document-uuid"]
}
```

The `documentIds` field is optional if the product supports searching all accessible documents in the workspace.

Proposed response:

```json
{
  "id": "message-uuid",
  "sessionId": "session-uuid",
  "role": "ASSISTANT",
  "content": "The notice period is specified in the cited passage.",
  "citations": [
    {
      "documentId": "document-uuid",
      "chunkId": "chunk-uuid",
      "pageNumber": 12,
      "passageReference": "Section 8.2",
      "quotedText": "Relevant source passage"
    }
  ],
  "createdAt": "2026-10-20T10:30:00Z"
}
```

The example is illustrative, not a claim about an actual document.

Citation rules:

- Citations must reference source material actually retrieved and used.
- Validate that every cited document and chunk belongs to the active workspace.
- Preserve page and passage metadata when available.
- Clearly indicate when evidence is unavailable or insufficient.
- Never fabricate a quotation, page number, or source reference.
- Apply the final synchronous or asynchronous response strategy consistently.

## 6. Structured Data Extraction

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/workspaces/{workspaceId}/documents/{documentId}/extractions` | Start an extraction run | Admin or Reviewer |
| GET | `/workspaces/{workspaceId}/extractions/{runId}` | Get extraction status | Authorized member |
| GET | `/workspaces/{workspaceId}/extractions/{runId}/fields` | Retrieve extracted fields | Authorized member |
| POST | `/workspaces/{workspaceId}/extractions/{runId}/retry` | Retry a failed extraction | Admin or Reviewer |

Example request:

```json
{
  "schemaName": "contract-summary"
}
```

The selected schema must be registered and validated by the server. Do not execute arbitrary client-provided code or accept unrestricted extraction instructions.

Proposed start response: `202 Accepted`.

```json
{
  "id": "extraction-run-uuid",
  "documentId": "document-uuid",
  "status": "PENDING",
  "schemaName": "contract-summary"
}
```

Each extracted field should include its name, typed value, evidence, page number when available, and confidence when the extraction system can meaningfully provide it.

## 7. Compliance Checklists and Results

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/workspaces/{workspaceId}/checklists` | Create a checklist | Admin |
| GET | `/workspaces/{workspaceId}/checklists` | List checklists | Workspace member |
| GET | `/workspaces/{workspaceId}/checklists/{checklistId}` | Get checklist and criteria | Workspace member |
| PATCH | `/workspaces/{workspaceId}/checklists/{checklistId}` | Update checklist metadata | Admin |
| DELETE | `/workspaces/{workspaceId}/checklists/{checklistId}` | Delete a checklist | Admin, subject to retention rules |
| POST | `/workspaces/{workspaceId}/checklists/{checklistId}/items` | Add a criterion | Admin |
| PATCH | `/workspaces/{workspaceId}/checklists/{checklistId}/items/{itemId}` | Update a criterion | Admin |
| DELETE | `/workspaces/{workspaceId}/checklists/{checklistId}/items/{itemId}` | Remove a criterion | Admin |
| POST | `/workspaces/{workspaceId}/documents/{documentId}/compliance-evaluations` | Start an evaluation | Admin or Reviewer |
| GET | `/workspaces/{workspaceId}/documents/{documentId}/compliance-results` | Retrieve evaluation results | Authorized member |

Compliance evaluation may be asynchronous and should expose a status if processing takes time.

Supported result outcomes are `PASS`, `FAIL`, and `UNCLEAR`. Results should include evidence and an explanation where available. The product must not present automated results as guaranteed legal advice or verified human approval.

## 8. Audit Logs

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| GET | `/workspaces/{workspaceId}/audit-logs` | Search and paginate workspace audit events | Admin or explicitly authorized auditor |

Audit events should cover important security and data operations, including authentication events, membership changes, document upload/deletion, extraction, compliance evaluation, and permission changes.

Audit logs should be append-oriented and access-controlled. Do not expose passwords, tokens, raw credentials, or unnecessary document contents in log details.

## 9. HTTP Status Codes

| Status | Meaning | Typical use |
|---|---|---|
| 200 | OK | Successful reads and updates |
| 201 | Created | Resource created synchronously |
| 202 | Accepted | Background processing started |
| 204 | No Content | Successful deletion with no response body |
| 400 | Bad Request | Invalid request or validation failure |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Authenticated but not permitted |
| 404 | Not Found | Resource absent or intentionally concealed |
| 409 | Conflict | Duplicate resource or incompatible state |
| 413 | Payload Too Large | Upload exceeds configured limit |
| 415 | Unsupported Media Type | Unsupported content type |
| 422 | Unprocessable Content | Semantically invalid operation, if adopted |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Unexpected server error |
| 503 | Service Unavailable | Required service temporarily unavailable |

Use `404` consistently for inaccessible resources when revealing their existence would create an information leak. Otherwise, use `403` where appropriate.

## 10. Authorization and Tenant Isolation

- Deny access by default.
- Validate the authenticated user's workspace membership and role on every protected request.
- Verify resource ownership and workspace scope for nested resources.
- Do not rely only on UUID unpredictability to prevent unauthorized access.
- Apply tenant isolation to documents, chunks, citations, chats, extraction runs, checklists, results, and audit records.
- Validate both parent and child IDs when accepting nested routes.
- Keep authorization checks in the service layer and enforce appropriate database constraints.
- Return no cross-workspace search results or citations.
- Test horizontal and vertical privilege escalation, including attempts to access another workspace by changing IDs.

## 11. Validation, Reliability, and Observability

- Validate JSON fields, IDs, pagination parameters, upload sizes, and content types.
- Set request timeouts and bounded upload limits.
- Make retries safe; avoid creating duplicate jobs or results when an operation is repeated.
- Define idempotency behavior for uploads and background-job submissions before implementation.
- Return a request ID for support and troubleshooting.
- Use structured logs and metrics without logging sensitive content.
- Configure rate limits for authentication, upload, chat, and expensive AI operations.
- Document retryable versus non-retryable failures.
- Add API contract tests and authorization tests before release.

## 12. API Documentation and Testing

The implementation should publish an OpenAPI specification and, where appropriate, a Swagger UI restricted to approved environments.

Required test categories:

- Request validation and response contracts.
- Authentication and token/session lifecycle.
- Role-based authorization.
- Cross-workspace isolation.
- Upload validation and processing state transitions.
- Citation integrity and source traceability.
- Extraction and compliance result integrity.
- Pagination, rate limiting, and error handling.
- Safe deletion, retries, and duplicate requests.

## 13. Open Decisions

- Final access-token and refresh-token/session strategy.
- Whether chat responses are synchronous, streamed, or asynchronous.
- Exact upload size limits and accepted file formats.
- Whether document downloads use a proxy endpoint or temporary signed URLs.
- Idempotency-key behavior for uploads and job creation.
- Versioning policy for extraction schemas and checklist criteria.
- Retention and deletion behavior for documents, derived chunks, citations, and audit logs.
- Exact role permissions for every operation.
- API rate limits, quotas, and service-level targets.

These decisions must be resolved against the project requirements, security design, and architecture decisions before the API is treated as final.
