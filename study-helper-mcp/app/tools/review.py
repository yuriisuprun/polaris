from __future__ import annotations

from fastapi import APIRouter

from app.models.request import ScheduleReviewRequest
from app.models.response import ScheduleReviewResponse
from app.services.spaced_repetition import SpacedRepetitionService

router = APIRouter(tags=["tools"])


@router.post("/schedule_review", response_model=ScheduleReviewResponse)
async def schedule_review(req: ScheduleReviewRequest) -> ScheduleReviewResponse:
    return await SpacedRepetitionService().schedule(req)
