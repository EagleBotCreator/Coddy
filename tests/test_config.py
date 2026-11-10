import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from coddy.config import Settings


class SettingsTests(unittest.TestCase):
    def test_settings_from_environment(self):
        with tempfile.TemporaryDirectory() as tmp:
            with patch.dict('os.environ', {'CODDY_VAULT_PATH': str(Path(tmp)/'Vault'), 'CODDY_MODEL': 'gpt-5.4-mini', 'CODDY_AUTO_SUGGEST': 'true', 'CODDY_VOICE': 'Test Voice'}):
                s = Settings.from_environment()
                self.assertEqual(s.vault_path, Path(tmp)/'Vault')
                self.assertEqual(s.model, 'gpt-5.4-mini')
                self.assertTrue(s.auto_suggest)
                self.assertEqual(s.voice_name, 'Test Voice')

    def test_no_vault_path_does_not_invent_one(self):
        with patch.dict('os.environ', {'CODDY_VAULT_PATH': ''}):
            with self.assertRaises(ValueError):
                Settings.from_environment()


if __name__ == '__main__':
    unittest.main()
