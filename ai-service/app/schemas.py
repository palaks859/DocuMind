from typing import List, Optional
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
