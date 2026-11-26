import json
import re
from typing import Any, List, Optional, Tuple
import httpx
import numpy as np
from app.config import settings
from app.embeddings import embed_texts
from app.schemas import (
    ComplianceEvaluateRequest,
    ComplianceEvaluateResponse,
    ComplianceItemResultOut,
    DocumentChunkInput,
    ExtractedFieldOut,
    FieldExtractRequest,
    FieldExtractResponse,
    FieldSpecInput,
)

DEFAULT_CONTRACT_FIELDS: List[FieldSpecInput] = [
    FieldSpecInput(field_name="governing_law", description="Jurisdiction or state whose laws govern the agreement"),
    FieldSpecInput(field_name="term_duration", description="Contract term length or duration"),
    FieldSpecInput(field_name="uptime_sla", description="Service availability or monthly uptime percentage"),
    FieldSpecInput(field_name="liability_cap", description="Limitation of liability or indemnification monetary cap"),
    FieldSpecInput(field_name="reimbursement_amount", description="Stipend or reimbursement monetary amount"),
]

_FIELD_PATTERNS = {
    "governing_law": [
        r"(?:Governing Law|governed by the laws of)\s*(?:is|shall be)?\s*(?:the\s+State\s+of\s+)?([A-Z][A-Za-z\s]{2,30})(?:[.,;]|$)",
    ],
    "term_duration": [
        r"(?:Term\s+is|term\s+of)\s+(\d+\s+(?:months|years|days))",
    ],
    "uptime_sla": [
        r"(?:Uptime\s+guarantee\s+is|availability\s+of)\s+(\d{2,3}(?:\.\d+)?%)",
    ],
    "liability_cap": [
        r"(?:capped\s+at|indemnifies\s+[^.]+?\s+up\s+to)\s+([^.]+)",
    ],
    "reimbursement_amount": [
        r"(\$?\d[\d,]*(?:\.\d{2})?\s+(?:home\s+office\s+equipment\s+)?reimbursement)",
    ],
}


def _best_chunk_for_query(
    query: str,
    chunks: List[DocumentChunkInput],
    chunk_matrix: np.ndarray,
) -> Tuple[DocumentChunkInput, float]:
    q_vec = np.array(embed_texts([query], is_query=True)[0], dtype=np.float32)
    scores = chunk_matrix @ q_vec
    best_idx = int(np.argmax(scores))
    return chunks[best_idx], float(scores[best_idx])


def _extract_value_heuristic(field_name: str, description: Optional[str], text: str) -> Tuple[Any, Optional[str], float]:
    norm_key = field_name.strip().lower().replace(" ", "_")
    for pat in _FIELD_PATTERNS.get(norm_key, []):
        m = re.search(pat, text, flags=re.IGNORECASE)
        if m:
            val = m.group(1).strip()
            return val, m.group(0).strip(), 0.91

    keywords = {
        w.lower()
        for w in re.findall(r"[a-zA-Z0-9]{3,}", f"{field_name} {description or ''}")
        if w.lower() not in {"the", "and", "for", "with", "that", "from", "amount", "value"}
    }
    sentences = [s.strip() for s in re.split(r"(?<=[.!?])\s+", text.strip()) if s.strip()]
    if not sentences:
        return None, None, 0.0

    best_sentence = max(
        sentences,
        key=lambda s: sum(1 for kw in keywords if kw in s.lower()),
    )
    hits = sum(1 for kw in keywords if kw in best_sentence.lower())
    if hits == 0:
        return None, None, 0.25

    money_or_pct = re.search(r"(\$\d[\d,]*(?:\.\d+)?|\d+(?:\.\d+)?%|\d+\s+(?:months|years|days))", best_sentence)
    extracted = money_or_pct.group(1) if money_or_pct else best_sentence
    conf = min(0.88, round(0.62 + 0.08 * hits, 4))
    return extracted, best_sentence, conf


