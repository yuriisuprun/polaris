from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class Flashcard(BaseModel):
    model_config = ConfigDict(extra="forbid")
    question: str = Field(..., min_length=1, max_length=500)
    answer: str = Field(..., min_length=1, max_length=2000)


class GenerateFlashcardsResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")
    tool: Literal["generate_flashcards"] = "generate_flashcards"
    topic: str
    cards: list[Flashcard]


class QuizQuestion(BaseModel):
    model_config = ConfigDict(extra="forbid")
    question: str = Field(..., min_length=1, max_length=500)
    options: list[str] = Field(..., min_length=2, max_length=6)
    correct_index: int = Field(..., ge=0)
    explanation: str = Field(..., min_length=1, max_length=2000)


class CreateQuizResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")
    tool: Literal["create_quiz"] = "create_quiz"
    topic: str
    questions: list[QuizQuestion]


class ExplainSimpleResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")
    tool: Literal["explain_simple"] = "explain_simple"
    topic: str
    summary: str = Field(..., min_length=1, max_length=4000)
    key_points: list[str] = Field(..., min_length=3, max_length=10)
    analogy: str = Field(..., min_length=1, max_length=2000)


class ScheduleReviewResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")
    tool: Literal["schedule_review"] = "schedule_review"
    user_id: str
    item_id: str
    difficulty: int
    previous_interval_days: float
    ease_factor: float
    next_interval_days: float
    next_ease_factor: float
