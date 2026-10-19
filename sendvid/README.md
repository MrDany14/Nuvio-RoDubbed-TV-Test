# RoDubbed Sendvid test

This isolated test adds Sendvid MP4 support to official v4. The official repository remains unchanged. Existing test builds are preserved.

Install this repository in **Nuvio TV**:

```text
https://raw.githubusercontent.com/MrDany14/Nuvio-RoDubbed-TV-Test/main/sendvid/repo.json
```

Enable **RoDubbed [Sendvid Test]** and temporarily disable other RoDubbed providers to identify the test result. Search **Tiny Toons Looniversity** (Romanian title **Looniversitatea Micuților**), season 1, episode 8. The source should be **Sendvid | RO Dub | DozaAnimata**. Check that it plays and that the spoken audio is Romanian.

The native Android integration test found exactly one supported source for that episode in about 3 seconds. It fetched a fresh signed MP4 address, validated the first 4096 bytes, and built a native VIDEO link. Actual Mi Box playback has not been verified. Signed URLs are fetched fresh on each lookup.

Existing Toy Story 5 Byse, SeekStreaming and StreamP2P sources still pass playlist/initial-segment tests. The provider still returns at most three sources across DeseneFaine and DozaAnimata. Netu and Playmogo remain excluded.

Only Sendvid’s own player video element and matching media-file identity are accepted; advertising scripts are not executed. MP4 responses are validated separately from HLS.
