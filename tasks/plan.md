# Harmonia: what to build after the UI

## Context

The UI is finished, but it only runs on seed data. Nothing scans the device, nothing plays sound and nothing is saved. Several UI callbacks are also still no-ops:
- `AddToPlaylistSheet.onDone` (`SongsScreen.kt:180`) closes the sheet without adding songs, and the merge path doesn't merge.
- "Remove from playlist" passes `{}` (`PlaylistsScreen.kt:281`).
- The song-menu "Add to playlist" rows only dismiss the menu (`Components.kt:408`).
- `DeleteFileDialog` only dismisses (`Components.kt:455`).
- Artist and album rows are `clickable { }` (`ExploreScreen.kt:97`).
- The Lyrics chip does nothing, the sleep timer is only a label, and none of the Settings toggles are wired to anything.
- `step()` walks the whole library, not the list the song was played from.
- ~~`CleanTagTest` doesn't exist~~ Correction: it does, as a class inside `ExampleUnitTest.kt`.

**Answer to "PRD or other ideas?"** Use the PRD for *what* to build, but not in its phase order. Build in dependency order:

1. Real library (phase 1).
2. Real playback. Phases 2, 4, 7 and 9 all depend on it.
3. Persistence (phase 8).
4. The remaining PRD features.
5. Polish (phases 5 and 6).

Add three things the PRD leaves out that any real player needs:
- Background playback with a notification, lock-screen and Bluetooth controls. PRD phase 7 assumes these exist.
- Resume the last session after a restart.
- Artist and album detail screens, because those taps currently go nowhere.

Defer or drop four PRD items that cost a lot for little return:
- **Track crossfade:** Media3 has no built-in crossfade, it would take two players, and it conflicts with gapless playback. Do a fade-in on resume instead.
- **Ducking by exactly 70%:** Media3 and Android 8+ already duck audio to about 20%. Don't fight that.
- **Bundled fallback fonts:** Android has shipped Noto CJK, Hangul, Cyrillic and Arabic since 5.0, and bundling them would add more than 10 MB.
- **`.lrc` sidecar files:** scoped storage on Android 11+ blocks reading non-media files, so start with embedded lyrics.

## Architecture decisions

- **Library holder:** a process-level `object Library` in `data/Library.kt`. It holds `mutableStateListOf<Song>` and `<Playlist>` and exposes `scan()`, `load()` and `save()`. It survives rotation, and the playback service can reach it. It is the "real repository" CLAUDE.md expects `HarmoniaApp` to wire in. There's no ViewModel, following the project's convention.
- **Song ids:** keep `Song.id: Int` as MediaStore `_ID.toInt()`, so no screen signatures change. Get the URI from `ContentUris.withAppendedId(EXTERNAL_CONTENT_URI, id)` instead of adding a field. Add a `ponytail:` comment saying to switch to Long if an id ever overflows Int.
- **Playback:** Media3 `ExoPlayer` running inside a `MediaSessionService`. This gives the notification, lock screen, Bluetooth buttons, audio focus, ducking and gapless playback for free. The UI talks to it through a `MediaController` built with `buildAsync()` plus `addListener` on the main executor, so no guava or coroutines adapter is needed. The only new dependencies are `media3-exoplayer` and `media3-session`.
- **Persistence:** no new dependency.
  - Likes, play counts and playlists: one JSON file in `filesDir`, written with the platform's `org.json`.
  - Settings and the resume state: `SharedPreferences`.
  - Add a `ponytail:` comment saying to move to Room if the library grows past about 10k songs or needs queries.
- **Permissions:** request `READ_MEDIA_AUDIO` on API 33+ and `READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`) below that, through `rememberLauncherForActivityResult(RequestPermission())` from activity-compose. The existing `PermissionEmptyState` doubles as the friendly explanation shown before the system prompt (PRD phase 1).

## Dependency graph

```
M0 finish in-memory wiring (T1–T3)   ← no new deps, touches only UI state
        │
M1 T4 permission ──► T5 MediaStore scan (Library object)
        │
M2 T6 PlaybackService + controller ──► T7 progress/seek/modes ──► T8 error-skip, T9 focus/noisy/prev
        │
M3 T10 JSON persistence    T11 settings prefs → service    T12 resume session (T6 + T10)
        │
M4 T13 sleep timer · T14 smart shuffle · T15 album art + lock-screen privacy · T16 delete file · T17 drag-select · T18 lyrics spike
        │
M5 T19 lite mode · T20 encoding repair + RTL audit · T21 (opt) Indonesian strings · T22 release checklist
```

## Tasks

Verification for every task: `./gradlew :app:testDebugUnitTest :app:assembleDebug`, plus `:app:lintDebug` at each checkpoint. There is no emulator, so the user runs the **release** build on a phone at each checkpoint.

### M0: Finish the in-memory behaviour (no new deps)

