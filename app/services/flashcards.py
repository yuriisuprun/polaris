from __future__ import annotations

from pydantic import ValidationError

from app.core import llm as llm_mod
from app.core.errors import InvalidLLMOutputError, UpstreamLLMError
from app.core.prompting import wrap_source_text
from app.models.request import FlashcardsRequest
from app.models.response import Flashcard, GenerateFlashcardsResponse
from app.utils.text import clean_text


class FlashcardsService:
    _schema = {
        "type": "object",
        "additionalProperties": False,
        "properties": {
            "cards": {
                "type": "array",
                "minItems": 1,
                "maxItems": 50,
                "items": {
                    "type": "object",
                    "additionalProperties": False,
                    "properties": {
                        "question": {"type": "string", "minLength": 1, "maxLength": 500},
                        "answer": {"type": "string", "minLength": 1, "maxLength": 2000},
                    },
                    "required": ["question", "answer"],
                },
            }
        },
        "required": ["cards"],
    }

    async def generate(self, req: FlashcardsRequest) -> GenerateFlashcardsResponse:
        text = clean_text(req.text)

        system_prompt = (
            "You generate high-quality study flashcards.\n"
            "Return ONLY valid JSON matching the provided schema.\n"
            "No markdown, no extra keys."
        )
        user_prompt = (
            f"Topic: {req.topic}\n"
            f"Generate {req.num_cards} flashcards from this text.\n"
            "Write questions that test understanding (not trivia). Answers must be concise.\n\n"
            f"{wrap_source_text(text)}"
        )

        llm = llm_mod.get_llm()
        try:
            out = await llm.generate_structured(
                system_prompt=system_prompt,
                user_prompt=user_prompt,
                json_schema=self._schema,
            )
        except llm_mod.LLMError as e:
            raise UpstreamLLMError(str(e)) from e

        try:
            cards = [Flashcard.model_validate(c) for c in out.get("cards", [])]
            return GenerateFlashcardsResponse(topic=req.topic, cards=cards)
        except ValidationError as e:
            raise InvalidLLMOutputError("LLM returned invalid flashcards structure") from e
