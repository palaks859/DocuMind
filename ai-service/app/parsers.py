from io import BytesIO
from typing import List
import pymupdf
from docx import Document as DocxDocument
from app.schemas import PageContent


SUPPORTED_MIME_TYPES = {
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "text/plain",
}


def parse_document_bytes(file_bytes: bytes, content_type: str, filename: str = "") -> List[PageContent]:
    normalized_type = (content_type or "").split(";")[0].strip().lower()
    lower_name = filename.lower()

    if normalized_type == "application/pdf" or lower_name.endswith(".pdf"):
        return _parse_pdf(file_bytes)
    if (
        normalized_type == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        or lower_name.endswith(".docx")
    ):
        return _parse_docx(file_bytes)
    if normalized_type == "text/plain" or lower_name.endswith(".txt"):
        return _parse_txt(file_bytes)

    raise ValueError(f"Unsupported document type: {content_type} ({filename})")


def _parse_pdf(file_bytes: bytes) -> List[PageContent]:
    pages: List[PageContent] = []
    with pymupdf.open(stream=file_bytes, filetype="pdf") as doc:
        if doc.page_count == 0:
            raise ValueError("PDF file contains 0 pages.")
        for idx, page in enumerate(doc, start=1):
            text = page.get_text("text").strip()
            pages.append(PageContent(page_number=idx, text=text))
    return pages


def _parse_docx(file_bytes: bytes) -> List[PageContent]:
    doc = DocxDocument(BytesIO(file_bytes))
    blocks: List[str] = []

    for para in doc.paragraphs:
        cleaned = para.text.strip()
        if cleaned:
            blocks.append(cleaned)

    for table in doc.tables:
        rows_md: List[str] = []
        for row in table.rows:
            cells = [cell.text.strip().replace("\n", " ") for cell in row.cells]
            if any(cells):
                rows_md.append("| " + " | ".join(cells) + " |")
        if rows_md:
            blocks.append("\n".join(rows_md))

    full_text = "\n\n".join(blocks).strip()
    if not full_text:
        raise ValueError("DOCX document contains no extractable text.")

    # Approximate ~3000 chars per logical page for DOCX files
    approx_page_chars = 3000
    if len(full_text) <= approx_page_chars:
        return [PageContent(page_number=1, text=full_text)]

    pages: List[PageContent] = []
    start = 0
    page_num = 1
    while start < len(full_text):
        end = min(start + approx_page_chars, len(full_text))
        if end < len(full_text):
            split_at = full_text.rfind("\n\n", start, end)
            if split_at > start + 500:
                end = split_at
        segment = full_text[start:end].strip()
        if segment:
            pages.append(PageContent(page_number=page_num, text=segment))
            page_num += 1
        start = end

    return pages or [PageContent(page_number=1, text=full_text)]


def _parse_txt(file_bytes: bytes) -> List[PageContent]:
    text = file_bytes.decode("utf-8", errors="replace").strip()
    if not text:
        raise ValueError("Text file is empty.")
    return [PageContent(page_number=1, text=text)]
