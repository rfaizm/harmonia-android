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
- [x] T16 Delete file: `ui/DeleteSong.kt` holds all three paths behind `rememberSongDeleter` (`deletePathFor` test). Our red dialog comes first on every version; `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion=28`) is asked for only when a delete is confirmed on Android 7–9, with a rationale and a settings link after a permanent denial. Deleting removes the song from the queue, library and every playlist (`removeSongEverywhere` test). **Android 7–10 paths are unverified — no emulator here**
- [x] T17 Batch selection: (a) search bar stays usable in selection mode, "Select all" adds the matches; (b) drag after a long press selects the rows the finger covers, with edge auto-scroll (`edgeScrollSpeed` test). The row's own long press is untouched, so tap-selection still works if the gesture misbehaves. **Gestures need a phone check**
- [x] T18 Lyrics: the Lyrics chip opens a sheet with the song's embedded ID3 `USLT` lyrics (`parseUslt` test), or an honest note when the file has none. The service extracts them because `Format.toBundle()` drops track metadata before it reaches a controller, and publishes them beside the sleep timer in the session extras. **Not covered: M4A/FLAC tags, synchronised `SYLT`, and `.lrc` files — see the spike's findings in `tasks/plan.md`**
- [x] T27 Swipe a row right-to-left to reveal Delete (Songs/artist/album) or Remove (inside a playlist), which still has to be tapped (`swipeSettlesOpen` test). One row open at a time; scrolling or entering selection closes it. The delete warning moved from `SongRow` to the screens, so the menu and the swipe share one dialog. **Gesture needs a phone check**
- [x] T28 Optional online lyrics via LRCLIB (`data/LyricsOnline.kt`; `lyricsFrom`, `stripLrcTimestamps`, `lyricsUrl` tests): off by default behind a Settings switch that asks first, fetched only when "Find lyrics online" is tapped, cached in `lyrics.json` so a song needs the network at most once. Offline promise reworded in the permission screen and About row. **T22 still has to cover it in the privacy policy and Data safety form**. Follow-up from `SPEC.md`: S1 (artist from the file name), S2 (the player's own artist tag) and S3 (a result is used only if its title matches and either its artist or its length does) — a tagless file no longer gets another song's lyrics (`LyricsMatchTest`)
- [x] T29 `.lrc` lyrics files beside songs (`lrcNameFor` test, reuses `stripLrcTimestamps`): Settings › Lyrics folder takes a one-time folder grant, and a `.lrc` matching the song's file name is read before anything online. `Song` now keeps its MediaStore file name. **Only the granted folder itself is searched, not subfolders**
- [ ] **Checkpoint M4:** user tests each feature on a phone

## M7: CI/CD on free services (before M6, so the refactor is checked on every push)
- [x] T34 Put the project on GitHub: `gradlew` executable, `.gitattributes`, secrets in `.gitignore`, the mixed commit split, every commit moved to the private noreply email, AI co-author lines removed, `main` up to date, live at github.com/rfaizm/harmonia-android
- [x] T35 CI on every push and pull request: tests, lint and a debug build on Java 25, reports kept on failure, README badge
- [x] T36 Real signing and version numbers: upload keystore made by the user and its 4 secrets added, signing from the `KEYSTORE_FILE`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD` env vars, version from `VERSION_TAG` (v1.2.3 → 1.2.3 / 10203), debug fallback locally
- [x] T37 Tag → signed `.aab` and `.apk` attached to a GitHub Release (first one: `v0.1.0`, versionCode 100, signed with the upload key)
- [ ] T38 Tag → Play internal testing (waits for the Play Console account)
- [ ] **Checkpoint M7:** CI green on a push (done); `v0.1.0` installs from its GitHub Release; after T38 it appears in Play internal testing

## M8: Synced lyrics (next, before M6)
- [ ] T39 Synced lyrics follow the song: `parseLrc` and `currentLine`; every source keeps its timings (online prefers synced, cache reset); the lyrics replace the art in the full player with the current line highlighted
- [ ] T40 Tap a line to play from it; scrolling by hand pauses the following for 3 s
- [ ] **Checkpoint M8:** phone check on the release build (follows the song, tap to jump, controls still work, plain and no-lyrics states)

## M6: MVVM migration (before M5 — T19 and T20 rework the same UI)
- [ ] T30 Tidy `data/`: split the models, the display helpers and the seed data; move `albumArtOf` into `ui/` (no behaviour change)
- [ ] T31 Hilt and data sources: prove KSP builds under AGP 9's built-in Kotlin first, then `data/local/` and `data/remote/` classes taking the context once
- [ ] T32 Repositories: `MusicRepository`, `LyricsRepository`, `SettingsRepository`; delete `object Library` and `object Settings`; the View stops calling the network
- [ ] T33 ViewModels: one per screen, `@HiltViewModel` + `StateFlow` + `collectAsStateWithLifecycle`; `PlayerViewModel` takes over the controller and most of `HarmoniaApp.kt`
- [ ] **Checkpoint M6:** full phone run of every feature from M0 to M4

## M5: Polish and release
- [ ] T19 Light performance mode (`LocalLiteMode`, on by default for low-RAM devices)
- [ ] T20 Mojibake repair (`CharsetDetector`, injected) and RTL audit
- [ ] T21 (optional) Indonesian strings
- [ ] T22 Release checklist (keystore, Play Console declarations, privacy policy, R8 smoke test)
- [ ] **Checkpoint: Complete**, ready for release review

## Loose ends (details in `tasks/plan.md`)
- [ ] T23 Adaptive launcher icon: note-only foreground inside the safe zone, dark slate background, real monochrome layer (before T22)
- [ ] T24 Delete the template leftovers (unused colours, backup-rule templates, instrumented test stub and its deps). **Keep `keepRules/rules.keep`: it now holds the Gson DTO rule**
- [x] T25 Git hygiene: the commit that mixed the shuffle fix with the icon layers is split in two (done in T34, before the first push); `main` catches up in T34
- [ ] T26 "Rate Harmonia" opens the Play Store listing, with a browser fallback (after T22)
