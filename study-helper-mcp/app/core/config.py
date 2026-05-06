from __future__ import annotations

from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    openai_api_key: str | None = Field(default=None, alias="OPENAI_API_KEY")
    openai_model: str = Field(default="gpt-4.1-mini", alias="OPENAI_MODEL")
    openai_timeout_s: int = Field(default=30, alias="OPENAI_TIMEOUT_S")
    openai_max_retries: int = Field(default=3, alias="OPENAI_MAX_RETRIES")

    log_level: str = Field(default="INFO", alias="LOG_LEVEL")
    storage_path: str = Field(default="./data/storage.json", alias="STORAGE_PATH")

    @property
    def storage_path_obj(self) -> Path:
        return Path(self.storage_path).expanduser().resolve()


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
