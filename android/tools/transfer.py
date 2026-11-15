"""Explicit offline transfer between Coddy's vault, approval queue and Android."""
from __future__ import annotations
import argparse
import json
import re
import uuid
from datetime import datetime, timezone
from pathlib import Path
from coddy.memory import CATEGORIES, MemoryStore
from coddy.pending import PendingQueue

MAX_BYTES = 8 * 1024 * 1024
SECRET = re.compile(r"sk-[A-Za-z0-9_-]{16,}|gh[pousr]_[A-Za-z0-9]{20,}|-----BEGIN .*PRIVATE KEY-----|(?:api[_-]?key|password|access[_-]?token)\s*[:=]\s*\S{8,}", re.I)

def _time(value):
    if not isinstance(value, str):
        raise ValueError("Missing timestamp")
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as exc:
        raise ValueError("Invalid timestamp") from exc
    if parsed.tzinfo is None:
        raise ValueError("Timestamp must include a timezone")

def _atomic_json(path, value, serialized=None):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + "." + uuid.uuid4().hex + ".tmp")
    try:
        encoded = serialized if isinstance(serialized, bytes) else json.dumps(value, ensure_ascii=False, indent=2).encode("utf-8")
        temporary.write_bytes(encoded)
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)

def export_snapshot(vault, output, selected=None):
    """Read approved notes only; never initialize or modify the vault."""
    store = MemoryStore(Path(vault))
    if not store.is_initialized():
        raise ValueError("Choose an initialized Coddy vault")
    output = Path(output)
    if output.resolve().is_relative_to(store.root.resolve()):
        raise ValueError("Save the transfer file outside the vault")
    selected_ids = None if selected is None else set(selected)
    memories = store._memories()
    if selected_ids is not None:
        if not selected_ids.issubset({m.id for m in memories}):
            raise ValueError("An explicitly selected memory was not found")
        memories = [m for m in memories if m.id in selected_ids]
    if len(memories) > 2000:
        raise ValueError("Select at most 2,000 memories")
    notes = []
    for item in memories:
        if item.category not in CATEGORIES or not re.fullmatch(r"[a-f0-9]{32}",item.id):
            raise ValueError("A selected note has invalid metadata")
        _time(item.created_at)
        if not item.title.strip() or len(item.title)>120 or any(ord(c)<32 or 127<=ord(c)<=159 for c in item.title):
            raise ValueError("A selected note has an invalid title")
        if SECRET.search(item.title + "\n" + item.content):
            raise ValueError("A selected note resembles a credential; exclude it")
        notes.append(dict(id=item.id, category=item.category, title=item.title, content=item.content,
                          source=f"01 Memories/{CATEGORIES[item.category]}/{item.id}.md", created_at=item.created_at))
    payload = dict(format="coddy.snapshot", version=1, exported_at=datetime.now(timezone.utc).isoformat(), notes=notes)
    serialized = json.dumps(payload, ensure_ascii=False, indent=2).encode("utf-8")
    if len(serialized) > MAX_BYTES:
        raise ValueError("Snapshot exceeds 8 MiB")
    if output.exists():
        raise ValueError("Output already exists; choose a new filename")
    _atomic_json(output, payload, serialized)
    return len(notes)

def _validated_proposal(p):
    if not isinstance(p, dict) or p.get("state") != "REVIEWED":
        raise ValueError("Only explicitly reviewed Android proposals can be imported")
    if not re.fullmatch(r"[a-f0-9]{32}", str(p.get("id", ""))):
        raise ValueError("Invalid proposal ID")
    if p.get("category") not in CATEGORIES:
        raise ValueError("Unsupported category")
    for field, limit in [("title", 120), ("content", 12000), ("source", 240)]:
        value = p.get(field)
        if not isinstance(value, str) or not value.strip() or len(value) > limit:
            raise ValueError(f"Invalid {field}")
    confidence, importance = p.get("confidence"), p.get("importance")
    if isinstance(confidence, bool) or not isinstance(confidence, (int, float)) or not 0 <= confidence <= 1:
        raise ValueError("Confidence must be between zero and one")
    if isinstance(importance, bool) or not isinstance(importance, int) or not 1 <= importance <= 5:
        raise ValueError("Importance must be between one and five")
    _time(p.get("proposed_at"))
    if SECRET.search(p["title"] + "\n" + p["content"] + "\n" + p["source"]):
        raise ValueError("Credentials cannot be imported as memories")
    return p

def import_proposals(bundle_path, pending_path):
    """Merge reviewed candidates into pending.json, never into the vault."""
    path, pending_path = Path(bundle_path), Path(pending_path)
    if path.resolve() == pending_path.resolve() or path.stat().st_size > MAX_BYTES:
        raise ValueError("Choose a separate proposal bundle under 8 MiB")
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict) or data.get("format") != "coddy.proposals" or type(data.get("version")) is not int or data["version"] != 1:
        raise ValueError("Unsupported proposal bundle")
    _time(data.get("exported_at"))
    proposals = data.get("proposals")
    if not isinstance(proposals, list) or len(proposals) > 2000:
        raise ValueError("Invalid proposal list")
    proposals = [_validated_proposal(p) for p in proposals]
    if len({p["id"] for p in proposals}) != len(proposals):
        raise ValueError("Duplicate proposal IDs")
    receipts_path = pending_path.with_name(pending_path.stem + ".android-receipts.json")
    receipts = json.loads(receipts_path.read_text()) if receipts_path.exists() else []
    if not isinstance(receipts, list) or any(not isinstance(v, str) for v in receipts):
        raise ValueError("Import receipts are corrupt; no changes made")
    queue = PendingQueue(pending_path)
    entries = queue.list_pending()
    added = 0
    for proposal in proposals:
        if proposal["id"] in receipts:
            continue
        duplicate = any((p.get("category"), p.get("title"), p.get("content")) == (proposal["category"], proposal["title"], proposal["content"]) for p in entries)
        if not duplicate:
            entries.append(dict(proposal, source=("android:" + proposal["id"] + " " + proposal["source"])[:120]))
            added += 1
        receipts.append(proposal["id"])
    # Validate the complete bundle before writing. Run with desktop CLI closed.
    queue._write(entries)
    _atomic_json(receipts_path, receipts)
    return added

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    export = commands.add_parser("export")
    export.add_argument("--vault", required=True)
    export.add_argument("--output", required=True)
    selection = export.add_mutually_exclusive_group(required=True)
    selection.add_argument("--all", action="store_true")
    selection.add_argument("--id", action="append")
    incoming = commands.add_parser("import")
    incoming.add_argument("--file", required=True)
    incoming.add_argument("--pending", required=True)
    args = parser.parse_args()
    try:
        count = export_snapshot(args.vault, args.output, args.id) if args.command == "export" else import_proposals(args.file, args.pending)
        print(f"{count} notes exported." if args.command == "export" else f"{count} proposals queued. Review with coddy review; approve each explicitly.")
    except (ValueError, OSError, KeyError) as exc:
        parser.exit(1, f"Transfer stopped: {exc}\n")

if __name__ == "__main__":
    main()
