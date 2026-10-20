-- DocuMind proposed PostgreSQL schema
-- Status: Design artifact only; not yet applied to a database.
-- Requires PostgreSQL 17+ and the pgvector extension.
-- VECTOR(384) assumes the proposed all-MiniLM-L6-v2 embedding model.

CREATE EXTENSION IF NOT EXISTS vector;

-- 1. Users
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- 2. Workspaces
CREATE TABLE workspaces (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Workspace membership and role
CREATE TABLE workspace_members (
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (workspace_id, user_id),
    CONSTRAINT ck_workspace_members_role
        CHECK (role IN ('ADMIN', 'REVIEWER', 'VIEWER'))
);

CREATE INDEX idx_workspace_members_user ON workspace_members(user_id);

-- 4. Documents
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    filename VARCHAR(500) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(1000) NOT NULL,
    checksum_sha256 CHAR(64),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    failure_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_documents_file_size CHECK (file_size > 0),
    CONSTRAINT ck_documents_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT uq_documents_id_workspace UNIQUE (id, workspace_id)
);

CREATE INDEX idx_documents_workspace_created
    ON documents(workspace_id, created_at DESC);
CREATE INDEX idx_documents_workspace_status
    ON documents(workspace_id, status);
CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);

-- 5. Document chunks and embeddings
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    page_number INTEGER,
    start_offset INTEGER,
    end_offset INTEGER,
    embedding VECTOR(384),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_document_chunks_index UNIQUE (document_id, chunk_index),
    CONSTRAINT uq_document_chunks_scope
        UNIQUE (id, document_id, workspace_id),
    CONSTRAINT fk_document_chunks_document_workspace
        FOREIGN KEY (document_id, workspace_id)
        REFERENCES documents(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT ck_document_chunks_index CHECK (chunk_index >= 0),
    CONSTRAINT ck_document_chunks_page
        CHECK (page_number IS NULL OR page_number > 0),
    CONSTRAINT ck_document_chunks_offsets
        CHECK (
            start_offset IS NULL OR end_offset IS NULL
            OR (start_offset >= 0 AND end_offset >= start_offset)
        )
);

CREATE INDEX idx_document_chunks_workspace_document
    ON document_chunks(workspace_id, document_id);

-- Add a vector index after validating pgvector version and retrieval strategy.
-- Example for cosine distance:
-- CREATE INDEX idx_document_chunks_embedding
--     ON document_chunks USING hnsw (embedding vector_cosine_ops);

-- 6. Chat sessions
CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    title VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_chat_sessions_id_workspace UNIQUE (id, workspace_id),
    CONSTRAINT fk_chat_sessions_workspace_member
        FOREIGN KEY (workspace_id, user_id)
        REFERENCES workspace_members(workspace_id, user_id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_sessions_workspace_user
    ON chat_sessions(workspace_id, user_id, updated_at DESC);

-- 7. Chat messages
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_chat_messages_role
        CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    CONSTRAINT fk_chat_messages_session_workspace
        FOREIGN KEY (session_id, workspace_id)
        REFERENCES chat_sessions(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT uq_chat_messages_id_workspace UNIQUE (id, workspace_id)
);

CREATE INDEX idx_chat_messages_session_created
    ON chat_messages(session_id, created_at);

-- 8. Message citations
CREATE TABLE message_citations (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    document_id UUID NOT NULL,
    chunk_id UUID,
    page_number INTEGER,
    passage_reference TEXT,
    quoted_text TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_message_citations_page
        CHECK (page_number IS NULL OR page_number > 0),
    CONSTRAINT fk_message_citations_message_workspace
        FOREIGN KEY (message_id, workspace_id)
        REFERENCES chat_messages(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_message_citations_document_workspace
        FOREIGN KEY (document_id, workspace_id)
        REFERENCES documents(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_message_citations_chunk_scope
        FOREIGN KEY (chunk_id, document_id, workspace_id)
        REFERENCES document_chunks(id, document_id, workspace_id)
        ON DELETE SET NULL (chunk_id)
);

CREATE INDEX idx_message_citations_message ON message_citations(message_id);
CREATE INDEX idx_message_citations_document ON message_citations(document_id);

-- 9. Extraction runs
CREATE TABLE extraction_runs (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    requested_by UUID REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    schema_name VARCHAR(150) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_extraction_runs_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT uq_extraction_runs_id_workspace UNIQUE (id, workspace_id),
    CONSTRAINT fk_extraction_runs_document_workspace
        FOREIGN KEY (document_id, workspace_id)
        REFERENCES documents(id, workspace_id) ON DELETE CASCADE
);

CREATE INDEX idx_extraction_runs_document_created
    ON extraction_runs(document_id, created_at DESC);

-- 10. Extracted fields
CREATE TABLE extracted_fields (
    id UUID PRIMARY KEY,
    extraction_run_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    field_name VARCHAR(200) NOT NULL,
    field_value JSONB,
    evidence TEXT,
    page_number INTEGER,
    confidence NUMERIC(5,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_extracted_fields_confidence
        CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1),
    CONSTRAINT ck_extracted_fields_page
        CHECK (page_number IS NULL OR page_number > 0),
    CONSTRAINT fk_extracted_fields_run_workspace
        FOREIGN KEY (extraction_run_id, workspace_id)
        REFERENCES extraction_runs(id, workspace_id) ON DELETE CASCADE
);

CREATE INDEX idx_extracted_fields_run ON extracted_fields(extraction_run_id);

-- 11. Workspace compliance checklists
CREATE TABLE checklists (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(250) NOT NULL,
    description TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_checklists_id_workspace UNIQUE (id, workspace_id)
);

CREATE INDEX idx_checklists_workspace ON checklists(workspace_id);

-- 12. Checklist criteria
CREATE TABLE checklist_items (
    id UUID PRIMARY KEY,
    checklist_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    criterion VARCHAR(250) NOT NULL,
    description TEXT,
    item_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_checklist_items_order CHECK (item_order >= 0),
    CONSTRAINT fk_checklist_items_checklist_workspace
        FOREIGN KEY (checklist_id, workspace_id)
        REFERENCES checklists(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT uq_checklist_items_id_workspace UNIQUE (id, workspace_id)
);

CREATE INDEX idx_checklist_items_checklist_order
    ON checklist_items(checklist_id, item_order);

-- 13. Compliance results
CREATE TABLE compliance_results (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    checklist_item_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    evidence TEXT,
    explanation TEXT,
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_compliance_results_outcome
        CHECK (outcome IN ('PASS', 'FAIL', 'UNCLEAR')),
    CONSTRAINT fk_compliance_results_document_workspace
        FOREIGN KEY (document_id, workspace_id)
        REFERENCES documents(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_compliance_results_item_workspace
        FOREIGN KEY (checklist_item_id, workspace_id)
        REFERENCES checklist_items(id, workspace_id) ON DELETE CASCADE
);

CREATE INDEX idx_compliance_results_document
    ON compliance_results(document_id, created_at DESC);
CREATE INDEX idx_compliance_results_checklist_item
    ON compliance_results(checklist_item_id);

-- 14. Audit logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(150) NOT NULL,
    target_type VARCHAR(100),
    target_id UUID,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_workspace_created
    ON audit_logs(workspace_id, created_at DESC);
CREATE INDEX idx_audit_logs_actor_created
    ON audit_logs(actor_user_id, created_at DESC);
