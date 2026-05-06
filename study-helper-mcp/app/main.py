from __future__ import annotations

import logging
import time
from typing import Callable

from fastapi import FastAPI, Request, Response
from fastapi.responses import JSONResponse

from app.core.config import get_settings
from app.core.logging import setup_logging
from app.api.health import router as health_router
from app.tools.explain import router as explain_router
from app.tools.flashcards import router as flashcards_router
from app.tools.quiz import router as quiz_router
from app.tools.review import router as review_router


def create_app() -> FastAPI:
    settings = get_settings()
    setup_logging(settings.log_level)

    app = FastAPI(
        title="Study Helper MCP Server",
        version="1.0.0",
        docs_url="/docs",
        redoc_url="/redoc",
    )

    logger = logging.getLogger("app")

    @app.middleware("http")
    async def request_logging_middleware(request: Request, call_next: Callable[[Request], Response]):
        start = time.perf_counter()
        try:
            response = await call_next(request)
        except Exception as e:  # noqa: BLE001 (boundary)
            logger.exception("Unhandled error", extra={"path": str(request.url.path), "method": request.method})
            return JSONResponse(status_code=500, content={"detail": "Internal Server Error"})
        dur_ms = int((time.perf_counter() - start) * 1000)
        logger.info(
            "request",
            extra={
                "method": request.method,
                "path": str(request.url.path),
                "status_code": response.status_code,
                "duration_ms": dur_ms,
            },
        )
        return response

    @app.exception_handler(ValueError)
    async def value_error_handler(_: Request, exc: ValueError):
        return JSONResponse(status_code=400, content={"detail": str(exc)})

    app.include_router(health_router)
    app.include_router(flashcards_router)
    app.include_router(quiz_router)
    app.include_router(explain_router)
    app.include_router(review_router)

    return app


app = create_app()
