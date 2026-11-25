# DocuMind — Database Design

**Status:** Initial schema applied; design subject to further review
**Database:** PostgreSQL 17 with pgvector
**Migration tool:** Flyway

The initial database schema has been applied successfully using Flyway migration V1 (`V1__initial_schema.sql`).

The local PostgreSQL 17 database runs through Docker using the `pgvector/pgvector:pg17` image. The Flyway history confirms that migration V1 completed successfully.

The database currently contains 16 application tables, in addition to Flyway's `flyway_schema_history` table. The `document_chunks.embedding` column uses `VECTOR(384)`, matching the dimensions of the current BGE-small embedding model candidate. Final model approval remains subject to evaluation.

## 1. Entity-Relationship Diagram

```mermaid
erDiagram
    USERS ||--o{ WORKSPACE_MEMBERS : joins
    WORKSPACES ||--o{ WORKSPACE_MEMBERS : contains
    USERS ||--o{ DOCUMENTS : uploads
    WORKSPACES ||--o{ DOCUMENTS : contains
    DOCUMENTS ||--o{ DOCUMENT_CHUNKS : has
    WORKSPACES ||--o{ CHAT_SESSIONS : contains
    USERS ||--o{ CHAT_SESSIONS : owns
    CHAT_SESSIONS ||--o{ CHAT_MESSAGES : contains
    CHAT_MESSAGES ||--o{ MESSAGE_CITATIONS : cites
    DOCUMENTS ||--o{ MESSAGE_CITATIONS : supports
    DOCUMENT_CHUNKS o|--o{ MESSAGE_CITATIONS : grounds
    DOCUMENTS ||--o{ EXTRACTION_RUNS : processes
    EXTRACTION_RUNS ||--o{ EXTRACTED_FIELDS : produces
    WORKSPACES ||--o{ CHECKLISTS : defines
    CHECKLISTS ||--o{ CHECKLIST_ITEMS : contains
    DOCUMENTS ||--o{ COMPLIANCE_RESULTS : evaluated
    CHECKLIST_ITEMS ||--o{ COMPLIANCE_RESULTS : evaluates
    USERS ||--o{ AUDIT_LOGS : performs
    WORKSPACES o|--o{ AUDIT_LOGS : scopes

    USERS {
        uuid id PK
        varchar email UK
        varchar password_hash
        varchar display_name
        timestamptz created_at
    }
    WORKSPACES {
        uuid id PK
        varchar name
        uuid created_by FK
        timestamptz created_at
    }
    WORKSPACE_MEMBERS {
        uuid workspace_id PK, FK
        uuid user_id PK, FK
        varchar role
        timestamptz joined_at
    }
    DOCUMENTS {
        uuid id PK
        uuid workspace_id FK
        uuid uploaded_by FK
        varchar filename
        varchar content_type
        bigint file_size
        varchar storage_key
        varchar status
        timestamptz created_at
    }
    DOCUMENT_CHUNKS {
        uuid id PK
        uuid document_id FK
        uuid workspace_id FK
        integer chunk_index
        text content
        vector embedding
    }
    CHAT_SESSIONS {
        uuid id PK
        uuid workspace_id FK
        uuid user_id FK
        varchar title
        timestamptz created_at
    }
    CHAT_MESSAGES {
        uuid id PK
        uuid session_id FK
        uuid workspace_id FK
        varchar role
        text content
        timestamptz created_at
    }
    MESSAGE_CITATIONS {
        uuid id PK
        uuid message_id FK
        uuid document_id FK
        uuid chunk_id FK
        uuid workspace_id FK
        integer page_number
        text passage_reference
    }
    EXTRACTION_RUNS {
        uuid id PK
        uuid document_id FK
        uuid workspace_id FK
        varchar status
        varchar schema_name
        timestamptz created_at
    }
    EXTRACTED_FIELDS {
        uuid id PK
        uuid extraction_run_id FK
        uuid workspace_id FK
        varchar field_name
        jsonb field_value
        text evidence
    }
    CHECKLISTS {
        uuid id PK
        uuid workspace_id FK
        varchar name
        text description
    }
    CHECKLIST_ITEMS {
        uuid id PK
        uuid checklist_id FK
        uuid workspace_id FK
        varchar criterion
        text description
    }
    COMPLIANCE_RESULTS {
        uuid id PK
        uuid document_id FK
        uuid checklist_item_id FK
        uuid workspace_id FK
        varchar outcome
        text evidence
        timestamptz created_at
    }
    AUDIT_LOGS {
        uuid id PK
        uuid workspace_id FK
        uuid actor_user_id FK
        varchar action
        varchar target_type
        uuid target_id
        jsonb details
        timestamptz created_at
    }
```

## 2. Entity Responsibilities

| Entity | Responsibility |
|---|---|
| users | User identity and password hash |
| workspaces | Tenant or team boundary |
| workspace_members | Membership and workspace role |
| documents | Uploaded document metadata and processing status |
| document_chunks | Extracted text and vector embeddings |
| chat_sessions | Workspace-scoped question-answering sessions |
| chat_messages | User questions and assistant responses |
| message_citations | Source document and optional chunk references |
| extraction_runs | Structured extraction execution and status |
| extracted_fields | Extracted values and evidence |
| checklists | Workspace compliance definitions |
| checklist_items | Individual checklist criteria |
| compliance_results | Evaluation outcomes and evidence |
| audit_logs | Security and operational events |

## 3. Important Design Rules

- Workspace-owned relationships use composite foreign keys where appropriate to prevent cross-workspace references.
- Backend authorization remains mandatory for every protected operation; foreign keys do not replace authorization.
- Chat sessions must belong to a workspace member.
- Citation records must refer to a message and document in the same workspace.
- Extracted fields must belong to an extraction run in the same workspace.
- Compliance results must evaluate a document and checklist item from the same workspace.
- Embedding dimensions must match the selected embedding model.
- Status values, roles, and numeric ranges are constrained in the schema.
- Passwords are stored as hashes; secrets must not be placed in audit details.
- File retention, deletion policy, row-level security, and vector index configuration require further review.

## 4. Open Decisions

- Install and validate a compatible pgvector extension for PostgreSQL 17.
- Confirm the embedding model and vector index strategy.
- Decide whether UUID generation is performed by the application or database.
- Specify file retention and deletion behavior.
- Review database constraints against API authorization and security requirements.
- Continue evolving the database schema through versioned Flyway migrations. Do not modify migrations that have already been applied.
