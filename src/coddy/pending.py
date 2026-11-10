"""Explicit approval queue for candidate long-term memories."""

from __future__ import annotations

import json
import uuid
from datetime import datetime, timezone
from pathlib import Path

from coddy.memory import CATEGORIES, Memory, MemoryStore


class PendingQueue:
    def __init__(self, path: Path):
        self.path = Path(path).expanduser()

    def list_pending(self) -> list[dict]:
        if not self.path.exists():
            return []
        try:
            result = json.loads(self.path.read_text(encoding='utf-8'))
            if not isinstance(result, list) or any(not isinstance(v, dict) for v in result):
                raise ValueError('Invalid queue format')
            return result
        except (json.JSONDecodeError, UnicodeError) as exc:
            raise ValueError('Memory suggestion queue is corrupt; no changes made') from exc

    def _write(self, entries: list[dict]) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        temporary = self.path.with_name(f'{self.path.name}.{uuid.uuid4().hex}.tmp')
        try:
            temporary.write_text(json.dumps(entries, ensure_ascii=False, indent=2), encoding='utf-8')
            temporary.replace(self.path)
        finally:
            temporary.unlink(missing_ok=True)

    def propose(self, category: str, title: str, content: str, source: str = 'manual') -> dict:
        if category not in CATEGORIES:
            raise ValueError('Unsupported category')
        title, content = title.strip(), content.strip()
        if not title or not content or len(title) > 120 or len(content) > 12000:
            raise ValueError('Invalid title or memory content')
        pending = self.list_pending()
        for candidate in pending:
            if (candidate.get('category'), candidate.get('title'), candidate.get('content')) == (category, title, content):
                return candidate
        proposal = {
            'id': uuid.uuid4().hex, 'category': category,
            'title': title, 'content': content,
            'source': source[:120],
            'proposed_at': datetime.now(timezone.utc).isoformat(timespec='seconds'),
        }
        pending.append(proposal)
        self._write(pending)
        return proposal

    def reject(self, proposal_id: str) -> bool:
        pending = self.list_pending()
        remaining = [p for p in pending if p.get('id') != proposal_id]
        if len(remaining) == len(pending):
            return False
        self._write(remaining)
        return True

    def approve(self, proposal_id: str, vault: MemoryStore) -> Memory:
        pending = self.list_pending()
        proposal = next((p for p in pending if p.get('id') == proposal_id), None)
        if proposal is None:
            raise ValueError('No pending suggestion with this ID')
        item = vault.remember(proposal['category'], proposal['title'], proposal['content'])
        self._write([p for p in pending if p.get('id') != proposal_id])
        return item
