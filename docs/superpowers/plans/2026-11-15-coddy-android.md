# Coddy Android Implementation Plan
> **For agentic workers:** Use superpowers:executing-plans to implement this plan task by task.

**Goal:** Build an installable Android companion with local inference and approval-based memory transfer.
**Architecture:** Independent Kotlin application under android/, JNI llama.cpp runtime, app-private JSON state and optional AnythingLLM client. Add offline transfer tools without changing Windows runtime behavior.
**Tech Stack:** Kotlin 2.2.21, Compose, AGP 8.13.1, Gradle 8.13, SDK 36, NDK 28.2, llama.cpp pinned source.
**Spec:** ../specs/2026-11-15-coddy-android-design.md

## Global Constraints
Android API 26 minimum; compile/target 36; no bundled model or credentials; local provider default; read-only snapshots; vault writes require existing desktop approval; no automatic fallback or sync.

## Review Focus
- Corrupt, oversized or malicious snapshot preserves prior state.
- Rejected/unreviewed proposals never enter exported ready set.
- Private LAN HTTP policy cannot leak a token through redirects.
- Interrupted model import/generation does not destroy valid files or freeze UI.
- No real notes, paths, access tokens or user conversations enter the public commit.

## Task 1: Domain, persistence and transfer
Files: android/core/, android/tools/, tests/test_android_transfer.py.
Interfaces: SnapshotCodec.decode(text): Snapshot; MemorySearch.search(snapshot,query); ProposalWorkflow.review/reject/export; PromptBuilder.build(history,memories); DesktopEndpoint.validate.
- [x] Write behavioral unit tests and watch required behavior fail.
- [x] Implement validation, deterministic search, separate proposal workflow and bounded untrusted prompt context.
- [x] Add atomic snapshot/proposal storage integration.
- [x] Test read-only desktop export and pending-only proposal import.
- [x] Commit tested domain and transfer tools.

## Task 2: Model and desktop engines
Files: android/app/src/main/cpp/, android/app/src/main/java/.../inference/, .../data/.
Interfaces: LocalEngine.load(file), generate(messages,onToken), stop(); DesktopClient.chat(config,messages).
- [x] Test endpoint rejection, response error handling, cancellation and context bounds.
- [x] Implement pinned JNI runtime, token streaming and explicit abort.
- [x] Implement authenticated desktop client and Keystore token store.
- [x] Compile ARM64/x86_64 native targets; device execution recorded separately.
- [x] Commit engines.

## Task 3: Compose interface
Files: android/app/src/main/java/.../ui/, DESIGN.md, UX-CONTRACT.md, android/design/.
Interfaces: AppViewModel exposes immutable UI state and intent functions; shared native Material components own form/navigation/dialog semantics.
- [x] Establish design tokens, canonical UI behavior and a Canva reference.
- [x] Build Chat, Memory and Connect with truthful empty/loading/error states.
- [x] Add model import, snapshot import, read-only detail, reviewed proposal export and persisted drafts.
- [x] Exercise narrow layout, keyboard, accessibility labels and restart behavior on a running Android device/emulator.
- [x] Commit app and interface.

## Task 4: Build, review and delivery
Files: .github/workflows/android.yml, android/README.md, docs/.../verification.md.
- [x] Run Python regression suite, Kotlin unit suite, Android lint and assembleDebug.
- [x] Run the design audit and source/credential hygiene checks.
- [x] Request a fresh whole-change review and fix significant findings.
- [x] Push a dedicated feature branch, open a draft PR and check CI.
- [x] Deliver APK, GitHub review link, Canva board and honest verification limits.
