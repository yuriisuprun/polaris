from __future__ import annotations

from pydantic import ValidationError

from app.core import llm as llm_mod
from app.core.errors import InvalidLLMOutputError, UpstreamLLMError
from app.core.prompting import wrap_source_text
from app.models.request import QuizRequest
from app.models.response import CreateQuizResponse, QuizQuestion
from app.utils.text import clean_text


class QuizService:
    def _schema(self, *, max_questions: int, max_options: int) -> dict:
        return {
            "type": "object",
            "additionalProperties": False,
            "properties": {
                "questions": {
                    "type": "array",
                    "minItems": 1,
                    "maxItems": max_questions,
                    "items": {
                        "type": "object",
                        "additionalProperties": False,
                        "properties": {
                            "question": {"type": "string", "minLength": 1, "maxLength": 500},
                            "options": {
                                "type": "array",
                                "minItems": 2,
                                "maxItems": max_options,
                                "items": {"type": "string", "minLength": 1, "maxLength": 200},
                            },
                            "correct_index": {"type": "integer", "minimum": 0, "maximum": max_options - 1},
                            "explanation": {"type": "string", "minLength": 1, "maxLength": 2000},
                        },
                        "required": ["question", "options", "correct_index", "explanation"],
                    },
                }
            },
            "required": ["questions"],
        }

    async def create(self, req: QuizRequest) -> CreateQuizResponse:
        text = clean_text(req.text)
        schema = self._schema(max_questions=req.num_questions, max_options=req.num_options)

        system_prompt = (
            "You generate multiple-choice quizzes.\n"
            "Return ONLY valid JSON matching the provided schema.\n"
            "No markdown, no extra keys."
        )
        user_prompt = (
            f"Topic: {req.topic}\n"
            f"Create {req.num_questions} multiple-choice questions with exactly {req.num_options} options each.\n"
            "Make wrong options plausible. correct_index must point to the correct option.\n\n"
            f"{wrap_source_text(text)}"
        )

        llm = llm_mod.get_llm()
        try:
            out = await llm.generate_structured(system_prompt=system_prompt, user_prompt=user_prompt, json_schema=schema)
        except llm_mod.LLMError as e:
            raise UpstreamLLMError(str(e)) from e

        try:
            questions = [QuizQuestion.model_validate(q) for q in out.get("questions", [])]
            # ensure correct_index fits the actual options list length
            for q in questions:
                if q.correct_index >= len(q.options):
                    raise ValueError("correct_index out of range")
            return CreateQuizResponse(topic=req.topic, questions=questions)
        except (ValidationError, ValueError) as e:
            raise InvalidLLMOutputError("LLM returned invalid quiz structure") from e
