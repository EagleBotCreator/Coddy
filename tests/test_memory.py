import tempfile
import unittest
from pathlib import Path

from coddy.memory import MemoryStore


class MemoryStoreTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name) / 'Coddy Memory'
        self.store = MemoryStore(self.root)

    def test_initialize_creates_vault_and_identity(self):
        self.store.initialize()
        self.assertTrue((self.root / '00 System' / 'Identity.md').is_file())
        self.assertTrue((self.root / '01 Memories').is_dir())

    def test_store_and_find_memory_across_instances(self):
        self.store.initialize()
        memory = self.store.remember('preference', 'Favorite editor', 'I prefer VSCodium because it is open source.')
        reopened = MemoryStore(self.root)
        hits = reopened.search('VSCodium')
        self.assertEqual(hits[0].id, memory.id)
        self.assertIn('open source', hits[0].content)

    def test_rejects_invalid_category_and_empty_content(self):
        self.store.initialize()
        with self.assertRaises(ValueError):
            self.store.remember('../../secrets', 'test', 'content')
        with self.assertRaises(ValueError):
            self.store.remember('fact', 'a', '   ')

    def test_archive_keeps_history_but_removes_from_search(self):
        self.store.initialize()
        m = self.store.remember('fact', 'Location', 'I once lived in Berlin.')
        self.assertTrue(self.store.archive(m.id))
        self.assertFalse(self.store.search('Berlin'))
        self.assertTrue((self.root / '90 Archive' / f'{m.id}.md').exists())
        self.assertFalse(self.store.archive(m.id))

    def test_no_symlink_escape_on_write(self):
        self.store.initialize()
        outside = Path(self.tmp.name) / 'other'
        outside.mkdir()
        folder = self.root / '01 Memories' / 'Facts'
        folder.rmdir()
        try:
            folder.symlink_to(outside, target_is_directory=True)
        except (OSError, NotImplementedError):
            self.skipTest('Symlinks not supported by environment')
        with self.assertRaises(ValueError):
            self.store.remember('fact', 'no', 'must not write outside')

    def test_search_excludes_system_files(self):
        self.store.initialize()
        self.assertEqual(self.store.search('immutable'), [])


if __name__ == '__main__':
    unittest.main()

class IdentitySecurityTests(unittest.TestCase):
    def test_identity_symlink_cannot_be_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)/'Vault'
            system = root/'00 System'
            system.mkdir(parents=True)
            external = Path(tmp)/'secret.txt'
            external.write_text('keep intact', encoding='utf-8')
            try:
                (system/'Identity.md').symlink_to(external)
            except (OSError, NotImplementedError):
                self.skipTest('Symlink not available')
            with self.assertRaises(ValueError):
                MemoryStore(root).initialize()
            self.assertEqual(external.read_text(encoding='utf-8'), 'keep intact')
