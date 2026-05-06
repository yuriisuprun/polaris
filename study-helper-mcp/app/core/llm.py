from __future__ import annotations

import json
import logging
from functools import lru_cache
from typing import Any

from openai import OpenAI
from openai.types.responses import Response
from tenacity import RetryError, retry, stop_after_attempt, wait_exponential_jitter

from app.core.config import get_settings

logger = logging.getLogger(__name__)


class LLMError(RuntimeError):
    pass


class LLMClient:
    def __init__(self, api_key: str, model: str, timeout_s: int, max_retries: int) -> None:
        self._client = OpenAI(api_key=api_key, timeout=timeout_s)
        self._model = model
        self._max_retries = max_retries

    @retry(wait=wait_exponential_jitter(initial=1, max=8), stop=stop_after_attempt(3), reraise=True)
    def _responses_create(self, **kwargs: Any) -> Response:
        return self._client.responses.create(**kwargs)

    def generate_structured(
        self,
        *,
        system_prompt: str,
        user_prompt: str,
        json_schema: dict[str, Any],
    ) -> dict[str, Any]:
        """
        Enforces structured JSON output using OpenAI Responses JSON schema output.
        Raises LLMError if the model output cannot be parsed or validated by the API.
        """
        try:
            resp = self._responses_create(
                model=self._model,
                input=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                response_format={
                    "type": "json_schema",
                    "json_schema": {
                        "name": "study_helper_output",
                        "schema": json_schema,
                        "strict": True,
                    },
                },
            )
        except RetryError as e:
            raise LLMError("LLM request failed after retries") from e
        except Exception as e:  # noqa: BLE001 (boundary)
            raise LLMError("LLM request failed") from e

        text = getattr(resp, "output_text", None)
        if not text or not isinstance(text, str):
            raise LLMError("LLM response contained no output_text")

        try:
            parsed = json.loads(text)
        except Exception as e:  # noqa: BLE001 (boundary)
            logger.warning("Failed to parse JSON from LLM", extra={"output_text": text})
            raise LLMError("LLM returned invalid JSON") from e

        if not isinstance(parsed, dict):
            raise LLMError("LLM JSON output must be an object")
        return parsed


@lru_cache(maxsize=1)
def get_llm() -> LLMClient:
    settings = get_settings()
    if not settings.openai_api_key:
        raise LLMError("OPENAI_API_KEY is not set")
    return LLMClient(
        api_key=settings.openai_api_key,
        model=settings.openai_model,
        timeout_s=settings.openai_timeout_s,
        max_retries=settings.openai_max_retries,
    )
