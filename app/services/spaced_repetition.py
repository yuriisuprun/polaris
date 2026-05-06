from __future__ import annotations

import time

from app.data.storage import get_storage
from app.core.errors import StorageError
from app.models.request import ScheduleReviewRequest
from app.models.response import ScheduleReviewResponse


class SpacedRepetitionService:
    """
    SM-2 inspired scheduler.

    - difficulty: 1(easy) .. 5(hard)
    - ease factor: clamped to [1.3, 3.5]
    - interval update: grows with ease, shrinks with difficulty
    """

    async def schedule(self, req: ScheduleReviewRequest) -> ScheduleReviewResponse:
        """
        SM-2-ish algorithm with persisted repetition state.

        Request keeps backwards-compatible inputs, but when we have stored state
        we prefer it over client-supplied counters to prevent drift.
        """
        d = int(req.difficulty)
        storage = get_storage()

        try:
            stored = storage.get_review(user_id=req.user_id, item_id=req.item_id) or {}
        except Exception as e:  # noqa: BLE001 (boundary)
            raise StorageError() from e

        # Backwards-compatible defaults (client-supplied), overridden by stored state if present.
        prev_interval = float(stored.get("next_interval_days", req.previous_interval_days))
        ef = float(stored.get("next_ease_factor", req.ease_factor))
        repetition = int(stored.get("repetition", 0))
        lapses = int(stored.get("lapses", 0))

        # Map difficulty -> "quality" (SM-2 uses 0..5, higher is better)
        # easy(1)->5 ... hard(5)->1
        quality = 6 - d

        # SM-2: if quality < 3 treat as a failed recall (lapse)
        failed = quality < 3

        # Ease factor adjustment (SM-2 standard)
        next_ef = ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
        next_ef = float(max(1.3, min(3.5, next_ef)))

        if failed:
            # Reset repetition; schedule a quick relearn.
            repetition = 0
            lapses += 1
            next_interval = 1.0
        else:
            repetition += 1
            if repetition == 1:
                next_interval = 1.0
            elif repetition == 2:
                next_interval = 6.0
            else:
                next_interval = max(1.0, prev_interval * next_ef)

        next_interval = float(max(0.5, min(3650.0, next_interval)))

        payload = {
            "user_id": req.user_id,
            "item_id": req.item_id,
            "difficulty": d,
            "previous_interval_days": prev_interval,
            "ease_factor": ef,
            "next_interval_days": next_interval,
            "next_ease_factor": next_ef,
            "repetition": repetition,
            "lapses": lapses,
            "ts": int(time.time()),
        }

        try:
            storage.upsert_review(user_id=req.user_id, item_id=req.item_id, payload=payload)
        except Exception as e:  # noqa: BLE001 (boundary)
            raise StorageError() from e

        return ScheduleReviewResponse(
            user_id=req.user_id,
            item_id=req.item_id,
            difficulty=d,
            previous_interval_days=prev_interval,
            ease_factor=ef,
            next_interval_days=next_interval,
            next_ease_factor=next_ef,
        )
