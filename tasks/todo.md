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
- [ ] **Checkpoint M1:** tests and lint pass ✅; waiting for the phone check: real library shows on the phone, first scan under 3 s, empty and permission states work

## M2: Real playback
- [ ] T6 Media3 `PlaybackService` and `MediaController` (notification, lock screen, Bluetooth, background)
- [ ] T7 Progress polling, seek, shuffle/repeat mapping, device volume
- [ ] T8 Corrupt file skips to the next with a Toast, no infinite loop
- [ ] T9 Noisy pause, ducking, previous restarts after 5 s, 30% speaker volume after unplug, fade-in on resume
- [ ] **Checkpoint M2:** device test covering playback, lock screen, Bluetooth, unplugging, a call, previous, and a corrupt file

## M3: Persistence
- [ ] T10 Likes, play counts and playlists saved to JSON (round-trip test), haptic feedback on like
- [ ] T11 Settings saved to `SharedPreferences` and wired into the service
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
