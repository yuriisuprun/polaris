from __future__ import annotations

from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.core.config import get_settings
from app.core.logging import setup_logging
from app.api.health import router as health_router
from app.api.exception_handlers import (
    app_error_handler,
    http_exception_handler,
    unhandled_exception_handler,
    validation_error_handler,
)
from app.api.middleware import request_context_middleware
from app.core.errors import AppError
from app.tools.explain import router as explain_router
from app.tools.flashcards import router as flashcards_router
from app.tools.quiz import router as quiz_router
from app.tools.review import router as review_router


def create_app() -> FastAPI:
    settings = get_settings()
    setup_logging(settings.log_level)
    if settings.is_prod and not settings.openai_api_key:
        raise RuntimeError("OPENAI_API_KEY must be set in production")

    app = FastAPI(
        title="Study Helper MCP Server",
        version="1.0.0",
        docs_url="/docs",
        redoc_url="/redoc",
    )

    app.middleware("http")(request_context_middleware)

    app.add_exception_handler(AppError, app_error_handler)
    app.add_exception_handler(RequestValidationError, validation_error_handler)
    app.add_exception_handler(StarletteHTTPException, http_exception_handler)
    app.add_exception_handler(Exception, unhandled_exception_handler)

    app.include_router(health_router)
    app.include_router(flashcards_router)
    app.include_router(quiz_router)
    app.include_router(explain_router)
    app.include_router(review_router)

    return app


app = create_app()
