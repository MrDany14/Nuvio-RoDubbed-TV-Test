# RoDubbed Doza Test

Native Nuvio TV test. The stable official repository is unchanged.

Install this repository URL on **Nuvio TV**, enable **RoDubbed [Doza Test]**, and temporarily disable other RoDubbed providers to avoid duplicate results:

```text
https://raw.githubusercontent.com/MrDany14/Nuvio-RoDubbed-TV-Test/main/doza/repo.json
```

## What to test

- **Un Show Obisnuit: Casetele Pierdute S01E01**: Android integration found Byse from DeseneFaine and Vidmoly from DozaAnimata. Play the Vidmoly result and confirm Romanian audio.
- **Wednesday S01E01**: DozaAnimata's Vidmoly source passed playlist and initial video-segment checks. The user confirms both website players have Romanian audio; please verify Vidmoly in Nuvio TV too.
- **Avatar: Aang The Last Airbender (2026)** on DozaAnimata is subtitled, so that website's sources are excluded.

The provider resolves DeseneFaine first and checks DozaAnimata only if fewer than three distinct sources were found. The total limit remains three across both websites. Doza series must match title/year and explicitly declare Romanian dubbing; the exact season/episode is selected from the matched series page. Dubbing declarations from related recommendations cannot qualify a title.

## Current limits

DozaAnimata support adds **Vidmoly**. **Playmogo is a DoodStream player**: its browser page loads, but direct Android and Node requests return HTTP403. It is excluded from this native test until a reliable on-device resolver is available. This is an access problem, not a Romanian-audio problem. Netu remains deferred and excluded.

All host, playlist and media checks run on the TV; no PC helper, browser cookie import or disabled TLS checks are used. Integration tests use actual Nuvio TV1.0.0 APK classes on Android ART. This exact test build still needs user playback confirmation on a physical TV.

Build with `node src/prepare.cjs` followed by `node src/build.cjs` (optionally pass your JDK bin path). Dependency downloads are checksum-pinned. Only explicit resolver classes and manifest are bundled; compile stubs, dependency JARs, test harnesses and private probe responses are excluded from the extension.
