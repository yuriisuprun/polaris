from __future__ import annotations

import asyncio
import json
import logging
import time
from functools import lru_cache
from hashlib import sha256
from typing import Any

from groq import AsyncGroq, RateLimitError

from app.core.config import get_settings

logger = logging.getLogger(__name__)

_request_lock = asyncio.Lock()
_last_request_time = 0.0


class LLMError(RuntimeError):
    pass


class LLMClient:
    def __init__(self, api_key: str, model: str, timeout_s: int, rate_limit_min_interval_s: float = 20.0, rate_limit_max_retries: int = 5) -> None:
        self._client = AsyncGroq(api_key=api_key, timeout=timeout_s, max_retries=0)
        self._model = model
        self._cache: dict[str, tuple[float, dict[str, Any]]] = {}
        self._cache_ttl_s = 300.0
        self._cache_max_items = 128
        self._max_retries = rate_limit_max_retries
        self._initial_retry_delay_s = 2.0
        self._min_request_interval_s = rate_limit_min_interval_s

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
            oldest_key = min(self._cache.items(), key=lambda kv: kv[1][0])[0]
            self._cache.pop(oldest_key, None)
        self._cache[key] = (now + self._cache_ttl_s, payload)

    async def generate_structured(self, *, system_prompt: str, user_prompt: str, json_schema: dict[str, Any]) -> dict[str, Any]:
        global _last_request_time

        cache_key = sha256(
            (self._model + "\n" + system_prompt + "\n" + user_prompt + "\n" + json.dumps(json_schema, sort_keys=True)).encode("utf-8")
        ).hexdigest()
        cached = self._cache_get(cache_key)
        if cached is not None:
            logger.debug("Cache hit for LLM request", extra={"cache_key": cache_key[:8]})
            return cached

        async with _request_lock:
            now = time.time()
            time_since_last = now - _last_request_time
            if time_since_last < self._min_request_interval_s:
                wait_time = self._min_request_interval_s - time_since_last
                logger.debug("Rate limit throttle", extra={"wait_time_s": wait_time})
                await asyncio.sleep(wait_time)
            _last_request_time = time.time()

        schema_instruction = (
            "\n\nYou MUST respond with valid JSON matching this exact schema:\n"
            f"{json.dumps(json_schema, indent=2)}\n"
            "Do not include any text before or after the JSON object."
        )
        enhanced_system_prompt = system_prompt + schema_instruction

        for attempt in range(self._max_retries):
            try:
                resp = await self._client.chat.completions.create(
                    model=self._model,
                    messages=[
                        {"role": "system", "content": enhanced_system_prompt},
                        {"role": "user", "content": user_prompt},
                    ],
                    response_format={"type": "json_object"},
                )

                if not resp.choices:
                    raise LLMError("LLM response contained no choices")

                text = resp.choices[0].message.content
                if not text or not isinstance(text, str):
                    raise LLMError("LLM response contained no content")

                try:
                    parsed = json.loads(text)
                except Exception as e:  # noqa: BLE001
                    logger.warning("Failed to parse JSON from LLM", extra={"content": text})
                    raise LLMError("LLM returned invalid JSON") from e

                if not isinstance(parsed, dict):
                    raise LLMError("LLM JSON output must be an object")

                self._cache_set(cache_key, parsed)
                logger.debug("LLM request successful", extra={"attempt": attempt + 1})
                return parsed

            except RateLimitError as e:
                if attempt < self._max_retries - 1:
                    retry_delay = self._initial_retry_delay_s * (2**attempt)
                    logger.warning(
                        "Groq rate limited (429). Retrying",
                        extra={"attempt": attempt + 1, "max_retries": self._max_retries, "retry_delay_s": retry_delay, "error": str(e)},
                    )
                    await asyncio.sleep(retry_delay)
                    continue
                raise LLMError(f"Groq rate limit persisted after {self._max_retries} retries") from e
            except LLMError:
                raise
            except Exception as e:  # noqa: BLE001
                raise LLMError(f"LLM client error: {e}") from e

        raise LLMError("LLM client exhausted all retries without success")


@lru_cache(maxsize=1)
def get_llm() -> LLMClient:
    settings = get_settings()
    if not settings.groq_api_key:
        raise LLMError("No valid GROQ_API_KEY configured")
    return LLMClient(
        api_key=settings.groq_api_key,
        model=settings.groq_model,
        timeout_s=settings.groq_timeout_s,
        rate_limit_min_interval_s=settings.rate_limit_min_interval_s,
        rate_limit_max_retries=settings.groq_max_retries,
    )
