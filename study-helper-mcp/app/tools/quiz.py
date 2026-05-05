from __future__ import annotations

import logging

from fastapi import APIRouter, HTTPException

from app.models.request import QuizRequest
from app.models.response import CreateQuizResponse
from app.services.quiz import QuizService

logger = logging.getLogger(__name__)

router = APIRouter(tags=["tools"])


@router.post("/create_quiz", response_model=CreateQuizResponse)
def create_quiz(req: QuizRequest) -> CreateQuizResponse:
    try:
        return QuizService().create(req)
    except Exception as e:  # noqa: BLE001 (boundary)
        logger.exception("create_quiz failed")
        raise HTTPException(status_code=500, detail=str(e)) from e
