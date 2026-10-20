# DocuMind — Architecture Diagrams

**Status:** Proposed architecture  
**Last updated:** 2026-10-20

These diagrams describe the planned system. They are not evidence that the services have been implemented.

## 1. System Context

Shows the people who use DocuMind and the external systems it may depend on.

```mermaid
flowchart TD
    Admin[Workspace Admin]
    Reviewer[Document Reviewer]
    Viewer[Document Viewer]

    subgraph DocuMind["DocuMind Platform"]
        Platform[Document Intelligence Platform]
    end

    LLM[External LLM Provider]

    Admin -->|Manage membership and roles| Platform
    Reviewer -->|Upload, search, extract, review| Platform
    Viewer -->|Search and ask questions| Platform
    Platform -->|Grounded generation requests| LLM
    LLM -->|Generated output| Platform
```

## 2. Container Architecture

Shows the planned major application components and their responsibilities.

```mermaid
flowchart TD
    User[User]
    Frontend[React and TypeScript Frontend]
    Backend[Java and Spring Boot Backend API]
    AI[Python and FastAPI AI Service]
    Database[(PostgreSQL and pgvector)]
    Storage[(Local File Storage)]
    LLM[LLM Provider]

    User -->|HTTPS| Frontend
    Frontend -->|Versioned REST API| Backend
    Backend -->|Metadata and application queries| Database
    Backend -->|Authorized file operations| Storage
    Backend -->|Authenticated internal API| AI
    AI -->|Embedding and retrieval operations| Database
    AI -->|Generation requests| LLM
```

## 3. Sequence: Document Upload and Processing

```mermaid
sequenceDiagram
    actor User
    participant UI as React Frontend
    participant API as Spring Boot API
    participant DB as PostgreSQL
    participant FS as File Storage
    participant Worker as Processing Worker
    participant AI as AI Service

    User->>UI: Select and upload document
    UI->>API: Submit authenticated upload
    API->>API: Authenticate and authorize
    API->>API: Validate file type and size
    API->>FS: Store validated file
    API->>DB: Create document record
    API-->>UI: Return document ID and status
    API->>Worker: Schedule processing
    Worker->>FS: Read stored document
    Worker->>AI: Request text processing and embeddings
    AI->>DB: Store chunks and vectors
    AI-->>Worker: Processing result
    Worker->>DB: Update processing status
```

## 4. Sequence: Question Answering

```mermaid
sequenceDiagram
    actor User
    participant UI as React Frontend
    participant API as Spring Boot API
    participant AI as Python AI Service
    participant DB as PostgreSQL
    participant LLM as LLM Provider

    User->>UI: Ask a document question
    UI->>API: Submit authenticated question
    API->>API: Check user and workspace permissions
    API->>AI: Send authorized query
    AI->>DB: Retrieve relevant authorized chunks
    DB-->>AI: Candidate passages and source metadata
    AI->>AI: Evaluate evidence sufficiency
    alt Sufficient evidence
        AI->>LLM: Request evidence-grounded answer
        LLM-->>AI: Generated answer
        AI->>AI: Validate answer and citations
        AI-->>API: Answer with source references
    else Insufficient evidence
        AI-->>API: Not-found or uncertainty response
    end
    API-->>UI: Return authorized response
    UI-->>User: Display answer and citations
```

## 5. Sequence: Structured Extraction

```mermaid
sequenceDiagram
    actor Reviewer
    participant UI as React Frontend
    participant API as Spring Boot API
    participant AI as Python AI Service
    participant FS as File Storage
    participant DB as PostgreSQL
    participant LLM as LLM Provider

    Reviewer->>UI: Request extraction
    UI->>API: Submit extraction request
    API->>API: Authenticate and authorize
    API->>DB: Verify document access
    API->>AI: Request schema-based extraction
    AI->>FS: Retrieve authorized source file or extracted text
    AI->>LLM: Request structured output
    LLM-->>AI: Candidate extracted fields
    AI->>AI: Validate output schema
    AI->>DB: Store fields and available evidence
    AI-->>API: Extraction result
    API-->>UI: Return result
    UI-->>Reviewer: Display fields and source evidence
```

## 6. Sequence: Compliance Check

```mermaid
sequenceDiagram
    actor Reviewer
    participant UI as React Frontend
    participant API as Spring Boot API
    participant AI as Python AI Service
    participant DB as PostgreSQL
    participant LLM as LLM Provider

    Reviewer->>UI: Request compliance check
    UI->>API: Submit check request
    API->>API: Authenticate and authorize
    API->>DB: Verify document access and load criteria
    API->>AI: Send authorized evidence and criteria
    AI->>LLM: Evaluate evidence against criteria
    LLM-->>AI: Candidate findings
    AI->>AI: Validate findings and evidence
    AI->>DB: Store results and check status
    AI-->>API: PASS, FAIL, or UNCLEAR results
    API-->>UI: Return check results
    UI-->>Reviewer: Display findings and supporting evidence
```

## 7. Architecture Notes and Open Decisions

- All protected resources require backend authentication and authorization.
- Workspace isolation must apply to document metadata, files, chunks, conversations, extracted fields, and compliance results.
- The exact asynchronous worker or queue mechanism remains undecided.
- The LLM provider remains undecided.
- The file-storage approach is proposed for initial development and may change for deployment.
- The internal AI service must not expose unrestricted public endpoints.
- Processing failures, timeouts, retries, and duplicate processing require explicit handling.
- Citation references must correspond to actual stored evidence.
- These diagrams must be updated when accepted ADRs or implementation details change.
