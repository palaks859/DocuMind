from functools import lru_cache
from typing import List
from fastembed import TextEmbedding
from app.config import settings


@lru_cache(maxsize=1)
def get_embedding_model() -> TextEmbedding:
    return TextEmbedding(model_name=settings.embedding_model_name)


def embed_texts(texts: List[str], is_query: bool = False) -> List[List[float]]:
    if not texts:
        return []
    model = get_embedding_model()
    if is_query:
        # BGE retrieval instruction prefix improves asymmetric query-to-passage cosine retrieval
        prefixed = [f"Represent this sentence for searching relevant passages: {t}" for t in texts]
        vectors = list(model.embed(prefixed))
    else:
        vectors = list(model.embed(texts))
    return [vec.tolist() for vec in vectors]
