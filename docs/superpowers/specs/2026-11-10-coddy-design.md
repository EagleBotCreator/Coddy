# Coddy assistant design (v0.1)

## Purpose
Provide a Windows-first, personal assistant named Coddy that can chat through OpenAI, maintain short-term session continuity, read approved Obsidian memories, and let the owner inspect and approve new long-term memory candidates.

## Initial approach
Selected implementation: Python CLI + OpenAI Agents SDK + SQLite session history + directly managed, human-readable Obsidian Markdown vault. This is easier to test and secure than adding an MCP server and voice stack immediately.

## Alternatives considered
- **Obsidian MCP at launch:** provides broader note tools, but increases setup complexity and permission exposure. Deferred.
- **Fully local LLM:** removes cloud API dependency but introduces hardware and model management, distracts from durable memory. Deferred.
- **Live voice from day one:** complex WebRTC/session management. Deferred in favor of Windows output speech and a proven text+memory foundation.

## Required behavior
- Session history persists across restarts in a local SQLite file.
- Approved memories survive restarts as Obsidian Markdown notes, in allowed folders only.
- Search returns relevant approved memories; archived memories do not appear as active.
- All AI-extracted memory proposals require explicit user approval; no unattended edits to identity.
- Persona customization is by a protected identity note.
- Windows installed voice can speak replies optionally; microphone and custom voice still need later work.
- Credentials, conversation DB, queue and vault notes stay outside the public GitHub repo.

## Non-goals for v0.1
No mic input, GPT-Live session, embeddings, autonomous file editing, cross-device sync or seamless phone app. No unverified claims about model calls without a supplied API key.
