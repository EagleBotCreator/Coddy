import tempfile
import unittest
from pathlib import Path

from coddy.memory import MemoryStore
from coddy.pending import PendingQueue


class PendingQueueTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.store = MemoryStore(Path(self.tmp.name) / 'Vault')
        self.store.initialize()
        self.queue = PendingQueue(Path(self.tmp.name) / 'data' / 'pending.json')

    def test_proposal_requires_approval(self):
        p = self.queue.propose('preference', 'Music', 'The user likes jazz.', source='conversation')
        self.assertEqual(self.store.search('jazz'), [])
        self.assertEqual(self.queue.list_pending()[0]['id'], p['id'])
        added = self.queue.approve(p['id'], self.store)
        self.assertEqual(self.store.search('jazz')[0].id, added.id)
        self.assertEqual(self.queue.list_pending(), [])

    def test_reject_discards_pending_candidate(self):
        p = self.queue.propose('fact', 'Tmp', 'This is unreliable.')
        self.assertTrue(self.queue.reject(p['id']))
        self.assertEqual(self.queue.list_pending(), [])

    def test_corrupt_queue_fails_closed(self):
        self.queue.path.parent.mkdir(parents=True, exist_ok=True)
        self.queue.path.write_text('not json', encoding='utf-8')
        with self.assertRaises(ValueError):
            self.queue.list_pending()

    def test_duplicate_proposal_not_readded(self):
        self.queue.propose('preference', 'A', 'B')
        self.queue.propose('preference', 'A', 'B')
        self.assertEqual(len(self.queue.list_pending()), 1)


if __name__ == '__main__':
    unittest.main()
