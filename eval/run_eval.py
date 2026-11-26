import json
import sys
from pathlib import Path
import numpy as np

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT / "ai-service"))

from app.embeddings import embed_texts
from app.extraction import evaluate_compliance_checklist, extract_structured_fields
from app.rag import generate_grounded_answer
from app.schemas import (
    ChecklistCriterionInput,
    ComplianceEvaluateRequest,
    DocumentChunkInput,
    FieldExtractRequest,
    RagAnswerRequest,
    RagContextChunk,
)


def main() -> int:
    dataset = json.loads((REPO_ROOT / "eval" / "dataset.json").read_text(encoding="utf-8"))

    all_chunks = []
    for doc in dataset["documents"]:
        for ch in doc["chunks"]:
            all_chunks.append({**ch, "document_id": doc["document_id"], "document_filename": doc["filename"]})

    chunk_vecs = np.array(embed_texts([c["content"] for c in all_chunks], is_query=False), dtype=np.float32)

    # 1. Evaluate Grounded RAG & Out-of-Scope Refusal
    rag_passed = 0
    for case in dataset["rag_cases"]:
        q_vec = np.array(embed_texts([case["question"]], is_query=True)[0], dtype=np.float32)
        scores = chunk_vecs @ q_vec
        ranked = sorted(zip(all_chunks, scores.tolist()), key=lambda x: x[1], reverse=True)

        ctx = [
            RagContextChunk(
                chunk_id=c["chunk_id"],
                document_id=c["document_id"],
                document_filename=c["document_filename"],
                chunk_index=c["chunk_index"],
                page_number=c["page_number"],
                page_start=c["page_start"],
                page_end=c["page_end"],
                content=c["content"],
                similarity_score=score,
            )
            for c, score in ranked[:3]
        ]
        res = generate_grounded_answer(RagAnswerRequest(question=case["question"], chunks=ctx))

        if res.found_answer != case["expected_found"]:
            print(f"[FAIL] {case['id']}: expected found_answer={case['expected_found']}, got {res.found_answer}")
            continue
        if case["expected_found"]:
            cited_docs = {c.document_filename for c in res.citations}
            if case["expected_doc"] not in cited_docs or case["expected_substring"] not in res.answer:
                print(f"[FAIL] {case['id']}: missing expected doc/substring in answer: {res.answer}")
                continue
        rag_passed += 1

    # 2. Evaluate Structured Field Extraction
    ext_total = 0
    ext_passed = 0
    for case in dataset["extraction_cases"]:
        doc = next(d for d in dataset["documents"] if d["filename"] == case["document_filename"])
        chunks_in = [DocumentChunkInput(**c) for c in doc["chunks"]]
        ext_res = extract_structured_fields(
            FieldExtractRequest(schema_name="enterprise_contract_v1", document_filename=doc["filename"], chunks=chunks_in)
        )
        by_name = {f.field_name: f.field_value for f in ext_res.fields}
        for k, expected_v in case["expected_fields"].items():
            ext_total += 1
            if by_name.get(k) == expected_v:
                ext_passed += 1
            else:
                print(f"[FAIL] extraction {k}: expected {expected_v!r}, got {by_name.get(k)!r}")

    # 3. Evaluate Compliance Checklist Accuracy
    comp_total = 0
    comp_passed = 0
    for case in dataset["compliance_cases"]:
        doc = next(d for d in dataset["documents"] if d["filename"] == case["document_filename"])
        chunks_in = [DocumentChunkInput(**c) for c in doc["chunks"]]
        criteria = [
            ChecklistCriterionInput(item_id=c["item_id"], criterion=c["criterion"], description=c["description"])
            for c in case["criteria"]
        ]
        comp_res = evaluate_compliance_checklist(
            ComplianceEvaluateRequest(document_filename=doc["filename"], criteria=criteria, chunks=chunks_in)
        )
        by_id = {r.checklist_item_id: r.outcome for r in comp_res.results}
        for c in case["criteria"]:
            comp_total += 1
            if by_id.get(c["item_id"]) == c["expected_outcome"]:
                comp_passed += 1
            else:
                print(f"[FAIL] compliance {c['criterion']}: expected {c['expected_outcome']}, got {by_id.get(c['item_id'])}")

    rag_total = len(dataset["rag_cases"])
    print("\n=== DocuMind Evaluation Scorecard ===")
    print(f"Grounded RAG & Refusal : {rag_passed}/{rag_total} ({rag_passed / rag_total:.0%})")
    print(f"Structured Extraction  : {ext_passed}/{ext_total} ({ext_passed / ext_total:.0%})")
    print(f"Compliance Auditing    : {comp_passed}/{comp_total} ({comp_passed / comp_total:.0%})")

    return 0 if (rag_passed == rag_total and ext_passed == ext_total and comp_passed == comp_total) else 1


if __name__ == "__main__":
    raise SystemExit(main())
