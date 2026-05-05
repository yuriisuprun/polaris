from __future__ import annotations

import logging

from fastapi import APIRouter, HTTPException

from app.models.request import ScheduleReviewRequest
from app.models.response import ScheduleReviewResponse
from app.services.spaced_repetition import SpacedRepetitionService

logger = logging.getLogger(__name__)

router = APIRouter(tags=["tools"])


@router.post("/schedule_review", response_model=ScheduleReviewResponse)
def schedule_review(req: ScheduleReviewRequest) -> ScheduleReviewResponse:
    try:
        return SpacedRepetitionService().schedule(req)
    except Exception as e:  # noqa: BLE001 (boundary)
        logger.exception("schedule_review failed")
        raise HTTPException(status_code=500, detail=str(e)) from e
