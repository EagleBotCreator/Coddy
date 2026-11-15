# Coddy ChatGPT Project — automatic routines

This bundle contains a **Project Instructions** router and 10 small reference procedures. It is designed for a normal ChatGPT Project with optional access to the Coddy Obsidian vault through Remote Desktop Commander.

## Installation
1. Open the **Coddy** ChatGPT Project → project settings → instructions. **Merge or append** `PROJECT_INSTRUCTIONS.md` to the existing Coddy Core Instructions. Do not remove the core identity or existing privacy safeguards.
2. Upload the ten Markdown files in `skills/` as Project reference files, if the Project file interface supports them. You may also keep them in GitHub as documentation. Reference files **do not automatically execute**. The router in Project Instructions tells Coddy when to consult them.
3. Keep `C:\Users\Matt\Documents\Coddy Memory` outside the public GitHub repository. Only access it through an authorized connected tool.
4. Test with three example prompts: 'What did we decide about Coddy's memory?' (retrieval), 'I prefer concise technical answers' (propose memory, no silent write), and 'I no longer prefer X' (contradiction proposal). Check no memories were modified without approval.

## Important limits
- ChatGPT Project instructions do not create a service that runs continuously or guaranteed per-turn hooks. Automation here means **automatic behavior selection during active conversations**, not autonomous scheduled execution.
- Full review of all past conversations or the entire vault is not guaranteed. Access varies with context and connected tools.
- External actions, voice cloning, and background jobs require their own supported tools and configuration.
- No executable Python or PowerShell scripts are included in this bundle, because they cannot be triggered directly by a Project file alone. Build those separately if scheduled computer-side maintenance is desired.
