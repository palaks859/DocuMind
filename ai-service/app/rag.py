import json
import re
from typing import List
import httpx
from app.config import settings
from app.schemas import RagAnswerRequest, RagAnswerResponse, RagCitationOut, RagContextChunk


REFUSAL_MESSAGE = (
    "I could not find sufficient verified evidence in the workspace documents to answer this question."
)

SYSTEM_PROMPT = """You are DocuMind, an enterprise document intelligence assistant.
Answer the user's question STRICTLY using ONLY the numbered source excerpts provided below.
Rules:
1. Do NOT use outside knowledge, assumptions, or extrapolation.
2. If the provided sources do not contain enough evidence to answer the question completely and accurately, set "found_answer" to false and explain clearly what information is missing from the workspace documents.
3. Every factual claim in your answer must cite its source marker like [S1] or [S2].
4. Return ONLY valid JSON matching this schema:
{
  "found_answer": true,
  "answer": "Concise grounded answer with [S1] citations.",
  "citations": [
    {
      "source_index": 1,
      "quoted_text": "Exact verbatim quote from source S1 supporting the answer."
    }
  ]
}"""


def _format_passage_ref(chunk: RagContextChunk) -> str:
    if chunk.page_start and chunk.page_end and chunk.page_start != chunk.page_end:
        return f"{chunk.document_filename} (Pages {chunk.page_start}-{chunk.page_end}, Chunk #{chunk.chunk_index + 1})"
    page = chunk.page_number or chunk.page_start or 1
    return f"{chunk.document_filename} (Page {page}, Chunk #{chunk.chunk_index + 1})"


def _build_citation(chunk: RagContextChunk, quote: str) -> RagCitationOut:
    clean_quote = (quote or chunk.content).strip()
    if len(clean_quote) > 320:
        clean_quote = clean_quote[:317].rstrip() + "..."
    score = max(0.0, min(1.0, round(chunk.similarity_score, 5)))
    return RagCitationOut(
        chunk_id=chunk.chunk_id,
        document_id=chunk.document_id,
        document_filename=chunk.document_filename,
        page_number=chunk.page_number,
        page_start=chunk.page_start,
        page_end=chunk.page_end,
        passage_reference=_format_passage_ref(chunk),
        quoted_text=clean_quote,
        relevance_score=score,
    )


def _extractive_grounded_answer(question: str, relevant: List[RagContextChunk]) -> RagAnswerResponse:
    q_terms = {
        w.lower()
        for w in re.findall(r"[a-zA-Z0-9]{3,}", question)
        if w.lower() not in {"what", "when", "where", "which", "does", "have", "from", "with", "that", "this", "the", "and", "for", "are"}
    }

    selected_chunks = relevant[:3]
    answer_lines: List[str] = []
    citations: List[RagCitationOut] = []

    for idx, chunk in enumerate(selected_chunks, start=1):
        sentences = [s.strip() for s in re.split(r"(?<=[.!?])\s+", chunk.content.strip()) if s.strip()]
        best_sentence = sentences[0] if sentences else chunk.content.strip()
        if q_terms and len(sentences) > 1:
            best_sentence = max(
                sentences,
                key=lambda s: sum(1 for t in q_terms if t in s.lower()),
            )
        answer_lines.append(f"{best_sentence} [S{idx}]")
        citations.append(_build_citation(chunk, best_sentence))

    return RagAnswerResponse(
        answer=" ".join(answer_lines),
        found_answer=True,
        prompt_version=settings.rag_prompt_version,
        model_provider="local-extractive",
        model_name="bge-small-extractive-v1",
        citations=citations,
    )


def _llm_grounded_answer(request: RagAnswerRequest, relevant: List[RagContextChunk]) -> RagAnswerResponse:
    top_chunks = relevant[:5]
    context_blocks = []
    for idx, c in enumerate(top_chunks, start=1):
        ref = _format_passage_ref(c)
        context_blocks.append(f"[S{idx}] Source: {ref}\nExcerpt:\n{c.content}")

    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    for turn in request.history[-4:]:
        role = turn.role.lower()
        if role in ("user", "assistant"):
            messages.append({"role": role, "content": turn.content})

    user_prompt = (
        f"Workspace Document Excerpts:\n\n" + "\n\n---\n\n".join(context_blocks)
        + f"\n\nQuestion: {request.question}"
    )
    messages.append({"role": "user", "content": user_prompt})

    url = f"{settings.llm_base_url.rstrip('/')}/chat/completions"
    headers = {
        "Authorization": f"Bearer {settings.llm_api_key}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": settings.llm_model,
        "temperature": 0.0,
        "response_format": {"type": "json_object"},
        "messages": messages,
    }

    with httpx.Client(timeout=30.0) as client:
        resp = client.post(url, headers=headers, json=payload)
        resp.raise_for_status()
        data = resp.json()

    raw_content = data["choices"][0]["message"]["content"]
    parsed = json.loads(raw_content)
    found = bool(parsed.get("found_answer", False))
    answer_text = str(parsed.get("answer") or REFUSAL_MESSAGE).strip()

    if not found:
        return RagAnswerResponse(
            answer=answer_text or REFUSAL_MESSAGE,
            found_answer=False,
            prompt_version=settings.rag_prompt_version,
            model_provider="openai-compatible",
            model_name=settings.llm_model,
            citations=[],
        )

    citations: List[RagCitationOut] = []
    seen_indices = set()
    for item in parsed.get("citations", []):
        src_idx = int(item.get("source_index", 0))
        if 1 <= src_idx <= len(top_chunks) and src_idx not in seen_indices:
            seen_indices.add(src_idx)
            citations.append(_build_citation(top_chunks[src_idx - 1], item.get("quoted_text", "")))

    if not citations:
        citations.append(_build_citation(top_chunks[0], top_chunks[0].content))

    return RagAnswerResponse(
        answer=answer_text,
        found_answer=True,
        prompt_version=settings.rag_prompt_version,
        model_provider="openai-compatible",
        model_name=settings.llm_model,
        citations=citations,
    )


def generate_grounded_answer(request: RagAnswerRequest) -> RagAnswerResponse:
    threshold = request.min_similarity if request.min_similarity is not None else settings.rag_min_similarity
    relevant = [c for c in request.chunks if c.similarity_score >= threshold]

    if not relevant:
        return RagAnswerResponse(
            answer=REFUSAL_MESSAGE,
            found_answer=False,
            prompt_version=settings.rag_prompt_version,
            model_provider="openai-compatible" if settings.llm_api_key else "local-extractive",
            model_name=settings.llm_model if settings.llm_api_key else "bge-small-extractive-v1",
            citations=[],
        )

    if settings.llm_api_key and settings.llm_api_key.strip():
        return _llm_grounded_answer(request, relevant)

    return _extractive_grounded_answer(request.question, relevant)
