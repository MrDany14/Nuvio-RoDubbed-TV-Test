# DeseneleDublate OK.ru test

Install [this repository](https://raw.githubusercontent.com/MrDany14/Nuvio-RoDubbed-TV-Test/main/dd-okru/repo.json) alongside the official repository. This provider searches only DeseneleDublate and resolves only OK.ru. The official plugin is unchanged.

Please test Masca magică (2020), Degrassi S10E09 and Kirby Buckets/Kirby Desenatorul S03E08 on the TV, including Romanian speech. All three passed discovery, native VIDEO callbacks, audio/video metadata and decoding distinct frames at 30/60 seconds. Degrassi and Kirby passed muted Android MediaPlayer seek/playback after settling. Movie playback passed initially but failed a repeat's progress assertion; the emulator results are inconsistent and do not establish reliable physical-TV playback.

High School Musical (2006) supplied no usable player metadata and returns no link. H2O flattened episode lists remain excluded without a verified season map. No title URLs or video IDs are hardcoded in production.

Source code is in src/. Building requires the same json.jar, jsoup.jar and r8.jar as the official project; compile-only stubs must never be packaged.
