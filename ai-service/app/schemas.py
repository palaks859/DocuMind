from typing import Any, List, Optional
from pydantic import BaseModel, Field


class PageContent(BaseModel):
    page_number: int = Field(..., ge=1)
    text: str


class DocumentChunkOut(BaseModel):
    chunk_index: int = Field(..., ge=0)
    content: str
    token_count: int = Field(..., ge=0)
    page_number: Optional[int] = Field(None, ge=1)
    page_start: Optional[int] = Field(None, ge=1)
    page_end: Optional[int] = Field(None, ge=1)
    start_offset: int = Field(..., ge=0)
    end_offset: int = Field(..., ge=0)
    embedding: List[float] = Field(default_factory=list)


class IngestResponse(BaseModel):
    filename: str
    content_type: str
    page_count: int = Field(..., ge=1)
    total_chunks: int = Field(..., ge=0)
    embedding_model: str
    embedding_model_revision: str
    embedding_dimension: int
    chunks: List[DocumentChunkOut]


class StorageIngestRequest(BaseModel):
    storage_key: str = Field(..., min_length=1)
    filename: str = Field(..., min_length=1)
    content_type: str = Field(..., min_length=1)
    chunk_size: Optional[int] = Field(None, ge=200, le=8000)
    chunk_overlap: Optional[int] = Field(None, ge=0, le=2000)


class EmbedRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1, max_length=256)
    is_query: bool = False


class EmbedResponse(BaseModel):
    model: str
    revision: str
    dimension: int
    embeddings: List[List[float]]


class RagContextChunk(BaseModel):
    chunk_id: str
    document_id: str
    document_filename: str
    chunk_index: int
    page_number: Optional[int] = None
    page_start: Optional[int] = None
    page_end: Optional[int] = None
    content: str
    similarity_score: float


class RagHistoryTurn(BaseModel):
    role: str
    content: str


class RagAnswerRequest(BaseModel):
    question: str = Field(..., min_length=1, max_length=4000)
    chunks: List[RagContextChunk] = Field(default_factory=list)
    history: List[RagHistoryTurn] = Field(default_factory=list)
    min_similarity: Optional[float] = Field(None, ge=0.0, le=1.0)


class RagCitationOut(BaseModel):
    chunk_id: str
    document_id: str
    document_filename: str
    page_number: Optional[int] = None
    page_start: Optional[int] = None
    page_end: Optional[int] = None
    passage_reference: str
    quoted_text: str
    relevance_score: float


class RagAnswerResponse(BaseModel):
    answer: str
    found_answer: bool
    prompt_version: str
    model_provider: str
    model_name: str
    citations: List[RagCitationOut] = Field(default_factory=list)


class DocumentChunkInput(BaseModel):
    chunk_id: str
    chunk_index: int
    page_number: Optional[int] = None
    page_start: Optional[int] = None
    page_end: Optional[int] = None
    content: str


class FieldSpecInput(BaseModel):
    field_name: str = Field(..., min_length=1, max_length=200)
    description: Optional[str] = None


class FieldExtractRequest(BaseModel):
    schema_name: str = Field(..., min_length=1, max_length=150)
    document_filename: str
    fields: List[FieldSpecInput] = Field(default_factory=list)
    chunks: List[DocumentChunkInput] = Field(..., min_length=1)


class ExtractedFieldOut(BaseModel):
    field_name: str
    field_value: Any
    source_chunk_id: Optional[str] = None
    evidence: Optional[str] = None
    page_number: Optional[int] = None
    page_start: Optional[int] = None
    page_end: Optional[int] = None
    confidence: float


class FieldExtractResponse(BaseModel):
    schema_name: str
    prompt_version: str
    model_provider: str
    model_name: str
    fields: List[ExtractedFieldOut]


class ChecklistCriterionInput(BaseModel):
    item_id: str
    criterion: str
    description: Optional[str] = None


class ComplianceEvaluateRequest(BaseModel):
    document_filename: str
    criteria: List[ChecklistCriterionInput] = Field(..., min_length=1)
    chunks: List[DocumentChunkInput] = Field(..., min_length=1)


class ComplianceItemResultOut(BaseModel):
    checklist_item_id: str
    source_chunk_id: Optional[str] = None
    outcome: str
    evidence: Optional[str] = None
    explanation: str


class ComplianceEvaluateResponse(BaseModel):
    prompt_version: str
    model_provider: str
    model_name: str
    results: List[ComplianceItemResultOut]
