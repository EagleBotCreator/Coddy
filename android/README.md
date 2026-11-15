# Coddy for Android

A native, local-first Android companion to Coddy. Chat with an on-device GGUF model, carry a read-only copy of approved memories and prepare memory proposals for explicit desktop review.

## Install

Use the APK attached to the development delivery or the **Coddy-Android-debug** artifact from this branch's GitHub Actions run. Open the APK on Android and follow the system installation prompt.

This is a personal-use debug build for Android 8+; no Play Store publication, model weights or account credentials are included. ARM64 phones and x86_64 test devices are supported. It has no advertisements or telemetry.

## Start local chat

1. Obtain an instruction-tuned **GGUF** model from its publisher or a trusted model distributor. Start small: approximately 0.5–1.5B parameters, quantized. Check that the model's license permits your use.
2. In Coddy, open **Connect → Choose GGUF model** and select the downloaded file.
3. Wait for **Model loaded**. Import makes a private copy, so leave space for both copies during setup.
4. Open **Chat**. Local mode never falls back to an online provider.

Model inference uses CPU llama.cpp. Larger models consume more memory and may be slow; an unsupported chat template produces an error instead of a fabricated answer. The initial context is 2,048 tokens with up to 384 generated tokens. If a conversation exceeds the context, shorten the message or explicitly clear the chat.

No voice assistant, background listening, live vault synchronization or autonomous phone control is claimed for this version.

## Bring approved memories from Windows

The transfer helper is part of this repository and uses the existing Coddy memory parser. It reads approved notes only, excludes system/identity and archive folders, and does not initialize or modify the vault.

From the repository root in PowerShell:

```powershell
$env:PYTHONPATH = 'src'
py -m android.tools.transfer export --vault "$HOME\Documents\Coddy Memory" --output "$HOME\Desktop\coddy-snapshot.json" --all
```

To export selected notes, replace `--all` with repeated `--id MEMORY_ID` arguments. Choose a new output filename each time. Transfer the JSON to Android using your preferred local file-transfer method, then choose **Memory → Import snapshot** and review the count/date before confirming.

A snapshot is a point-in-time copy, not live synchronization. Source paths are relative note references; the phone does not access arbitrary desktop files. Imported notes remain read-only.

## Propose memories from the phone

1. Open **Memory → Proposals → New proposal**.
2. Enter the exact proposed text, source, importance and confidence.
3. Save the draft, then **Review proposal → Mark reviewed**.
4. Choose **Export reviewed** and transfer that JSON to Windows.
5. Close the desktop CLI while importing, then run:

```powershell
$env:PYTHONPATH = 'src'
py -m android.tools.transfer import --file "$HOME\Downloads\coddy-reviewed-proposals.json" --pending "$HOME\.coddy\pending.json"
coddy review
coddy approve PROPOSAL_ID
```

Phone review only permits export. The final `coddy approve` command is the separate, explicit action that writes the vault. Rejecting a phone proposal excludes it from export. Import receipts are retained next to the desktop queue to avoid re-importing the same Android ID after review; keep them with your queue backup.

Credential-like values are rejected as a guardrail; review the complete content yourself before exporting or approving. Pattern detection cannot recognize every possible secret.

## Optional AnythingLLM Desktop connection

The current Python Coddy CLI is **not** a phone server. The optional connection here targets an independently configured AnythingLLM instance.

- Enable AnythingLLM's network discovery, use a local model and configure the Coddy workspace.
- Enter its base address, workspace slug and API access token in **Connect**.
- **Save** stores the token encrypted with Android Keystore.
- **Test connection** checks access to the chosen workspace.
- Select **Use desktop for chat** explicitly.

Desktop mode sends the message, bounded chat context and relevant imported memory excerpts to the configured instance. That instance's own model/provider settings determine whether it processes locally or uses a cloud provider. The computer must be reachable. No provider switching or automatic retry occurs.

Plain HTTP is permitted only to numeric private/loopback addresses. Use HTTPS for other hosts. Redirects are blocked so the token cannot be forwarded to a different address.

## Storage and recovery

Conversations, drafts and imported snapshots are stored privately inside the app. OS/cloud backup is disabled. The original imported documents stay in the location you chose. Keep the desktop vault and exported files as your durable copies.

If saved app data is corrupt, the app reports it and blocks writes instead of replacing it with an empty state. Keep your original export files before clearing Android app storage. Reimport snapshots/models after resetting; clearing app storage removes local conversations, proposals and connection settings.

Debug builds made on different machines can have different signing keys. An update needs the same signing key; retain your exports before uninstalling a development build.

## Build and checks

Requires JDK 21 and Android SDK 36. Gradle downloads the pinned build dependencies; CMake downloads a SHA-256-verified llama.cpp source archive. No model is downloaded by the build.

```bash
./android/gradlew -p android :core:test :app:assembleDebug :app:lintDebug
PYTHONPATH=src python3 -m unittest discover -s tests -v
python3 scripts/generate_design_tokens.py --check
# With an isolated Android emulator/device:
./android/gradlew -p android :app:connectedDebugAndroidTest
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

The design system is documented in [DESIGN.md](../DESIGN.md) and [UX-CONTRACT.md](../UX-CONTRACT.md). The [Canva reference board](https://canva.link/bcimvte0shvqrsg) is editable design material; the APK is the application.

## Licensing

New Android companion code and transfer helpers are MIT-licensed; see [LICENSE](LICENSE). The existing repository's code retains its existing terms. llama.cpp/ggml use MIT licenses. AndroidX, Kotlin and kotlinx libraries use their respective Apache 2.0 licenses. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
