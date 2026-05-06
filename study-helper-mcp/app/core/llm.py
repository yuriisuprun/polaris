from __future__ import annotations

import json
import logging
import time
from hashlib import sha256
from functools import lru_cache
from typing import Any

from openai import AsyncOpenAI
from openai.types.responses import Response
from tenacity import AsyncRetrying, RetryError, stop_after_attempt, wait_exponential_jitter

from app.core.config import get_settings

logger = logging.getLogger(__name__)


class LLMError(RuntimeError):
    pass


class LLMClient:
    def __init__(self, api_key: str, model: str, timeout_s: int, max_retries: int) -> None:
        self._client = AsyncOpenAI(api_key=api_key, timeout=timeout_s)
        self._model = model
        self._max_retries = max_retries
        self._cache: dict[str, tuple[float, dict[str, Any]]] = {}
        self._cache_ttl_s = 300.0
        self._cache_max_items = 128

    def _cache_get(self, key: str) -> dict[str, Any] | None:
        now = time.time()
        hit = self._cache.get(key)
        if not hit:
            return None
        expires_at, payload = hit
        if expires_at < now:
            self._cache.pop(key, None)
            return None
        return payload

    def _cache_set(self, key: str, payload: dict[str, Any]) -> None:
        now = time.time()
        if len(self._cache) >= self._cache_max_items:
            # Drop oldest entry (simple, deterministic).
            oldest_key = min(self._cache.items(), key=lambda kv: kv[1][0])[0]
            self._cache.pop(oldest_key, None)
        self._cache[key] = (now + self._cache_ttl_s, payload)

    async def _responses_create(self, **kwargs: Any) -> Response:
        async for attempt in AsyncRetrying(
            wait=wait_exponential_jitter(initial=1, max=8),
            stop=stop_after_attempt(self._max_retries),
            reraise=True,
        ):
            with attempt:
                return await self._client.responses.create(**kwargs)
        raise AssertionError("unreachable")

    async def generate_structured(
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
        cache_key = sha256(
            (self._model + "\n" + system_prompt + "\n" + user_prompt + "\n" + json.dumps(json_schema, sort_keys=True)).encode(
                "utf-8"
            )
        ).hexdigest()
        cached = self._cache_get(cache_key)
        if cached is not None:
            return cached

        try:
            resp = await self._responses_create(
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
        self._cache_set(cache_key, parsed)
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
