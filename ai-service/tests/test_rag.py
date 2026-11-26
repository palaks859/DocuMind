from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_rag_answers_with_citations_when_evidence_matches():
    res = client.post(
        "/api/v1/rag/answer",
        json={
            "question": "What is the monthly uptime guarantee?",
            "chunks": [
                {
                    "chunk_id": "11111111-1111-1111-1111-111111111111",
                    "document_id": "22222222-2222-2222-2222-222222222222",
                    "document_filename": "enterprise-sla.txt",
                    "chunk_index": 0,
                    "page_number": 1,
                    "page_start": 1,
                    "page_end": 1,
                    "content": "Enterprise SLA Agreement: Uptime guarantee is 99.95% monthly. Support response time is 1 hour.",
                    "similarity_score": 0.84,
                }
            ],
        },
    )
    assert res.status_code == 200
    body = res.json()
    assert body["found_answer"] is True
    assert "99.95%" in body["answer"]
    assert "[S1]" in body["answer"]
    assert len(body["citations"]) == 1
    assert body["citations"][0]["document_filename"] == "enterprise-sla.txt"


def test_rag_refuses_when_no_relevant_evidence():
    res = client.post(
        "/api/v1/rag/answer",
        json={
            "question": "What is the company stock ticker symbol?",
            "chunks": [
                {
                    "chunk_id": "11111111-1111-1111-1111-111111111111",
                    "document_id": "22222222-2222-2222-2222-222222222222",
                    "document_filename": "enterprise-sla.txt",
                    "chunk_index": 0,
                    "page_number": 1,
                    "content": "Enterprise SLA Agreement: Uptime guarantee is 99.95% monthly.",
                    "similarity_score": 0.31,
                }
            ],
        },
    )
    assert res.status_code == 200
    body = res.json()
    assert body["found_answer"] is False
    assert body["citations"] == []
