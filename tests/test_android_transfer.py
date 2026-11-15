import json
import tempfile
import unittest
from unittest.mock import patch
from datetime import datetime, timezone
from pathlib import Path
from android.tools.transfer import export_snapshot, import_proposals
from coddy.memory import MemoryStore
from coddy.pending import PendingQueue

class AndroidTransferTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.vault = MemoryStore(self.root / "vault")
        self.vault.initialize()
        self.note = self.vault.remember("preference", "Style", "Use short paragraphs.")
        self.pending = self.root / "pending.json"
    def vault_bytes(self):
        return {str(p.relative_to(self.vault.root)):p.read_bytes() for p in self.vault.root.rglob("*") if p.is_file()}
    def proposal(self, **changes):
        value = dict(id="b"*32, category="fact", title="Example", content="A sample proposal.", source="Android chat", importance=3, confidence=0.9, proposed_at="2026-11-15T15:00:00Z", state="REVIEWED")
        value.update(changes)
        return value
    def bundle(self, proposals):
        path=self.root/"proposals.json"
        path.write_text(json.dumps(dict(format="coddy.proposals",version=1,exported_at="2026-11-15T16:00:00Z",proposals=proposals)))
        return path
    def test_export_contains_only_selected_approved_notes_and_preserves_vault(self):
        other=self.vault.remember("fact","Other","Not selected.")
        before=self.vault_bytes()
        out=self.root/"snapshot.json"
        self.assertEqual(1,export_snapshot(self.vault.root,out,[self.note.id]))
        data=json.loads(out.read_text())
        self.assertEqual([self.note.id],[n["id"] for n in data["notes"]])
        self.assertTrue(data["notes"][0]["source"].startswith("01 Memories/"))
        self.assertEqual(before,self.vault_bytes())
    def test_import_adds_pending_candidates_and_never_writes_vault(self):
        before=self.vault_bytes()
        path=self.bundle([self.proposal()])
        self.assertEqual(1,import_proposals(path,self.pending))
        self.assertEqual(1,len(PendingQueue(self.pending).list_pending()))
        self.assertEqual(before,self.vault_bytes())
        self.assertEqual(0,import_proposals(path,self.pending))
    def test_invalid_second_proposal_does_not_partially_import(self):
        path=self.bundle([self.proposal(),self.proposal(id="c"*32,category="system")])
        with self.assertRaises(ValueError):
            import_proposals(path,self.pending)
        self.assertFalse(self.pending.exists())
    def test_unreviewed_or_secret_proposals_are_rejected(self):
        for item in [self.proposal(state="DRAFT"), self.proposal(content="OPENAI_API_KEY=sk-proj-"+"x"*40)]:
            with self.assertRaises(ValueError):
                import_proposals(self.bundle([item]),self.pending)
        self.assertFalse(self.pending.exists())
    def test_unsupported_version_is_rejected(self):
        path=self.bundle([])
        data=json.loads(path.read_text()); data["version"]=9; path.write_text(json.dumps(data))
        with self.assertRaises(ValueError):
            import_proposals(path,self.pending)
    def test_size_limit_applies_to_the_exact_written_json(self):
        at=datetime(2026,11,15,16,0,0,tzinfo=timezone.utc)
        note=self.note
        payload=dict(format="coddy.snapshot",version=1,exported_at=at.isoformat(),notes=[dict(id=note.id,category=note.category,title=note.title,content=note.content,source=f"01 Memories/Preferences/{note.id}.md",created_at=note.created_at)])
        compact_size=len(json.dumps(payload,ensure_ascii=False).encode())
        with patch("android.tools.transfer.datetime") as clock, patch("android.tools.transfer.MAX_BYTES",compact_size+1):
            clock.now.return_value=at
            with self.assertRaises(ValueError):
                export_snapshot(self.vault.root,self.root/"too-large.json")
