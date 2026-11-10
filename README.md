# Coddy

**Your personal AI assistant, with an evolving memory you own in Obsidian.**

Coddy is a **Windows-first Python CLI** built with the OpenAI Agents SDK. Conversations are saved locally in SQLite; approved long-term memories are stored as ordinary Markdown notes in a dedicated Obsidian vault. A separate optional memory curator proposes notes that require your approval before they become permanent memories.

> [!IMPORTANT]
> Coddy v0.1 is an **MVP**, not a finished voice companion. Chat, vault storage, memory search, approval queue, and optional Windows speech *output* are implemented. Continuous microphone conversation, prompt-generated custom GPT-Live voices, automatic semantic retrieval, and autonomous memory consolidation are future phases. No API key, private memories, or vault files belong in this public repository.

## Requirements

- Windows 10 or 11 (macOS/Linux text chat may work; speech output is Windows-only)
- Python 3.11+ (3.13 recommended)
- [Obsidian](https://obsidian.md/download), free for personal use
- [Git](https://git-scm.com/downloads/win) optional for cloning
- [OpenAI API account](https://platform.openai.com/) for `chat` and AI memory suggestions; API billed separately from ChatGPT subscriptions

**No separate MCP server or Obsidian plugin is needed for v0.1.** Coddy directly writes Markdown files to a vault you explicitly configure. The MCP interface can be added after basic memory reliability is established.

## Installation (Windows PowerShell)

```powershell
# Download this repo as a ZIP from GitHub, unzip, then open PowerShell inside that folder.
py -3.13 -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install -e .

# Choose an EMPTY or new folder, then open it as a vault in Obsidian.
$env:CODDY_VAULT_PATH = "$HOME\Documents\Coddy Memory"
.\.venv\Scripts\coddy.exe init

# Supply a real API key ONLY in your local terminal or secure secret store.
$env:OPENAI_API_KEY = Read-Host "Paste your OpenAI API key"
.\.venv\Scripts\coddy.exe chat
```

The `$env:` variables above last for the current PowerShell session. Set `CODDY_VAULT_PATH` again in a later terminal session, or create a private launch script outside the repository. Do not commit your key or store it in Obsidian.

If Python 3.13 is unavailable, use another compatible installed Python version and adjust the `py -3.13` command. You can also clone with `git clone https://github.com/EagleBotCreator/Coddy.git` instead of downloading a ZIP.

## Commands

```powershell
coddy init
coddy chat
coddy chat --speak    # Windows local voice output; not voice input
coddy voices          # List installed Windows voices
coddy remember preference "Editor" "I prefer VSCodium."
coddy search "VSCodium"
coddy recent
coddy review          # Pending AI-suggested memories
coddy approve <ID>    # Exact ID from `review`
coddy reject <ID>
coddy archive <MEMORY_ID>   # Keeps archived note, removes from active search
```

All commands read `CODDY_VAULT_PATH` except `coddy voices`. If the CLI is not on your PATH, run `\.venv\Scripts\coddy.exe` in PowerShell.

## Evolving memory

Coddy has two separate memory layers:

1. **Working memory:** the Agents SDK `SQLiteSession`, stored at `%USERPROFILE%\.coddy\conversations.db`. It persists conversation history across program restarts.
2. **Long-term memory:** Markdown notes in `Coddy Memory/01 Memories/` inside Obsidian. The assistant can search these notes but does not get an unrestricted filesystem or memory-write tool.

To enable **automatic memory proposals**, set this in PowerShell before chatting:

```powershell
$env:CODDY_AUTO_SUGGEST = 'true'
coddy chat
```

After each turn, a separate curator model proposes up to three explicitly stated, durable memories. These go to `%USERPROFILE%\.coddy\pending.json`, **not** directly into the vault. You can inspect them with `coddy review`, then approve or reject individually. Curator calls consume additional API tokens. The system deliberately does not autonomously revise Coddy's core identity or delete memories.

The allowed categories are `fact`, `preference`, `decision`, `experience`, `project`, and `lesson`. Notes have unique IDs and creation times. `archive` removes a memory from active search while preserving the file in `90 Archive`.

**Current search is lexical, not embedding-based:** it works for direct term matches and small vaults. Semantic retrieval, duplicate merging, contradiction resolution and versioned personality evolution are planned features.

## Personalization

Edit the Markdown file `00 System/Identity.md` in your Obsidian vault to change Coddy's persona. Restart chat after editing. This file is read only by the agent; the agent has no tool to modify it.

For Windows speech output, run `coddy voices` and choose an installed voice:

```powershell
$env:CODDY_VOICE = 'Microsoft David Desktop'
coddy chat --speak
```

The installed voice name varies by computer. This feature uses built-in Windows SAPI speech synthesis. A **genuinely custom prompt-generated voice requires the planned GPT-Live integration** and separate API access. There is no voice-cloning claim for this MVP.

## Security and privacy

- Keep the Obsidian vault and `.coddy` runtime data OUTSIDE the Git repository. This repo is public.
- Memory writes use only fixed, allowlisted categories and random IDs. System notes are not available to the agent's memory tools.
- The app rejects memory category traversal and symlink escapes from its memory folders.
- No memory candidate becomes permanent without explicit user approval; direct `remember` is itself an explicit save request.
- Vault data and relevant conversation content **are transmitted to OpenAI** when using cloud-based chat or curator requests. Local Markdown storage does not imply offline AI processing.
- Be cautious before storing secrets, sensitive health details or others' private information. Do not expose Coddy's local data directory via a public web server.
- The voice output feature is local Windows speech synthesis and does not add an audio API charge.

## Offline tests

```powershell
$env:PYTHONPATH = 'src'
py -3.13 -m unittest discover -s tests -v
```

The tests do not use an API key or touch your real Obsidian vault.

## Roadmap

See [`docs/superpowers/plans/2026-11-10-coddy-assistant.md`](docs/superpowers/plans/2026-11-10-coddy-assistant.md) for the implementation roadmap and acceptance criteria.
