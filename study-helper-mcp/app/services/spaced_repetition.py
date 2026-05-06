from __future__ import annotations

import time

from app.data.storage import get_storage
from app.models.request import ScheduleReviewRequest
from app.models.response import ScheduleReviewResponse


class SpacedRepetitionService:
    """
    SM-2 inspired scheduler.

    - difficulty: 1(easy) .. 5(hard)
    - ease factor: clamped to [1.3, 3.5]
    - interval update: grows with ease, shrinks with difficulty
    """

    def schedule(self, req: ScheduleReviewRequest) -> ScheduleReviewResponse:
        d = int(req.difficulty)
        prev = float(req.previous_interval_days)
        ef = float(req.ease_factor)

        # Map difficulty -> "quality" (SM-2 uses 0..5, higher is better)
        # Here: easy(1)->5, hard(5)->1
        q = 6 - d

        # Standard SM-2 ease adjustment formula
        next_ef = ef + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        next_ef = max(1.3, min(3.5, next_ef))

        if prev <= 0:
            next_iv = 1.0 if d <= 2 else 0.5
        elif prev < 1.5:
            next_iv = 6.0 if d <= 2 else 2.0
        else:
            # grow interval; difficulty dampens growth
            growth = next_ef * (1.15 if d <= 2 else 0.85 if d == 3 else 0.6)
            next_iv = max(0.5, prev * growth)

        # clamp to reasonable bounds
        next_iv = float(max(0.5, min(3650.0, next_iv)))

        payload = {
            "user_id": req.user_id,
            "item_id": req.item_id,
            "difficulty": d,
            "previous_interval_days": prev,
            "ease_factor": ef,
            "next_interval_days": next_iv,
            "next_ease_factor": next_ef,
            "ts": int(time.time()),
        }
        get_storage().upsert_review(user_id=req.user_id, item_id=req.item_id, payload=payload)

        return ScheduleReviewResponse(
            user_id=req.user_id,
            item_id=req.item_id,
            difficulty=d,
            previous_interval_days=prev,
            ease_factor=ef,
            next_interval_days=next_iv,
            next_ease_factor=next_ef,
        )
