from pathlib import Path
from typing import Optional
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from app.chunker import chunk_pages
from app.config import settings
from app.parsers import parse_document_bytes
from app.schemas import IngestResponse, StorageIngestRequest

app = FastAPI(
    title="DocuMind AI Service",
    version=settings.service_version,
    description="Document parsing, page-aware chunking, embeddings, and grounded RAG microservice",
)


@app.get("/health")
def health() -> dict:
    return {
        "status": "UP",
        "service": settings.service_name,
        "version": settings.service_version,
    }


@app.post("/api/v1/ingest/parse", response_model=IngestResponse)
async def ingest_uploaded_file(
    file: UploadFile = File(...),
    chunk_size: Optional[int] = Form(None),
    chunk_overlap: Optional[int] = Form(None),
) -> IngestResponse:
    c_size = chunk_size or settings.default_chunk_size
    c_overlap = chunk_overlap if chunk_overlap is not None else settings.default_chunk_overlap

    file_bytes = await file.read()
    if not file_bytes:
        raise HTTPException(status_code=400, detail="Uploaded file is empty.")

    try:
        pages = parse_document_bytes(
            file_bytes=file_bytes,
            content_type=file.content_type or "application/octet-stream",
            filename=file.filename or "document",
        )
        chunks = chunk_pages(pages, chunk_size=c_size, chunk_overlap=c_overlap)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    return IngestResponse(
        filename=file.filename or "document",
        content_type=file.content_type or "application/octet-stream",
        page_count=len(pages),
        total_chunks=len(chunks),
        chunks=chunks,
    )


@app.post("/api/v1/ingest/from-storage", response_model=IngestResponse)
def ingest_from_local_storage(payload: StorageIngestRequest) -> IngestResponse:
    c_size = payload.chunk_size or settings.default_chunk_size
    c_overlap = payload.chunk_overlap if payload.chunk_overlap is not None else settings.default_chunk_overlap

    root = settings.storage_root_path
    target_path = (root / payload.storage_key).resolve()
    if not str(target_path).startswith(str(root)):
        raise HTTPException(status_code=400, detail="Invalid storage_key path traversal attempt.")
    if not target_path.is_file():
        raise HTTPException(status_code=404, detail=f"Stored file not found: {payload.storage_key}")

    try:
        file_bytes = target_path.read_bytes()
        pages = parse_document_bytes(
            file_bytes=file_bytes,
            content_type=payload.content_type,
            filename=payload.filename,
        )
        chunks = chunk_pages(pages, chunk_size=c_size, chunk_overlap=c_overlap)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    return IngestResponse(
        filename=payload.filename,
        content_type=payload.content_type,
        page_count=len(pages),
        total_chunks=len(chunks),
        chunks=chunks,
    )
