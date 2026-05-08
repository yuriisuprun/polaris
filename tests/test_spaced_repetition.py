from __future__ import annotations

import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


@pytest.fixture(autouse=True)
def _env(monkeypatch, tmp_path):
    monkeypatch.setenv("GROQ_API_KEY", "test")
    monkeypatch.setenv("STORAGE_PATH", str(tmp_path / "storage.json"))


@pytest.fixture()
def client():
    from app.main import create_app

    return TestClient(create_app())


def test_sm2_progression_uses_persisted_state(client: TestClient):
    base = {"user_id": "u1", "item_id": "i1", "previous_interval_days": 0, "ease_factor": 2.5}

    r1 = client.post("/schedule_review", json={**base, "difficulty": 1})
    assert r1.status_code == 200
    body1 = r1.json()
    assert body1["next_interval_days"] == 1.0

    # Client can send nonsense previous interval; server should use stored state.
    r2 = client.post("/schedule_review", json={**base, "difficulty": 1, "previous_interval_days": 999})
    assert r2.status_code == 200
    body2 = r2.json()
    assert body2["next_interval_days"] == 6.0


def test_sm2_failure_resets_interval(client: TestClient):
    base = {"user_id": "u2", "item_id": "i2", "previous_interval_days": 0, "ease_factor": 2.5}

    client.post("/schedule_review", json={**base, "difficulty": 1})
    # Hard recall should count as failure and reset repetition.
    r = client.post("/schedule_review", json={**base, "difficulty": 5})
    assert r.status_code == 200
    body = r.json()
    assert body["next_interval_days"] == 1.0

