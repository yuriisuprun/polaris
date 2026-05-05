from __future__ import annotations

from pathlib import Path

import pdfplumber

from app.utils.text import clean_text


def extract_text_from_pdf(path: str) -> str:
    p = Path(path).expanduser().resolve()
    if not p.exists() or not p.is_file():
        raise FileNotFoundError(f"PDF not found: {p}")

    parts: list[str] = []
    with pdfplumber.open(str(p)) as pdf:
        for page in pdf.pages:
            txt = page.extract_text() or ""
            if txt.strip():
                parts.append(txt)

    return clean_text("\n".join(parts))
