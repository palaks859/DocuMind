import bisect
import math
from typing import List, Tuple
from app.schemas import DocumentChunkOut, PageContent


def estimate_token_count(text: str) -> int:
    if not text:
        return 0
    return max(1, math.ceil(len(text) / 4))


def chunk_pages(
    pages: List[PageContent],
    chunk_size: int = 1000,
    chunk_overlap: int = 150,
) -> List[DocumentChunkOut]:
    if chunk_overlap >= chunk_size:
        raise ValueError("chunk_overlap must be strictly less than chunk_size.")

    non_empty_pages = [p for p in pages if p.text and p.text.strip()]
    if not non_empty_pages:
        return []

    # Build unified document text while tracking page character spans
    combined_parts: List[str] = []
    page_spans: List[Tuple[int, int, int]] = []  # (start_offset, end_offset, page_number)
    cursor = 0

    for i, page in enumerate(non_empty_pages):
        if i > 0:
            combined_parts.append("\n\n")
            cursor += 2
        start = cursor
        combined_parts.append(page.text)
        cursor += len(page.text)
        page_spans.append((start, cursor, page.page_number))

    full_text = "".join(combined_parts)
    total_len = len(full_text)
    span_starts = [s[0] for s in page_spans]

    def resolve_pages(start_off: int, end_off: int) -> Tuple[int, int]:
        first_idx = max(0, bisect.bisect_right(span_starts, start_off) - 1)
        last_idx = max(0, bisect.bisect_right(span_starts, max(start_off, end_off - 1)) - 1)
        return page_spans[first_idx][2], page_spans[last_idx][2]

    chunks: List[DocumentChunkOut] = []
    pos = 0
    chunk_idx = 0

    while pos < total_len:
        raw_end = min(pos + chunk_size, total_len)
        end = raw_end

        # Prefer breaking on paragraph, sentence, or word boundary near the end of window
        if end < total_len:
            min_break = pos + int(chunk_size * 0.6)
            for sep in ("\n\n", "\n", ". ", " "):
                candidate = full_text.rfind(sep, min_break, end)
                if candidate != -1:
                    end = candidate + len(sep)
                    break

        slice_text = full_text[pos:end].strip()
        if slice_text:
            # Trim leading/trailing whitespace offsets accurately
            leading_ws = len(full_text[pos:end]) - len(full_text[pos:end].lstrip())
            actual_start = pos + leading_ws
            actual_end = actual_start + len(slice_text)
            page_start, page_end = resolve_pages(actual_start, actual_end)

            chunks.append(
                DocumentChunkOut(
                    chunk_index=chunk_idx,
                    content=slice_text,
                    token_count=estimate_token_count(slice_text),
                    page_number=page_start,
                    page_start=page_start,
                    page_end=page_end,
                    start_offset=actual_start,
                    end_offset=actual_end,
                )
            )
            chunk_idx += 1

        if end >= total_len:
            break

        next_pos = max(pos + 1, end - chunk_overlap)
        pos = next_pos

    return chunks
