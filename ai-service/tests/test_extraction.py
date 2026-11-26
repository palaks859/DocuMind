from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_structured_field_extraction_finds_sla_and_governing_law():
    res = client.post(
        "/api/v1/extract/fields",
        json={
            "schema_name": "enterprise_contract_v1",
            "document_filename": "msa.pdf",
            "chunks": [
                {
                    "chunk_id": "11111111-1111-1111-1111-111111111111",
                    "chunk_index": 0,
                    "page_number": 1,
                    "page_start": 1,
                    "page_end": 1,
                    "content": "Master Services Agreement. Governing Law is Delaware. Uptime guarantee is 99.95% monthly.",
                }
            ],
        },
    )
    assert res.status_code == 200
    body = res.json()
    by_name = {f["field_name"]: f for f in body["fields"]}
    assert by_name["governing_law"]["field_value"] == "Delaware"
    assert by_name["governing_law"]["confidence"] > 0.8
    assert by_name["uptime_sla"]["field_value"] == "99.95%"


def test_compliance_checklist_evaluates_pass_and_unclear():
    res = client.post(
        "/api/v1/compliance/evaluate",
        json={
            "document_filename": "enterprise-sla.txt",
            "criteria": [
                {
                    "item_id": "aaaa1111-1111-1111-1111-111111111111",
                    "criterion": "Uptime SLA Guarantee",
                    "description": "Agreement specifies a monthly uptime SLA percentage",
                },
                {
                    "item_id": "bbbb2222-2222-2222-2222-222222222222",
                    "criterion": "HIPAA Business Associate Addendum",
                    "description": "Includes HIPAA protected health information safeguards",
                },
            ],
            "chunks": [
                {
                    "chunk_id": "11111111-1111-1111-1111-111111111111",
                    "chunk_index": 0,
                    "page_number": 1,
                    "content": "Enterprise SLA Agreement: Uptime guarantee is 99.95% monthly.",
                }
            ],
        },
    )
    assert res.status_code == 200
    by_id = {r["checklist_item_id"]: r for r in res.json()["results"]}
    assert by_id["aaaa1111-1111-1111-1111-111111111111"]["outcome"] == "PASS"
    assert by_id["bbbb2222-2222-2222-2222-222222222222"]["outcome"] == "UNCLEAR"
