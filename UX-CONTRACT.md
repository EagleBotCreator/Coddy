# Coddy Android UX contract
## Product context
Personal Android product, API 26+, English UI with native locale date/time formatting. Assistant follows the user's language. Accessibility target WCAG 2.2 AA interpreted through Android semantics, 48dp targets and font scaling.
## Business-context sources
| Domain | Source | Reviewed |
|---|---|---|
| Memory categories and vault writes | src/coddy/memory.py, src/coddy/pending.py | 2026-11-15 |
| Android privacy and lifecycle | docs/superpowers/specs/2026-11-15-coddy-android-design.md | 2026-11-15 |
| Current build scope | Current user request to create Android side | 2026-11-15 |
## Visual contract
DESIGN.md owns intent; android/design/tokens.json owns exact runtime values, exported through scripts/generate_design_tokens.py.
## Canonical UI Map
| Capability | Canonical owner | Source of truth | Allowed variants | Verification |
|---|---|---|---|---|
| Form | Material3 OutlinedTextField + shared validation | Compose UI | text / masked / multiline | unit + device workflow |
| Select/Listbox | Material3 buttons and AlertDialog | Compose UI | local / desktop, category | native semantics |
| Date | Android locale formatter | core timestamps | read-only | unit |
| Scrollbar | Android/Compose scrolling | native LazyColumn / verticalScroll | phone / tablet | device workflow |
| Toast | Scaffold SnackbarHost | app shell | success/info | UI test |
| CRUD | AppViewModel and repository | domain state transitions | import, draft, review, reject | domain tests |
No data grid, bulk selection or browser URL-state contract applies.
## Flow ledger
| Operation | Pending | Success | Failure |
|---|---|---|---|
| Model import | Copy/load progress + cancel generation only | local model ready | prior file preserved; actionable message |
| Send chat | streaming response + Stop | persisted assistant message with source references | partial response retained, error shown, no automatic retry |
| Snapshot import | bounded parse then atomic commit | memory list, source timestamp | old snapshot preserved |
| Draft proposal | explicit save | separate proposal list | keep form and show validation |
| Review proposal | show exact title, content, source, importance, confidence | reviewed for export | no vault write |
| Reject proposal | explicit reject | retained rejected record, omitted from export | retain record |
| Export | Android CreateDocument | reviewed proposals only | original proposal state retained |
| Desktop test | explicitly requested network call | server accepted workspace request | remain on form, no token in error |
## Dataset navigation
Memory search is local, immediate, clearable, bounded to 2,000 imported records; render with LazyColumn. Source data is private, so no URL serialization. Keep query, drafts and scroll state in the app. Distinguish empty dataset from no matches.
## Navigation and resilience
Scaffold owns top/bottom insets. Separate forms scroll naturally; messages and memory lists own lazy scrolling. Input stays above the keyboard. Back dismisses dialogs before exiting. Persist drafts on field changes without writing the vault. No automatic request replay or provider switching.
## Security and sensitive values
Snapshots are imported through explicit file selection. Tokens are masked and Keystore-encrypted; backup disabled. Desktop mode is user-selected and discloses transmitted content. HTTP limited to numeric private/loopback hosts; HTTPS allowed, redirects prohibited. File import validates before commit. Notes are reference data, not executable instructions.
## Validation
Domain validators own bounds and categories. Save validates on submit, displays field/global errors, preserves typed input, blocks duplicate saves. Native Android document picker and native Compose modal focus are accepted platform owners.
## Overlays
Material3 AlertDialog is the app-owned confirmation/review primitive. Preview web page uses a real dialog element, keyboard focus and Escape. No native browser alert/confirm/prompt.
## Verification
Run Python tests, Kotlin core tests, assembleDebug, lintDebug, token drift and premium audit. Record actual emulator/native inference coverage. Compare all three tabs for labels, surface hierarchy and error recovery.
