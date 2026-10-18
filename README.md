# RoDubbed TV Test

Separate test repository. The original Nuvio-RoDubbed-plugin repository is unchanged.

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

Provider versions: StreamP2P 2.0.4, SeekStreaming 2.0.4, Netu 2.0.4, Byse 2.0.11. Repository manifest 4.3.0.