- **T1 Playlist mutations (S–M).**
  - Acceptance criteria:
    - "Add N songs" appends the selected ids to a new or existing playlist.
    - Merging into an existing name skips ids that are already there.
    - Song-menu "Add to playlist" and "Remove from playlist" work.
    - The play button on a playlist card starts playback instead of opening the playlist.
  - Put the logic in a pure `mergeInto(playlist, ids)` in `SampleData.kt`, with a unit test.
  - Files: `HarmoniaApp.kt`, `SongsScreen.kt`, `PlaylistsScreen.kt`, `Components.kt`, test.
- **T2 Queue context (S).**
  - `onPlay(song)` becomes `onPlay(song, queue)`. Next and previous walk the list the song was tapped from: the sorted and filtered Songs list, a playlist, or Liked.
  - Acceptance criteria: in a playlist, next goes to the playlist's next song, not the library's.
  - Files: `HarmoniaApp.kt` and the screens.
- **T3 Artist and album detail plus CleanTagTest (S–M).**
  - Make `PlaylistDetail` internal and reuse it for an artist or album, switched by local state the same way as `PlaylistsScreen`.
  - Add `CleanTagTest`, covering the seed cases: y2mate, `feat.`, `prod.`, brackets and underscores.
  - Files: `ExploreScreen.kt`, `PlaylistsScreen.kt`, `app/src/test/.../CleanTagTest.kt`.

**Checkpoint M0:** tests pass, and the user confirms playlist add, merge, remove and the artist drill-in on a phone.

### M1: Real library (PRD phase 1)

- **T4 Permission flow (S).**
  - Update the manifest permissions.
  - On first launch show `PermissionEmptyState`. "Grant" opens the system prompt.
  - If the user denies, the same screen stays. If the user denies permanently, the button opens app settings.
  - Files: `AndroidManifest.xml`, `HarmoniaApp.kt`, `Components.kt`.
- **T5 MediaStore scan (M).**
  - `Library.scan(context)` runs on `Dispatchers.IO` and reads `_ID, TITLE, ARTIST, ALBUM, YEAR, DURATION, DISPLAY_NAME` with `IS_MUSIC != 0 AND DURATION >= 30000`, which implements the Settings "Filters" row.
  - A pure mapping function handles missing tags: an empty title falls back to the file name without its extension (PRD phase 2), and `<unknown>` becomes "Unknown artist". Unit-test that function.
  - Settings "Rescan library" and pull-to-refresh on `NoMusicEmptyState` (M3 `PullToRefreshBox`) both call `scan`.
  - Seed data stays for `@Preview`s only.
  - Acceptance criteria:
    - The first scan finishes in under 3 s on the user's phone (PRD).
    - Rotation doesn't rescan.
  - Files: new `data/Library.kt`, `SampleData.kt`, `HarmoniaApp.kt`, `SettingsScreen.kt`, test.

**Checkpoint M1:** the real library shows up on the phone, and the empty and permission states work.

### M2: Real playback (core of PRD phases 2, 4, 7 and 9)

- **T6 PlaybackService and controller (M, highest risk, so it goes first).**
  - Add Media3 to `libs.versions.toml`, using the latest stable version.
  - Add `PlaybackService : MediaSessionService`. In the manifest, declare the service with `foregroundServiceType="mediaPlayback"` and add the `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissions.
  - `play(song, queue)` calls `setMediaItems` and then `play`. `isPlaying` and `activeId` come from `Player.Listener`.
  - Acceptance criteria:
    - Tapping a song plays audio.
    - Play and pause work from the app, the notification, the lock screen and Bluetooth.
    - Playback survives leaving the app.
  - Files: `libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, new `PlaybackService.kt`, `HarmoniaApp.kt`.
- **T7 Progress, seek and modes (S).**
  - Poll the position about every 500 ms while playing, for the mini player and the scrubber. Scrubbing calls `seekTo`.
  - Map shuffle and repeat 0/1/2 onto the controller.
  - The volume slider sets the device volume.
  - Files: `HarmoniaApp.kt`, `Player.kt`.
- **T8 Corrupt-file skip (S, PRD phase 2).**
  - `onPlayerError` skips to the next track and shows a Toast saying the song couldn't be played.
  - Stop after as many consecutive errors as there are songs in the queue, so there's no infinite loop.
  - File: `PlaybackService.kt`.
- **T9 Audio behaviour (S, PRD phases 4 and 7).**
  - `setHandleAudioBecomingNoisy(true)` and `handleAudioFocus = true` (ducking).
  - `setMaxSeekToPreviousPositionMs(5000)`: pressing previous after 5 s restarts the song.
  - After a pause caused by `PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY`, the next play on the speaker starts at `volume = 0.3f`, and volume is restored when headphones reconnect (`AudioDeviceCallback`).
  - Resuming fades in over about 1 s.
  - File: `PlaybackService.kt`.

**Checkpoint M2:** on the phone, test play, the lock screen, Bluetooth, unplugging headphones, an incoming call or notification, previous after 5 s, and a corrupt file.

### M3: Persistence (PRD phase 8, and phase 3 survives a restart)

