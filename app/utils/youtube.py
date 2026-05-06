from __future__ import annotations

import re
from urllib.parse import parse_qs, urlparse

from youtube_transcript_api import NoTranscriptFound, TranscriptsDisabled, YouTubeTranscriptApi

from app.utils.text import clean_text


_ID_RE = re.compile(r"^[a-zA-Z0-9_-]{6,}$")


def _extract_video_id(url: str) -> str:
    u = urlparse(url)
    if u.netloc in {"youtu.be"}:
        vid = u.path.lstrip("/")
        if _ID_RE.match(vid):
            return vid
    if "youtube.com" in u.netloc:
        qs = parse_qs(u.query)
        if "v" in qs and qs["v"]:
            vid = qs["v"][0]
            if _ID_RE.match(vid):
                return vid
        # /shorts/<id>
        parts = [p for p in u.path.split("/") if p]
        if len(parts) >= 2 and parts[0] == "shorts" and _ID_RE.match(parts[1]):
            return parts[1]
    raise ValueError("Could not extract YouTube video id from URL")


def fetch_transcript(url: str, *, languages: list[str] | None = None) -> str:
    video_id = _extract_video_id(url)
    languages = languages or ["en"]
    try:
        transcript = YouTubeTranscriptApi.get_transcript(video_id, languages=languages)
    except (NoTranscriptFound, TranscriptsDisabled) as e:
        raise RuntimeError("No transcript available for this video") from e

    text = " ".join(chunk.get("text", "") for chunk in transcript)
    return clean_text(text)
