from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field, HttpUrl


class FlashcardsRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    topic: str = Field(..., min_length=2, max_length=200, description="Topic or title for context.")
    text: str = Field(..., min_length=20, max_length=50_000, description="Source study text.")
    num_cards: int = Field(default=10, ge=1, le=50)


class QuizRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    topic: str = Field(..., min_length=2, max_length=200)
    text: str = Field(..., min_length=20, max_length=50_000)
    num_questions: int = Field(default=8, ge=1, le=30)
    num_options: int = Field(default=4, ge=2, le=6)


class ExplainSimpleRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    topic: str = Field(..., min_length=2, max_length=200)
    text: str = Field(..., min_length=20, max_length=50_000)
    target_audience: str = Field(default="beginner", min_length=2, max_length=50)


class ScheduleReviewRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    user_id: str = Field(..., min_length=1, max_length=100)
    item_id: str = Field(..., min_length=1, max_length=200)
    difficulty: int = Field(..., ge=1, le=5, description="1=easy, 5=hard")
    previous_interval_days: float = Field(default=0.0, ge=0.0, le=3650.0)
    ease_factor: float = Field(default=2.5, ge=1.3, le=3.5)


class ExtractPdfRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    file_path: str = Field(..., min_length=1, max_length=500)


class YouTubeTranscriptRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    url: HttpUrl
