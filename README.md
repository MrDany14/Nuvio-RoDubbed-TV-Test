# RoDubbed TV Test

Separate test repository. The original Nuvio-RoDubbed-plugin repository is unchanged.

## Unified native development build

The next test stage combines Byse, SeekStreaming and StreamP2P into one Romanian-only provider with a maximum of three validated sources. Netu is not yet verified working. Install the additional [unified repository](https://raw.githubusercontent.com/MrDany14/Nuvio-RoDubbed-TV-Test/main/unified/repo.json) and follow [unified/README.md](unified/README.md). The existing Byse-only build below remains available unchanged.

## New compiled Byse TV experiment

The fresh native Android resolver found and played Sneaks through Nuvio TV 1.0.0 in an emulator, with discovery/resolution around one second. Physical Mi Box testing is still needed. Install the separate native repository URL and follow the instructions in [native/README.md](native/README.md). It runs entirely on the TV; no helper server is required.

https://raw.githubusercontent.com/MrDany14/Nuvio-RoDubbed-TV-Test/main/native/repo.json

## Existing JavaScript test repository

Add this manifest in Nuvio:

https://raw.githubusercontent.com/MrDany14/Nuvio-RoDubbed-TV-Test/main/manifest.json

Disable the original repository's providers while testing to avoid duplicate searches.

## What changed

Each provider reads the title's server list and skips known unrelated servers. If no matching or unknown server remains, it returns immediately after title discovery. Unknown or conflicting labels remain eligible for wrapper inspection. Actual direct destinations override stale labels.

The site's old Filemoon label is retained as a Byse candidate. Its wrapper still must resolve to an actual Byse domain before the Byse authorization code runs. No stream is synthesized from the label.

On Aventura tenișilor / Sneaks (2025), Netu makes zero wrapper or provider requests. StreamP2P, SeekStreaming and Byse each inspect only their own wrapper. On Fallout S01E06, Byse recognizes the Filemoon-labeled Byse server; StreamP2P and SeekStreaming return without wrapper requests.

## Limits

Filtering begins once the correct title page has been found. Each independent plugin still performs its own metadata and title-page lookup; it cannot cancel another plugin or alter Nuvio's scheduling. Byse proof-of-work and Netu playback behavior are unchanged by this filtering change. Byse 2.0.11 includes the previous 2.0.9 performance candidate, which the user reported still failed on Mi Box. This is not a claim that Byse or Netu TV playback is fixed.

## Validation

Eight routing cases passed using actual title HTML and seven captured live wrappers. Additional cases cover unknown labels, conflicting labels, and direct destinations overriding labels. Eight routing cases also passed in WASM QuickJS using JavaScript extracted from Nuvio Android TV's PluginRuntime; native HTTP and DOM calls were modeled, and provider calls were intercepted before host work. No physical TV playback was verified. See validation.json.

Provider versions: StreamP2P 2.0.4, SeekStreaming 2.0.4, Netu 2.0.4, Byse 2.0.11. Repository manifest 4.3.1.

## Mi Box device check

Refresh this test repository, find **Byse Device Check [NOT VIDEO]**, and select **Test**. After it finishes, press Down once to bring the three reports into view and send a photo. They report signing support, custom-hash speed, and the last successful Byse API stage.

Nuvio may label these reports as "3 streams" with green checks. They are diagnostic reports, not playable video sources. The tool does not solve a full challenge or attempt playback. Its diagnostic media type excludes it from ordinary movie/TV searches; it runs entirely on the device.

The JavaScript manifest 4.3.1 adds only the diagnostic tool. The four JavaScript playback providers are unchanged, and this diagnostic is not a Mi Box playback fix. The tool was tested in the official Nuvio TV 1.0.0 x86 APK: signing succeeded, approximately 2,052 hashes/second, and details/attestation/challenge requests succeeded. These emulator measurements do not establish performance on a physical Mi Box. See device-check-validation.json.
