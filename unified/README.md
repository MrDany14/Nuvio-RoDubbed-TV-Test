# RoDubbed Unified Native Test

One native Nuvio TV provider with shared catalog discovery and up to three distinct, validated Romanian-labelled sources. This is a development test, not the final production release.

## Install and test

Add this new repository URL **on Nuvio TV**:

https://raw.githubusercontent.com/MrDany14/Nuvio-RoDubbed-TV-Test/main/unified/repo.json

Enable **RoDubbed [Unified Native Test]**. Temporarily disable the separate old Byse Native, SeekStreaming and StreamP2P providers while testing, so their results do not mix with this provider. They can be re-enabled as fallback; their published files remain unchanged.

Open **Toy Story 5 / Povestea jucăriilor 5 (2026)**. This test should offer Byse, SeekStreaming and StreamP2P. Play each source separately and listen long enough to confirm Romanian audio. Report the title and host if a source fails, has English audio, or takes unusually long.

Also try **Aventura tenișilor / Sneaks (2025)**: only Byse currently works there. The other hosts return HTTP 404 and should not appear as sources. Fewer than three results is correct when the other links are unavailable.

## Behavior

- Catalog: DeseneFaine only in this first stage. Other website adapters come later.
- Language: explicit Romanian server declarations are required. English, subtitled-only and unknown server labels are skipped. Host/HLS metadata that explicitly defaults to non-Romanian audio is rejected. Multi-audio playlists with a foreign default are excluded until reliable Romanian auto-selection is implemented. Source declarations are metadata, not automated recognition of spoken audio; TV listening tests remain necessary.
- Shared discovery: look up the movie/episode once, then inspect only eligible host wrappers. The old Filemoon label is still resolved to the actual Byse destination.
- Native hosts: Byse, SeekStreaming and StreamP2P. SeekStreaming/StreamP2P can try alternative streaming backends if an initial backend is unavailable or has an invalid certificate. TLS verification is always retained.
- Validation: check HLS playlist and initial media-segment access before returning a result. These checks establish media availability, not every device's decoder compatibility.
- Results: at most three distinct embedded videos; host work is concurrent, unnecessary work is deferred/cancelled. Other resolutions of the same embedded video do not count as separate backups.
- Everything runs on the TV; no helper server is required.

## Netu status

Netu discovery and candidate resolver code are present but **working Netu playback is not verified**. The supplied Fallout S01E06 Netu player currently says its file was removed. It is excluded, and Byse still resolves. A live Netu example is needed before claiming Netu support or making a production release.

## Validation and source

The Android integration harness uses actual classes from the official Nuvio TV 1.0.0 x86 APK. It returned three validated Toy Story 5 sources in 1,383ms in the final candidate. Sneaks returned one source and Fallout S01E06 preserved Filemoon-to-Byse routing in earlier candidates. These are emulator/API-integration results; this unified build still needs installed-app and physical TV playback testing. See `validation.json`.

Sources, clean reference fixtures and a build script are in `src/`. On Windows with Node.js and a JDK:

```text
node prepare.cjs
node build.cjs "C:/path/to/jdk/bin"
```

Dependencies are checksum-pinned compile tools/libraries. Runtime stubs and test harnesses are excluded from the extension; private host responses, signed links and raw app logs are not published.
