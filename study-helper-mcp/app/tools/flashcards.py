from __future__ import annotations

import logging

from fastapi import APIRouter, HTTPException

from app.models.request import FlashcardsRequest
from app.models.response import GenerateFlashcardsResponse
from app.services.flashcards import FlashcardsService

logger = logging.getLogger(__name__)

router = APIRouter(tags=["tools"])


@router.post("/generate_flashcards", response_model=GenerateFlashcardsResponse)
def generate_flashcards(req: FlashcardsRequest) -> GenerateFlashcardsResponse:
    try:
        return FlashcardsService().generate(req)
    except Exception as e:  # noqa: BLE001 (boundary)
        logger.exception("generate_flashcards failed")
        raise HTTPException(status_code=500, detail=str(e)) from e
