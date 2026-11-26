from pathlib import Path
from typing import Optional
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    service_name: str = "documind-ai-service"
    service_version: str = "0.3.0"
    local_storage_root: str = "../data/uploads"
    default_chunk_size: int = 1000
    default_chunk_overlap: int = 150
    embedding_model_name: str = "BAAI/bge-small-en-v1.5"
    embedding_model_revision: str = "v1.5"
    embedding_dimension: int = 384

    llm_api_key: Optional[str] = None
    llm_base_url: str = "https://api.openai.com/v1"
    llm_model: str = "gpt-4o-mini"
    rag_prompt_version: str = "v1.0-grounded-strict"
    rag_min_similarity: float = 0.62

    model_config = SettingsConfigDict(
        env_file="../.env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    @property
    def storage_root_path(self) -> Path:
        repo_root = Path(__file__).resolve().parents[2]
        backend_uploads = repo_root / "backend" / "data" / "uploads"
        root_uploads = repo_root / "data" / "uploads"
        if backend_uploads.exists():
            return backend_uploads.resolve()
        return root_uploads.resolve()


settings = Settings()
