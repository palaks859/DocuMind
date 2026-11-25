from io import BytesIO
import pymupdf
from docx import Document as DocxDocument
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def _make_sample_pdf() -> bytes:
    doc = pymupdf.open()
    page1 = doc.new_page()
    page1.insert_text((72, 72), "Page 1: Master Services Agreement between Acme Corp and DocuMind.")
    page2 = doc.new_page()
    page2.insert_text((72, 72), "Page 2: Governing Law shall be the State of Delaware. Liability is capped at 12 months fees.")
    buf = doc.tobytes()
    doc.close()
    return buf


def _make_sample_docx() -> bytes:
    doc = DocxDocument()
    doc.add_paragraph("Vendor Security Policy")
    table = doc.add_table(rows=2, cols=2)
    table.cell(0, 0).text = "Control"
    table.cell(0, 1).text = "Status"
    table.cell(1, 0).text = "SOC2 Type II"
    table.cell(1, 1).text = "Verified"
    bio = BytesIO()
    doc.save(bio)
    return bio.getvalue()


def test_health():
    res = client.get("/health")
    assert res.status_code == 200
    assert res.json()["status"] == "UP"


def test_pdf_ingestion_preserves_page_numbers():
    pdf_bytes = _make_sample_pdf()
    res = client.post(
        "/api/v1/ingest/parse",
        files={"file": ("contract.pdf", pdf_bytes, "application/pdf")},
        data={"chunk_size": "500", "chunk_overlap": "50"},
    )
    assert res.status_code == 200
    body = res.json()
    assert body["page_count"] == 2
    assert body["total_chunks"] >= 1
    assert body["chunks"][0]["page_start"] == 1
    assert body["chunks"][-1]["page_end"] == 2


def test_docx_ingestion_extracts_paragraphs_and_tables():
    docx_bytes = _make_sample_docx()
    res = client.post(
        "/api/v1/ingest/parse",
        files={
            "file": (
                "security.docx",
                docx_bytes,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            )
        },
    )
    assert res.status_code == 200
    body = res.json()
    assert body["page_count"] == 1
    content = body["chunks"][0]["content"]
    assert "Vendor Security Policy" in content
    assert "SOC2 Type II" in content
