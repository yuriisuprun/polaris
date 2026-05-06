from __future__ import annotations

from fastapi import APIRouter

from app.models.request import ExplainSimpleRequest
from app.models.response import ExplainSimpleResponse
from app.services.explanation import ExplanationService

router = APIRouter(tags=["tools"])


@router.post("/explain_simple", response_model=ExplainSimpleResponse)
async def explain_simple(req: ExplainSimpleRequest) -> ExplainSimpleResponse:
    return await ExplanationService().explain(req)
