from __future__ import annotations

import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


class _FakeLLM:
    async def generate_structured(self, *, system_prompt: str, user_prompt: str, json_schema: dict):  # noqa: ARG002
        # Return deterministic payloads compatible with our schemas.
        if "flashcards" in system_prompt.lower():
            return {"cards": [{"question": "Q1?", "answer": "A1."}, {"question": "Q2?", "answer": "A2."}]}
        if "multiple-choice" in system_prompt.lower() or "quizzes" in system_prompt.lower():
            return {
                "questions": [
                    {
                        "question": "What is X?",
                        "options": ["A", "B", "C", "D"],
                        "correct_index": 1,
                        "explanation": "Because.",
                    }
                ]
            }
        if "explain concepts" in system_prompt.lower():
            return {
                "summary": "Simple summary.",
                "key_points": ["p1", "p2", "p3"],
                "analogy": "Like something familiar.",
            }
        raise AssertionError("Unexpected prompt")


@pytest.fixture(autouse=True)
def _env(monkeypatch, tmp_path):
    # Ensure config reads without needing a real key and isolate storage.
    monkeypatch.setenv("GROQ_API_KEY", "test")
    monkeypatch.setenv("STORAGE_PATH", str(tmp_path / "storage.json"))

    # Patch get_llm to avoid network calls.
    import app.core.llm as llm_mod

    monkeypatch.setattr(llm_mod, "get_llm", lambda: _FakeLLM())


@pytest.fixture()
def client():
    from app.main import create_app

    return TestClient(create_app())


def test_generate_flashcards(client: TestClient):
    r = client.post(
        "/generate_flashcards",
        json={"topic": "Biology", "text": "Cells are the basic unit of life." * 5, "num_cards": 2},
    )
    assert r.status_code == 200
    body = r.json()
    assert body["tool"] == "generate_flashcards"
    assert body["topic"] == "Biology"
    assert isinstance(body["cards"], list) and len(body["cards"]) >= 1
    assert "question" in body["cards"][0] and "answer" in body["cards"][0]


def test_create_quiz(client: TestClient):
    r = client.post(
        "/create_quiz",
        json={"topic": "Math", "text": "Addition combines quantities." * 10, "num_questions": 1, "num_options": 4},
    )
    assert r.status_code == 200
    body = r.json()
    assert body["tool"] == "create_quiz"
    assert body["topic"] == "Math"
    assert len(body["questions"]) == 1
    q = body["questions"][0]
    assert len(q["options"]) == 4
    assert 0 <= q["correct_index"] < 4


def test_explain_simple(client: TestClient):
    r = client.post(
        "/explain_simple",
        json={"topic": "Gravity", "text": "Gravity attracts masses." * 10, "target_audience": "beginner"},
    )
    assert r.status_code == 200
    body = r.json()
    assert body["tool"] == "explain_simple"
    assert body["topic"] == "Gravity"
    assert isinstance(body["key_points"], list) and len(body["key_points"]) >= 3


def test_schedule_review(client: TestClient):
    r = client.post(
        "/schedule_review",
        json={
            "user_id": "u1",
            "item_id": "i1",
            "difficulty": 3,
            "previous_interval_days": 6,
            "ease_factor": 2.5,
        },
    )
    assert r.status_code == 200
    body = r.json()
    assert body["tool"] == "schedule_review"
    assert body["user_id"] == "u1"
    assert body["item_id"] == "i1"
    assert body["next_interval_days"] > 0

