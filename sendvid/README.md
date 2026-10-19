# Sendvid test withdrawn

**Update to test version 2, or disable this test provider and use the official RoDubbed provider.** Sendvid has been removed after the user confirmed that the returned video is a five-second clip saying the video is temporarily unavailable.

The earlier MP4-header validation accepted the valid MP4 error clip. It established container reachability, not the identity or availability of the requested episode.

Version 2 restores the proven official v4 resolver behavior under the existing test provider identity so installed test copies can update. Sendvid, Netu and Playmogo are excluded. The official repository and all other test providers are unchanged.
