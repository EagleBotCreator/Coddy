# Coddy Assistant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a Windows-first personal assistant with durable Obsidian memories and reviewable memory evolution, then extend it with two-way personalized voice.

**Architecture:** OpenAI Agents SDK manages chat and SQLite session context; a restricted local Obsidian Markdown store manages approved knowledge. A separate model proposes memory updates but never writes them without user approval.

**Tech Stack:** Python 3.11+, OpenAI Agents SDK, SQLite, Obsidian Markdown, PowerShell System.Speech (optional).

**Spec:** `docs/superpowers/specs/2026-11-10-coddy-design.md`

## Global Constraints
- Keep the repository public-data-only: no API keys, memory vaults or chat databases.
- Restrict memory writes to allowlisted folders and IDs; no unrestricted agent file-write tools.
- Test local memory behavior without external APIs.
- Make API cost and cloud privacy boundaries explicit.

## Review Focus
- User memory containing prompt injection must never become a system instruction: covered by `test_agent.py`.
- Memory category/path traversal and symlink folders must be rejected: covered by `test_memory.py`.
- Malformed queue JSON should fail closed rather than erase proposals: covered by `test_pending.py`.
- Archived notes should no longer appear in search: covered by `test_memory.py`.
- AI memory suggestions must not write approved notes directly: covered by `test_curator.py`.

---

### Task 1: Local vault and memory storage
**Files:** `src/coddy/memory.py`, `tests/test_memory.py`

- [x] Write failure tests for create, read, archive, invalid category and symlink boundaries.
- [x] Run tests red before implementation.
- [x] Implement `MemoryStore.initialize()`, `remember()`, `search()`, `archive()` and `list_recent()`.
- [x] Run tests green.

### Task 2: Reviewable memory suggestions
**Files:** `src/coddy/pending.py`, `src/coddy/curator.py`, `tests/test_pending.py`, `tests/test_curator.py`

- [x] Write failure tests for proposal approval, rejection, duplicate detection, malformed queue and model extraction.
- [x] Implement `PendingQueue` and optional curator extraction to queue only.
- [x] Run tests green.

### Task 3: Assistant runtime and configurable identity
**Files:** `src/coddy/agent.py`, `src/coddy/config.py`, `tests/test_agent.py`, `tests/test_config.py`

- [x] Test persona/memory separation and configuration.
- [x] Implement SDK-backed agent with read-only memory search tool and SQLite session persistence.
- [ ] Validate one live API conversation after owner supplies an API key.

### Task 4: Windows CLI and basic speech output
**Files:** `src/coddy/cli.py`, `src/coddy/voice.py`, `README.md`

- [x] Add install, chat, search, memory approval/rejection, archive and Windows speech output commands.
- [ ] Verify speech with a real Windows voice device.
- [ ] Run a live chat and restart to prove API-backed session continuity.

### Task 5: Full personalized voice (future)
**Files (planned):** `src/coddy/live/`, `web/`, `tests/test_live.py`

- [ ] Add a localhost-only secure browser UI.
- [ ] Add GPT-Live two-way voice and interruption handling with session credentials kept out of browser source.
- [ ] Create and select a custom voice from a text prompt, subject to API availability.
- [ ] Delegate memory search to Coddy's existing backend; validate behavior in Portuguese, English and Spanish.

### Task 6: Advanced memory evolution (future)
**Files (planned):** `src/coddy/memory/semantic.py`, `src/coddy/memory/consolidate.py`

- [ ] Add optional embedding-based search and stable memory IDs.
- [ ] Implement user-reviewed supersession and contradiction resolution.
- [ ] Implement scheduled consolidation with audit history and undo support.
- [ ] Test backup/restore and large-vault retrieval quality.

### Task 7: Continuous integration
**Files:** `.github/workflows/tests.yml`

- [x] Run offline unit tests on pushes and pull requests.
- [ ] Confirm GitHub Actions results after PR creation.

## Execution handoff

MVP text and memory implementation can be reviewed and merged separately from future custom voice, which requires credentials and live-service testing.
