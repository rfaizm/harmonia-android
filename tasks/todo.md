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
- [ ] T12 Resume last session and `onPlaybackResumption`
- [ ] **Checkpoint M3:** after killing and reopening the app, everything is restored

## M4: Remaining PRD features
- [ ] T13 Sleep timer with a 60 s fade-out (`fadeVolume` test)
- [ ] T14 Smart shuffle (`smartShuffle` tests)
- [ ] T15 Real album art with gradient fallback, and the lock-screen privacy toggle
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
