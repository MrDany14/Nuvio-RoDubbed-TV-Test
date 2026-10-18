# RoDubbed Byse Native TV Test

Fresh compiled Android extension for Nuvio TV's external/Cloudstream plugin runtime. Discovery, signing, proof-of-work and playback decryption run on the TV. No PC, server or browser needs to stay running.

## Install on Nuvio TV

Add this **new repository URL** in Settings → Content & Discovery → Plugins:

https://raw.githubusercontent.com/MrDany14/Nuvio-RoDubbed-TV-Test/main/native/repo.json

Enable **RoDubbed Byse Native [TV Test]**. Disable the older **RoDubbed Byse [TV Test] 2.0.11** while comparing results. SeekStreaming and StreamP2P can stay enabled.

Open **Aventura tenișilor / Sneaks (2025)** and select the native Byse source. Also try **Fallout S01E06**, whose Filemoon-labelled server is actually Byse. Test with these title pages: Nuvio's generic provider Test uses another title which may have no matching server in this catalog.

This native repository is separate from the existing JavaScript manifest. Refreshing the old manifest does not install it. It requires a Nuvio TV build with external extension support; compatibility with phone builds is not established. The existing mobile JavaScript provider remains available.

## What was verified

Test environment: official Nuvio TV 1.0.0 x86 APK inside Android TV 14 emulator. These are **emulator results**, not physical Mi Box measurements.

- Independent hash implementation passed 1,000 reference inputs from the public player's algorithm, including full output and first-word checks. The old JavaScript resolver was not used for these reference outputs.
- Compiled Android benchmark measured about 187,000 hashes/sec after warmup. Earlier QuickJS measurement on the same emulator was about 2,052 hashes/sec. A separate hash benchmark is not an end-to-end playback measurement.
- Native signing, required challenge verification, authenticated AES-GCM decryption, and HTTP 200 HLS playlist validation passed.
- Real app source discovery, with all four old JavaScript playback providers disabled: Sneaks returned one 1080p source in 967ms of discovery/resolution, plus 57ms metadata loading. Its challenge used 6,004 attempts and 47ms calculation time. Random challenges vary.
- Played that source through the real app. Playback advanced beyond 31 seconds, with approximately 81 seconds buffered and no playback error at that observation.
- Real app episode selection: Fallout S01E06 loaded the correct episode and returned one 1080p Byse source in 749ms of discovery/resolution, plus 91ms metadata loading. The Filemoon-labelled server was correctly resolved as Byse. Sustained playback of this episode was not separately tested.

No physical Mi Box result is available yet. Catalog discovery is new and may still need adjustments for other titles or site changes. Netu is outside this extension's scope. Existing JavaScript providers and their manifest are unchanged.

## Source and rebuild

Source is in `src/`. The extension uses Android cryptography and Nuvio's existing JSON, HTML and external-plugin APIs. `make-compile-stubs.cjs` creates compile-only public API signatures; these classes are deliberately excluded from the extension.

On Windows with Node.js and a JDK installed, enter `src/` and run:

```text
node prepare.cjs
node build.cjs "C:/path/to/jdk/bin"
```

Preparation downloads checksum-pinned Google R8, JSON and Jsoup compile dependencies. The build includes only the seven resolver/adapter classes and a plugin manifest. It does not bundle dependencies, API stubs or test harnesses. The build's ZIP timestamps may differ from the published artifact.

See `validation.json` for the tested artifact checksum and clean results. Raw app logs and expiring playback tokens are not published.
