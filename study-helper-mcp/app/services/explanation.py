from __future__ import annotations

from pydantic import ValidationError

from app.core.llm import LLMError, get_llm
from app.models.request import ExplainSimpleRequest
from app.models.response import ExplainSimpleResponse
from app.utils.text import clean_text, truncate


class ExplanationService:
    _schema = {
        "type": "object",
        "additionalProperties": False,
        "properties": {
            "summary": {"type": "string", "minLength": 1, "maxLength": 4000},
            "key_points": {
                "type": "array",
                "minItems": 3,
                "maxItems": 10,
                "items": {"type": "string", "minLength": 1, "maxLength": 300},
            },
            "analogy": {"type": "string", "minLength": 1, "maxLength": 2000},
        },
        "required": ["summary", "key_points", "analogy"],
    }

    def explain(self, req: ExplainSimpleRequest) -> ExplainSimpleResponse:
        text = truncate(clean_text(req.text), max_chars=18_000)

        system_prompt = (
            "You explain concepts simply and accurately.\n"
            "Return ONLY valid JSON matching the provided schema.\n"
            "No markdown, no extra keys."
        )
        user_prompt = (
            f"Topic: {req.topic}\n"
            f"Audience: {req.target_audience}\n"
            "Explain the topic using the provided text as source.\n"
            "Include a short analogy and 3-10 key points.\n\n"
            f"TEXT:\n{text}"
        )

        llm = get_llm()
        try:
            out = llm.generate_structured(
                system_prompt=system_prompt,
                user_prompt=user_prompt,
                json_schema=self._schema,
            )
        except LLMError as e:
            raise RuntimeError(str(e)) from e

        try:
            return ExplainSimpleResponse(topic=req.topic, **out)
        except ValidationError as e:
            raise RuntimeError("LLM returned invalid explanation structure") from e
