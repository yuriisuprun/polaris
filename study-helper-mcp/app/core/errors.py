from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True, slots=True)
class AppError(Exception):
    code: str
    message: str
    status_code: int = 400
    details: dict[str, Any] | None = None


class UpstreamLLMError(AppError):
    def __init__(self, message: str = "LLM request failed", *, details: dict[str, Any] | None = None) -> None:
        super().__init__(code="LLM_UPSTREAM_ERROR", message=message, status_code=502, details=details)


class InvalidLLMOutputError(AppError):
    def __init__(self, message: str = "LLM returned invalid output") -> None:
        super().__init__(code="LLM_INVALID_OUTPUT", message=message, status_code=502)


class StorageError(AppError):
    def __init__(self, message: str = "Storage error") -> None:
        super().__init__(code="STORAGE_ERROR", message=message, status_code=500)

