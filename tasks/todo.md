# Harmonia TODO

Full details are in `tasks/plan.md`. Verify each task with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## M0: Finish the in-memory behaviour
- [x] T1 Playlist mutations: add, merge, remove, song-menu add, card play button (pure `addSongs`/`removeSong` in `SampleData.kt`, `PlaylistTest`)
- [x] T2 Queue context: `onPlay(song, queue)`, next/prev walk the source list
- [x] T3 Artist/album detail (reuse `PlaylistDetail`); `CleanTagTest` already existed (class inside `ExampleUnitTest.kt`)
- [ ] **Checkpoint M0:** tests and lint pass ✅; waiting for the phone check: that playlist add/merge/remove and the artist drill-in work

## M1: Real library (PRD phase 1)
- [x] T4 Permission flow (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` ≤32, `PermissionEmptyState`, settings link after a permanent denial)
- [x] T5 MediaStore scan in `object Library` (filters, tag fallbacks plus a test, rescan, pull-to-refresh, no rescan on rotation)
- [x] **Checkpoint M1:** tests and lint pass ✅; phone check 2026-09-19: permission flow works and the list shows the phone's real songs

## M2: Real playback
- [x] T6 Media3 `PlaybackService` and `MediaController` (notification, lock screen, Bluetooth, background); play counts move to the service (`countPlay` test). Shuffle button is inert until T7
- [x] T7 Progress polling, seek, shuffle/repeat mapping, device volume (`PlayerTest`); T6 phone check passed 2026-09-19. Volume goes through `AudioManager` (Media3 controllers drop volume commands for local playback) and CBR seeking makes recorder `.aac`/`.amr` seekable; both confirmed on the phone
- [x] T8 Corrupt file skips to the next with a Toast, no infinite loop (`shouldSkipAfterError`, `PlaybackServiceTest`)
- [x] T9 Noisy pause, ducking, previous restarts after 5 s, 30% speaker volume after unplug, fade-in on resume (`speakerSafeVolume` test). T8 phone check passed 2026-09-20. The 30% caps the phone's speaker volume (Android keeps it separate from headphones) instead of player gain, so nothing needs restoring
- [x] **Checkpoint M2:** phone check passed 2026-09-20. Fixed along the way: volume via `AudioManager`, CBR seeking for recorder files, and the 30% cap no longer lowering headphone volume after a replug

## M3: Persistence
- [x] T10 Likes, play counts and playlists saved to JSON in `filesDir` (`stateJson`/`parseState` round-trip test, corrupt file reads as empty), haptic on like. `Library.restore()` runs before the first scan; `Library.save()` after every like, play count and playlist change
- [x] T11 Settings saved to `SharedPreferences` (`data/Settings.kt`) and wired into the service: ducking flips the audio content type (speech makes Media3 pause instead of duck, `audioAttributesFor` test), pause-on-unplug flips `setHandleAudioBecomingNoisy`. Crossfade and gapless switches removed (plan's open question). Smart shuffle, lite mode and lock privacy are stored for T14/T19/T15
- [x] T12 Resume last session and `onPlaybackResumption` (`resumeQueue` test keeps the index on the same song after a rescan). Restored idle, so no notification appears until play. `Library.ensureLoaded` shares one scan between the UI and the service; a scan without permission no longer marks the library loaded
- [x] **Checkpoint M3:** phone check passed 2026-09-20

## M4: Remaining PRD features
- [x] T13 Sleep timer with a 60 s fade-out (`fadeVolume` test). A custom `SessionCommand` carries the choice to the service, which owns the timer; "End of track" uses `pauseAtEndOfMediaItems` and clears itself. Resume fade-in and sleep fade-out share one target volume so they don't fight. The service publishes the running timer as session extras, so the chip resets to Off when it fires and shows a timer that is still running after the player is reopened
- [x] T14 Smart shuffle (`smartShuffle` tests: every song once, no same artist twice in a row over 1000 seeds, starts on the playing song). Handed to Media3 as a `ShuffleOrder`, so switching shuffle off restores the queue order for free; the Settings switch picks smart or plain random and applies live. Ordering is idempotent (`queueSignature`): Media3 reports our own new order as another playlist change and delivers it after the call returns, so a re-entry flag ANRed the app on the first shuffle tap
- [x] T15 Real album art with gradient fallback (`data/AlbumArt.kt`: `loadThumbnail` on API 29+, `MediaMetadataRetriever` below, `LruCache` of an eighth of the heap, songs without art remembered so scrolling doesn't reopen them, `sampleSizeFor` test), and the lock-screen privacy toggle (a `ForwardingPlayer` hides artwork from controllers; `withoutArtwork` test). The notification's art comes from Media3's own metadata extraction
- [ ] T16 Delete file (API 30+ / 29 / 24–28 branches)
- [ ] T17 Drag to select after a long press
- [ ] T18 Lyrics spike, embedded lyrics first (timeboxed)
- [ ] **Checkpoint M4:** user tests each feature on a phone

## M5: Polish and release
- [ ] T19 Light performance mode (`LocalLiteMode`, on by default for low-RAM devices)
- [ ] T20 Mojibake repair (`CharsetDetector`, injected) and RTL audit
- [ ] T21 (optional) Indonesian strings
- [ ] T22 Release checklist (keystore, Play Console declarations, privacy policy, R8 smoke test)
- [ ] **Checkpoint: Complete**, ready for release review

## Loose ends (details in `tasks/plan.md`)
- [ ] T23 Adaptive launcher icon: note-only foreground inside the safe zone, dark slate background, real monochrome layer (before T22)
- [ ] T24 Delete the template leftovers (~120 lines: unused colours, keep-rules file, backup-rule templates, instrumented test stub and its deps)
- [ ] T25 Git hygiene: split `12cb14e` (shuffle fix + icon layers), rename `m2-playback` or merge it into `main`
- [ ] T26 "Rate Harmonia" opens the Play Store listing, with a browser fallback (after T22)
