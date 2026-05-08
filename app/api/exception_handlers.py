from __future__ import annotations

import logging

from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.core.context import get_request_id
from app.core.errors import AppError

logger = logging.getLogger("app.errors")


def _error_payload(*, code: str, message: str, status_code: int) -> JSONResponse:
    request_id = get_request_id()
    return JSONResponse(
        status_code=status_code,
        content={
            "error": {
                "code": code,
                "message": message,
                "request_id": request_id,
            }
        },
    )


async def app_error_handler(_: Request, exc: AppError) -> JSONResponse:
    if exc.status_code >= 500:
        logger.error("app_error", extra={"code": exc.code, "request_id": get_request_id(), "details": exc.details})
    else:
        logger.info("app_error", extra={"code": exc.code, "request_id": get_request_id(), "details": exc.details})
    return _error_payload(code=exc.code, message=exc.message, status_code=exc.status_code)


async def validation_error_handler(_: Request, exc: RequestValidationError) -> JSONResponse:
    # Avoid echoing full payload; give clients structured, actionable fields only.
    logger.info("validation_error", extra={"request_id": get_request_id(), "errors": str(exc.errors())})
    return JSONResponse(
        status_code=422,
        content={
            "error": {
                "code": "VALIDATION_ERROR",
                "message": "Request validation failed",
                "request_id": get_request_id(),
                "details": exc.errors(),
            }
        },
    )


async def http_exception_handler(_: Request, exc: StarletteHTTPException) -> JSONResponse:
    # Normalize all HTTPExceptions into the same envelope.
    msg = exc.detail if isinstance(exc.detail, str) else "Request failed"
    logger.info("http_exception", extra={"status_code": exc.status_code, "request_id": get_request_id()})
    return _error_payload(code="HTTP_ERROR", message=msg, status_code=exc.status_code)


async def unhandled_exception_handler(_: Request, exc: Exception) -> JSONResponse:  # noqa: BLE001 (boundary)
    logger.exception("unhandled_exception", extra={"request_id": get_request_id()})
    return _error_payload(code="INTERNAL_ERROR", message="Internal Server Error", status_code=500)

