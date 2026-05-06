from __future__ import annotations

from pydantic import ValidationError

from app.core import llm as llm_mod
from app.core.errors import InvalidLLMOutputError, UpstreamLLMError
from app.core.prompting import wrap_source_text
from app.models.request import ExplainSimpleRequest
from app.models.response import ExplainSimpleResponse
from app.utils.text import clean_text


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

    async def explain(self, req: ExplainSimpleRequest) -> ExplainSimpleResponse:
        text = clean_text(req.text)

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
            return ExplainSimpleResponse(topic=req.topic, **out)
        except ValidationError as e:
            raise InvalidLLMOutputError("LLM returned invalid explanation structure") from e
