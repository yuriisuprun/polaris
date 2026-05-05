from __future__ import annotations

import logging

from fastapi import APIRouter, HTTPException

from app.models.request import ExplainSimpleRequest
from app.models.response import ExplainSimpleResponse
from app.services.explanation import ExplanationService

logger = logging.getLogger(__name__)

router = APIRouter(tags=["tools"])


@router.post("/explain_simple", response_model=ExplainSimpleResponse)
def explain_simple(req: ExplainSimpleRequest) -> ExplainSimpleResponse:
    try:
        return ExplanationService().explain(req)
    except Exception as e:  # noqa: BLE001 (boundary)
        logger.exception("explain_simple failed")
        raise HTTPException(status_code=500, detail=str(e)) from e
