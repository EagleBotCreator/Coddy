"""Optional Windows speech output using system-installed voices. No cloud audio."""

from __future__ import annotations

import base64
import os
import platform
import subprocess


def _run_powershell(script: str, text: str = '', voice: str = '') -> str:
    if platform.system() != 'Windows':
        raise RuntimeError('Windows SAPI voice is available only on Windows')
    # EncodedCommand encodes *fixed code*, not user text; text is supplied via stdin.
    command = base64.b64encode(script.encode('utf-16-le')).decode('ascii')
    env = dict(os.environ, CODDY_VOICE_NAME=voice)
    completed = subprocess.run(
        ['powershell.exe', '-NoProfile', '-NonInteractive', '-EncodedCommand', command],
        input=text, text=True, capture_output=True, timeout=90, check=True, env=env,
    )
    return completed.stdout.strip()


def list_windows_voices() -> list[str]:
    script = '''Add-Type -AssemblyName System.Speech
    $s = New-Object System.Speech.Synthesis.SpeechSynthesizer
    $s.GetInstalledVoices() | ForEach-Object { $_.VoiceInfo.Name }
    '''
    return [s.strip() for s in _run_powershell(script).splitlines() if s.strip()]


def speak(text: str, voice: str = '') -> None:
    if not text.strip():
        return
    script = '''Add-Type -AssemblyName System.Speech
    $speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer
    if ($env:CODDY_VOICE_NAME) { $speaker.SelectVoice($env:CODDY_VOICE_NAME) }
    $message = [Console]::In.ReadToEnd()
    $speaker.Speak($message)
    '''
    _run_powershell(script, text, voice)
