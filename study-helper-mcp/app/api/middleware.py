from __future__ import annotations

import logging
import time
import uuid
from typing import Callable

from fastapi import Request, Response

from app.core.context import set_request_id

logger = logging.getLogger("app.http")


def _safe_request_log_fields(request: Request) -> dict[str, object]:
    ua = request.headers.get("user-agent")
    return {
        "method": request.method,
        "path": str(request.url.path),
        "query": str(request.url.query) if request.url.query else None,
        "client_ip": request.client.host if request.client else None,
        "user_agent": ua[:200] if ua else None,
    }


async def request_context_middleware(request: Request, call_next: Callable[[Request], Response]) -> Response:
    request_id = request.headers.get("x-request-id") or str(uuid.uuid4())
    set_request_id(request_id)

    start = time.perf_counter()
    response = await call_next(request)
    duration_ms = int((time.perf_counter() - start) * 1000)

    response.headers["X-Request-ID"] = request_id
    response.headers["X-Response-Time-Ms"] = str(duration_ms)

    logger.info(
        "request_complete",
        extra={
            **_safe_request_log_fields(request),
            "status_code": response.status_code,
            "duration_ms": duration_ms,
            "request_id": request_id,
        },
    )
    return response

