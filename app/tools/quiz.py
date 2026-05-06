from __future__ import annotations

from fastapi import APIRouter

from app.models.request import QuizRequest
from app.models.response import CreateQuizResponse
from app.services.quiz import QuizService

router = APIRouter(tags=["tools"])


@router.post("/create_quiz", response_model=CreateQuizResponse)
async def create_quiz(req: QuizRequest) -> CreateQuizResponse:
    return await QuizService().create(req)