def _extract_with_llm(request: FieldExtractRequest, specs: List[FieldSpecInput]) -> FieldExtractResponse:
    chunk_blocks = [
        f"[C{idx}] (chunk_id={c.chunk_id}, page={c.page_start or c.page_number or 1}):\n{c.content}"
        for idx, c in enumerate(request.chunks[:12], start=1)
    ]
    field_lines = [f"- {f.field_name}: {f.description or f.field_name}" for f in specs]

    system_prompt = (
        "You are DocuMind's structured extraction engine. Extract ONLY values explicitly stated in the provided chunks. "
        "If a field is not present, return null for field_value and confidence 0.0. "
        "Return ONLY valid JSON: {\"fields\": [{\"field_name\": \"...\", \"field_value\": ..., \"chunk_index_ref\": 1, \"evidence\": \"verbatim quote\", \"confidence\": 0.95}]}"
    )
    user_prompt = (
        f"Document: {request.document_filename}\nSchema: {request.schema_name}\n\n"
        f"Target Fields:\n" + "\n".join(field_lines)
        + "\n\nChunks:\n\n" + "\n\n---\n\n".join(chunk_blocks)
    )

    url = f"{settings.llm_base_url.rstrip('/')}/chat/completions"
    headers = {"Authorization": f"Bearer {settings.llm_api_key}", "Content-Type": "application/json"}
    payload = {
        "model": settings.llm_model,
        "temperature": 0.0,
        "response_format": {"type": "json_object"},
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
    }
    with httpx.Client(timeout=30.0) as client:
        resp = client.post(url, headers=headers, json=payload)
        resp.raise_for_status()
        data = resp.json()

    parsed = json.loads(data["choices"][0]["message"]["content"])
    by_name = {item.get("field_name"): item for item in parsed.get("fields", [])}
    out_fields: List[ExtractedFieldOut] = []
    for spec in specs:
        item = by_name.get(spec.field_name, {})
        c_ref = int(item.get("chunk_index_ref") or 1)
        src_chunk = request.chunks[min(max(1, c_ref), len(request.chunks)) - 1]
        val = item.get("field_value")
        conf = float(item.get("confidence") or (0.85 if val is not None else 0.0))
        out_fields.append(
            ExtractedFieldOut(
                field_name=spec.field_name,
                field_value=val,
                source_chunk_id=src_chunk.chunk_id if val is not None else None,
                evidence=item.get("evidence"),
                page_number=src_chunk.page_number if val is not None else None,
                page_start=src_chunk.page_start if val is not None else None,
                page_end=src_chunk.page_end if val is not None else None,
                confidence=max(0.0, min(1.0, round(conf, 4))),
            )
        )

    return FieldExtractResponse(
        schema_name=request.schema_name,
        prompt_version="extract-v1.0",
        model_provider="openai-compatible",
        model_name=settings.llm_model,
        fields=out_fields,
    )


def extract_structured_fields(request: FieldExtractRequest) -> FieldExtractResponse:
    specs = request.fields if request.fields else DEFAULT_CONTRACT_FIELDS
    if settings.llm_api_key and settings.llm_api_key.strip():
        return _extract_with_llm(request, specs)

    chunk_vecs = np.array(embed_texts([c.content for c in request.chunks], is_query=False), dtype=np.float32)
    out_fields: List[ExtractedFieldOut] = []

    for spec in specs:
        query_text = f"{spec.field_name}: {spec.description or spec.field_name}"
        best_chunk, sim = _best_chunk_for_query(query_text, request.chunks, chunk_vecs)
        val, evidence, conf = _extract_value_heuristic(spec.field_name, spec.description, best_chunk.content)
        if val is None or sim < 0.45:
            out_fields.append(
                ExtractedFieldOut(
                    field_name=spec.field_name,
                    field_value=None,
                    source_chunk_id=None,
                    evidence=None,
                    page_number=None,
                    page_start=None,
                    page_end=None,
                    confidence=0.0,
                )
            )
        else:
            out_fields.append(
                ExtractedFieldOut(
                    field_name=spec.field_name,
                    field_value=val,
                    source_chunk_id=best_chunk.chunk_id,
                    evidence=evidence,
                    page_number=best_chunk.page_number,
                    page_start=best_chunk.page_start,
                    page_end=best_chunk.page_end,
                    confidence=max(0.0, min(1.0, round(conf, 4))),
                )
            )

    return FieldExtractResponse(
        schema_name=request.schema_name,
        prompt_version="extract-v1.0",
        model_provider="local-extractive",
        model_name="bge-small-extractor-v1",
        fields=out_fields,
    )


def evaluate_compliance_checklist(request: ComplianceEvaluateRequest) -> ComplianceEvaluateResponse:
    chunk_vecs = np.array(embed_texts([c.content for c in request.chunks], is_query=False), dtype=np.float32)
    results: List[ComplianceItemResultOut] = []

    for item in request.criteria:
        query = f"{item.criterion}. {item.description or ''}".strip()
        best_chunk, sim = _best_chunk_for_query(query, request.chunks, chunk_vecs)

        if sim < 0.64:
            results.append(
                ComplianceItemResultOut(
                    checklist_item_id=item.item_id,
                    source_chunk_id=None,
                    outcome="UNCLEAR",
                    evidence=None,
                    explanation=f"No sufficiently relevant clause found in {request.document_filename} for '{item.criterion}' (top similarity {sim:.2f}).",
                )
            )
            continue

        lower_content = best_chunk.content.lower()
        negative_markers = ("prohibited", "not permitted", "no indemnification", "no warranty", "excluded", "does not provide")
        if any(neg in lower_content for neg in negative_markers) and "not" not in item.criterion.lower():
            outcome = "FAIL"
            explanation = f"Document contains restrictive or exclusionary language conflicting with '{item.criterion}'."
        else:
            outcome = "PASS"
            explanation = f"Verified supporting clause in {request.document_filename} (semantic match {sim:.2f})."

        quote = best_chunk.content.strip()
        if len(quote) > 320:
            quote = quote[:317].rstrip() + "..."

        results.append(
            ComplianceItemResultOut(
                checklist_item_id=item.item_id,
                source_chunk_id=best_chunk.chunk_id,
                outcome=outcome,
                evidence=quote,
                explanation=explanation,
            )
        )

    return ComplianceEvaluateResponse(
        prompt_version="compliance-v1.0",
        model_provider="local-extractive",
        model_name="bge-small-compliance-v1",
        results=results,
    )