- **T10 Likes, play counts and playlists saved (S–M).**
  - JSON round-trip functions with a unit test. Add `testImplementation("org.json:json")`, because the android.jar stub throws in JVM tests.
  - Save on every mutation and load before the scan.
  - Tapping the heart also does a `LocalHapticFeedback` tap (PRD phase 8).
  - Files: `Library.kt`, `HarmoniaApp.kt`, `Components.kt`, `build.gradle.kts`, test.
- **T11 Settings saved and wired (S).**
  - Store dark mode and the toggles in `SharedPreferences`.
  - The service listens for preference changes and applies pause-on-unplug and ducking.
  - Files: `SettingsScreen.kt`, `HarmoniaApp.kt`, `PlaybackService.kt`.
- **T12 Resume last session (S, beyond the PRD).**
  - Save the queue ids, index and position on pause or stop, and restore them paused on launch.
  - `MediaSession.Callback.onPlaybackResumption` lets Bluetooth play work after the process has died.
  - File: `PlaybackService.kt`.

**Checkpoint M3:** kill the app and reopen it. Likes, playlists, settings and the last song and position are all still there.

### M4: Remaining PRD features

- **T13 Sleep timer with a 60 s fade-out (S–M, PRD phase 7).**
  - A custom `SessionCommand` in the service: fade volume over the last 60 s, then pause. "End of track" uses `pauseAtEndOfMediaItems`.
  - Unit-test the pure `fadeVolume(msLeft)`.
- **T14 Smart shuffle (S, PRD phase 9).**
  - A pure `smartShuffle(songs, random)` spreads out songs by the same artist. Apply it with `setMediaItems`, keeping the current song first. Turning shuffle off restores the original order.
  - Tests:
    - Every song appears exactly once.
    - On `SEED_SONGS`, no two adjacent songs share an artist across 1000 seeds.
- **T15 Real album art and lock-screen privacy (M, PRD phases 2, 5 and 9).**
  - Load art with `loadThumbnail` on API 29+ and `MediaMetadataRetriever.embeddedPicture` below that, into an `LruCache` holding about 1/8 of memory.
  - On OOM or missing art, fall back to the gradient.
  - Set the artwork in the MediaMetadata only when the privacy toggle is off.
- **T16 Delete file (M, PRD phase 7).**
  - API 30+: `MediaStore.createDeleteRequest` launched through `StartIntentSenderForResult`.
  - API 29: `RecoverableSecurityException`.
  - API 24–28: `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion=28`) plus `ContentResolver.delete`.
  - On success, remove the song from the library, playlists and queue.
- **T17 Drag to select (S–M, PRD phase 3 "swipe").**
  - After a long press, dragging extends the selection, using `detectDragGesturesAfterLongPress` together with `LazyListState.layoutInfo`.
- **T18 Lyrics spike (timeboxed).**
  - Read embedded USLT or SYLT lyrics through Media3 metadata and wire up the Lyrics chip.
  - `.lrc` sidecars only through an optional SAF folder grant, and only if the user wants it.

**Checkpoint M4:** the user tests each feature on the phone.

### M5: Polish and release

- **T19 Light performance mode (S, PRD phase 5).**
  - A `LocalLiteMode` CompositionLocal turns off `enterAnimation`, shadows, the art "breathing" effect and the soundbar animation.
  - It defaults on when `ActivityManager.isLowRamDevice()` is true.
- **T20 Encoding repair and RTL audit (S–M, PRD phase 6).**
  - Repair mojibake in the scan mapping with `android.icu.text.CharsetDetector` (API 24+, no dependency). The detector is injected so the function can be unit-tested with a fake.
  - Check the scrubber, drag gestures and slides with `@Preview(locale = "ar")`.
- **T21 (optional) Indonesian translation.** Move hardcoded strings into `strings.xml` and add `values-in`. Do it one screen at a time.
- **T22 Release checklist.** Real keystore (the user's action), Play Console declarations for the foreground-service type and `READ_MEDIA_AUDIO`, a no-data privacy policy, and an R8 release smoke test.

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Nothing can be tested on a device from this machine | High | Keep tasks small, with a user device check at every checkpoint. Put logic in pure functions and unit-test those. |
| Media3 service and permission edge cases across API 24 to 37 | High | T6 goes first in M2. Rely on Media3 defaults and don't customise the notification. |
| Scoped-storage delete behaves differently on each API level | Med | T16 is isolated with three explicit branches. |
| Lyrics are blocked by storage rules | Med | Timeboxed spike, embedded lyrics first. |

## Open questions (defaults in brackets; they don't block M0 to M3)

- Crossfade on track change: drop it and keep only the fade-in on resume? [drop]
- The Settings toggles "Gapless" and "Crossfade" have nothing to control once Media3 is in (gapless is always on). Remove them? [remove]
- On API 30+ the system already shows its own delete confirmation. Skip the app's red dialog there, to avoid confirming twice? [skip on 30+]
- Should the UI be translated to Indonesian (T21)? [not now]

## After approval

Copy this plan to `tasks/plan.md` and the task checklist, with its checkpoints, to `tasks/todo.md`. Neither file exists yet. Then start M0 with `/build`.
