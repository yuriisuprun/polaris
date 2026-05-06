from __future__ import annotations

from fastapi import APIRouter

from app.models.request import FlashcardsRequest
from app.models.response import GenerateFlashcardsResponse
from app.services.flashcards import FlashcardsService

router = APIRouter(tags=["tools"])


@router.post("/generate_flashcards", response_model=GenerateFlashcardsResponse)
async def generate_flashcards(req: FlashcardsRequest) -> GenerateFlashcardsResponse:
    return await FlashcardsService().generate(req)
