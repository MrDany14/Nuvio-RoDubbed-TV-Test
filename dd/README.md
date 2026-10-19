# DeseneleDublate OK.ru test

Adds Romanian-dubbed series from DeseneleDublate through OK.ru to the proven official v4 provider. Original and official repositories are unchanged. This is native **Nuvio TV**, not the mobile JavaScript installer.

Install repository:

```text
https://raw.githubusercontent.com/MrDany14/Nuvio-RoDubbed-TV-Test/main/dd/repo.json
```

Enable **RoDubbed [DD Test]** and temporarily disable other RoDubbed providers to identify the test results. Test **Bob Constructorul (2015), season1 episode1**, TMDB44396. Expected source: **OK.ru | RO Dub | DeseneleDublate**. The user confirmed Romanian playback on the website; this exact plugin build still needs Mi Box playback confirmation.

Other Android-validated samples: Polly Pocket S4E1, Pompierul Sam S14E1, Vida S1E1. Their spoken audio has not been listened to. All13 independently mapped title/episode addresses passed Android checks. One further candidate, Alex si trupa, remains unmapped due to metadata differences.

The resolver validates parent title/premiere year/dubbing, exact episode address and heading, OK.ru file ID/status/episode number/title, and matching full-episode MP4 duration in the actual media bytes. It returns native VIDEO links with the appropriate format. It does not execute ads. Language declarations and names cannot guarantee the actual spoken audio.

The total remains up to three streams, across DeseneFaine, DozaAnimata and DeseneleDublate. Existing Byse/SeekStreaming/StreamP2P regression passed. Netu, Streamtape (English-only tested copy), Sendvid and Playmogo are excluded.
