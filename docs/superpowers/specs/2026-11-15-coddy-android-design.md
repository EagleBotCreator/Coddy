# Coddy Android design
Date: 2026-11-15. Implementation requested directly after the development checklist.
## Intent
Create the Android side of Coddy for personal use: offline conversation, readable approved memory, explicit proposals and optional desktop inference. The Windows CLI and vault stay intact.
## Grounding
The existing repository is a Python CLI, not AnythingLLM and not an HTTP server. Its allowlisted memory categories and pending queue are the compatibility boundary. No real vault, account token or conversation is included in source.
## Approach and alternatives
- Configure AnythingLLM Mobile: quickest, but standalone custom tools are not available.
- Native Android companion: chosen implementation default for this build request; direct file import, local inference, controlled proposal state and clear connection mode.
- Fork AnythingLLM Mobile: reuses its engine but brings an unrelated application and larger maintenance surface.
The choice is the implementation agent's, not a previously approved user design.
## Architecture
Add android/ as a standalone Kotlin/Jetpack Compose application. Use a pinned llama.cpp source release via CMake/JNI for CPU inference with user-imported instruction-tuned GGUF models. Include no model weights in the APK. Android 8+ (API 26), target/compile API 36, arm64-v8a and x86_64.
Default provider is local. Optional explicit AnythingLLM Desktop mode uses its documented workspace chat endpoint. Never fall back to cloud or another provider. The existing Python CLI does not acquire a server.
## Memory contract
Import a versioned coddy.snapshot JSON file containing user-selected approved memories. Validate the entire payload before replacing any snapshot; preserve the old snapshot on failure. Maximum 8 MiB, 2,000 memories, title 120 characters, content 12,000 characters. Allowlisted categories match src/coddy/memory.py. Require unique 32-character lowercase hexadecimal IDs, safe relative source paths, ISO timestamps and source metadata. Search locally and cite source notes. Treat all note text as untrusted data.
Proposals are separate, private, persistent drafts with source, importance and confidence. User review marks a proposal ready for export, not a permanent memory. Export only reviewed proposals as coddy.proposals JSON. A small offline Python helper merges them into the existing pending queue, never into the vault. Existing coddy approve remains the final vault write.
## Data and network
Private app files hold conversations, snapshots and proposals; disable OS/cloud backup. Encrypt the optional desktop access token with Android Keystore. Mask it in UI and never log it. Local mode performs no network calls. Explicit desktop requests disclose that message and relevant memory excerpts go to the configured host. Allow HTTP only to numeric loopback/private LAN addresses; HTTPS for other hosts. Disable redirects. Bound responses and set timeouts.
No automatic synchronization, credentials in files, automatic memory approval, identity editing, telemetry or fabricated responses.
## Interaction
Three primary tabs: Chat, Memory, Connect. Stable top connection label and clear empty/setup states. Import model with Android document picker; copy to private storage, validate GGUF magic and load asynchronously. Stream tokens, offer Stop and retain incomplete output as stopped. Display errors persistently with an actionable recovery. Imported memories are read-only. Keep drafts during navigation and restart.
## Visual direction
A coastal instrument notebook: mist blue, ink navy, steel blue, copper, white and evergreen; provenance rail ties memory to source. Native readable sans with geometric display treatment and monospaced metadata. No stock imagery. English UI; assistant follows Portuguese, English or Spanish. Design board is supplementary; code is the implementation.
## Verification and practical limits
Pure domain/unit tests cover malformed import, duplicates, traversal, secret-like values, proposal transitions, server URL policy and prompt boundaries. Python integration tests prove export/import does not alter the vault. Build and lint the APK. Exercise UI on an emulator if available; separately report whether real on-device inference was run. No claim of phone installation without device access.
