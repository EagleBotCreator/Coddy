"""Configuration comes from environment variables, not repository files."""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Settings:
    vault_path: Path
    data_path: Path
    model: str = 'gpt-5.4-mini'
    session_id: str = 'personal'
    auto_suggest: bool = False
    voice_name: str = ''

    @classmethod
    def from_environment(cls) -> 'Settings':
        vault = os.environ.get('CODDY_VAULT_PATH', '').strip().strip('"')
        if not vault:
            raise ValueError('Set CODDY_VAULT_PATH to your dedicated Obsidian vault folder')
        data = os.environ.get('CODDY_DATA_PATH', '').strip()
        return cls(
            vault_path=Path(vault).expanduser().absolute(),
            data_path=Path(data).expanduser().absolute() if data else (Path.home() / '.coddy'),
            model=os.environ.get('CODDY_MODEL', 'gpt-5.4-mini').strip(),
            session_id=os.environ.get('CODDY_SESSION_ID', 'personal').strip(),
            auto_suggest=os.environ.get('CODDY_AUTO_SUGGEST', '').lower() in {'1', 'true', 'yes', 'on'},
            voice_name=os.environ.get('CODDY_VOICE', '').strip(),
        )
