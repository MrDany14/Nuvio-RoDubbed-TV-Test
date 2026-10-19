# FilmeDublate native TV test

A separate **FilmeDublate-only** provider. The official plugin and previous test folders remain unchanged. DeseneleDublate work is paused.

Install in native Nuvio TV:

```text
https://raw.githubusercontent.com/MrDany14/Nuvio-RoDubbed-TV-Test/main/fd/repo.json
```

Enable **RoDubbed [FD Test]**. Temporarily disable other RoDubbed providers when testing so results are identifiable. This is a native Android TV extension, not a mobile JavaScript repository.

Start with **Toy Story 5 (2026)**: expected **Byse | RO Dub | FilmeDublate** and **Streamtape | RO Dub | FilmeDublate**. The user confirmed all website servers play in Romanian. This exact build still needs Mi Box playback confirmation.

Six Android title/episode checks passed, including **Ironheart season1 episode2**, which must not return the default episode1. Eight media links passed: five Byse HLS links and three Streamtape MP4 links. ToyStory5 full metadata-to-links lookup took862ms in the emulator; physical TV timings vary.

One Mario Streamtape copy was excluded because its104-minute media disagrees with the90-minute page. Byse remains available. Abyss and morencius are not implemented; Netu and Sendvid are excluded. Only Byse and Streamtape are enabled, so this test currently returns at most two hosts.

Titles must be declared dubbed, match the requested title/original title and year, and have an exact episode selector for series. Streamtape is enabled only with FilmeDublate provenance and matching full-length MP4 duration. These checks cannot establish the actual spoken language for untested titles. No ad scripts are executed.

Source and fixtures are included under src. Build with Node.js and a JDK: run node prepare.cjs for checksum-verified dependencies, then node build.cjs. Test classes and compile-only API stubs are excluded from the extension.
