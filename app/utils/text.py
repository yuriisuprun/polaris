from __future__ import annotations

import re


_WS_RE = re.compile(r"\s+")


def clean_text(text: str) -> str:
    """
    Normalizes whitespace and removes obvious noise.
    Keeps content faithful while improving model input quality.
    """
    t = text.replace("\u00a0", " ").strip()
    t = _WS_RE.sub(" ", t)
    return t


def truncate(text: str, *, max_chars: int) -> str:
    if len(text) <= max_chars:
        return text
    return text[: max_chars - 1] + "…"
