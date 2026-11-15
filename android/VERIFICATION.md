# Android verification — 2026-11-15

## Scope delivered
Native Kotlin/Compose companion with local llama.cpp GGUF inference, read-only approved-memory snapshots, reviewed proposal export, private app storage and optional AnythingLLM Desktop transport. Windows runtime files under src/coddy are unchanged.

## Checks completed
- Existing and new Python tests: 24 passed.
- Kotlin domain and HTTP transport tests: 16 passed.
- ARM64 and x86_64 native compilation and debug APK assembly: passed.
- Android instrumentation APK assembly: passed.
- Android lintDebug: passed with zero errors (dependency/style advisory warnings remain).
- Design token drift check: passed.
- Premium static UI contract audit: zero findings.
- DESIGN.md lint: zero errors; semantic-token reference advisories remain.
- git diff --check: passed.
- APK contains both native ABIs and the bundled llama.cpp license; no model weights or credentials.

## Review fixes
A fresh reviewer identified and the implementation addressed:
- AtomicFile backup recovery and Activity-independent serialized writes.
- Complete newest-message preservation with an explicit 4,000-character send limit.
- Exact UTF-8 snapshot size checks, including Windows newline behavior.
- In-memory connection forms, atomic encrypted-token/address persistence and destination binding.
- Saving-state protection and preservation of unsaved form edits.

## Device verification
The hosted API 35 x86_64 emulator completed **6 device tests**: chat/memory/proposal UI flow; persistence after ViewModel clearing; recovery of AtomicFile backups; invalid-model handling; native generation from a separately supplied GGUF fixture; and encryption bound to the selected desktop destination.

Verified run: https://github.com/EagleBotCreator/Coddy/actions/runs/43764806394

The local software emulator booted and installed the APK but its test harness/package service was unreliable. Hosted accelerated testing supplied the device evidence. Native kernels are additionally compiled with verified -O3 flags for the delivered personal-use APK. The PR workflow reruns these checks on code changes.

No physical Android phone or user's real Obsidian vault has been accessed. The fixture proves native inference execution; response quality and speed depend on the model and the phone.
