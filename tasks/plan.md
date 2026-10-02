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
M4 T13 sleep timer · T14 smart shuffle · T15 album art + lock-screen privacy · T16 delete file · T17 batch selection · T18 lyrics spike · T27 row swipe
        │
M7 T34 GitHub · T35 CI · T36 signing · T37 tag release · T38 Play upload   ← before M6
        │
M8 T39 synced lyrics follow the song ──► T40 tap a line to play from it   ← next, before M6
        │
M9 T41 A–Z index bar jumps to a letter ──► T42 letter bubble + current-letter highlight   ← after M8
        │
M6 T30 tidy data · T31 Hilt + data sources · T32 repositories · T33 ViewModels   ← before M5
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
- **T17 Batch selection: search while selecting, then drag to select (S–M, PRD phase 3).**
  - PRD phase 3 asks for "menggeser (*swipe*) atau *tap* untuk memilih lagu": tapping works today, the swipe half
    doesn't. Selection mode also replaces the whole header, so the search bar disappears exactly when it is needed
    and the next song to add has to be found by scrolling the library by hand.
  - Two halves, shippable as two commits; the search one is small and lands first.
  - **(a) Search stays available while selecting.**
    - The search bar stays on screen in selection mode; only the row beneath it swaps between the sort pills and
      the selection controls, so the bar never moves as the mode changes.
    - Acceptance criteria:
      - Long-press a song, then type in the search bar: the list filters and the selection is kept.
      - Songs picked under one query stay selected after the query changes or is cleared, and the count and the
        "Add N songs" button count all of them together.
      - "Select all" adds what the search is currently showing to the existing selection instead of replacing it
        with the whole library, and reads "Select matches" while a query is active.
      - Tapping a row still toggles selection instead of playing, and Back still clears the selection.
  - **(b) Drag to select.**
    - After the long press, keeping the finger down and sliding selects every row it passes, via
      `detectDragGesturesAfterLongPress` with `LazyListState.layoutInfo` to map the finger's Y position to a row.
    - Acceptance criteria:
      - Dragging away from the long-pressed row selects each row passed; dragging back over them de-selects again.
      - Dragging to the top or bottom edge scrolls the list, so a drag can continue past one screenful.
      - Letter headers are passed over, never selected.
      - The list doesn't scroll by itself during a selection drag, and normal scrolling still works without a long
        press.
  - Files: `ui/SongsScreen.kt` (header layout, `SelectionBar`, the drag modifier); `ui/Components.kt` only if a row
    has to report its bounds.
  - Verification: gestures and layout are out of reach of JVM tests, so this one is checked on the phone against
    the criteria above, with a library long enough to need scrolling.
- **T27 Swipe a row for its safe action (S–M, after T17).**
  - Deleting or removing a song takes three taps today: the three-dot menu, the row, then the confirmation. A
    swipe puts the common action one gesture away without hiding the menu.
  - Decisions taken with the user:
    - **Swipe right-to-left**, not left-to-right: a rightward swipe near the left edge is Android's back gesture,
      and `systemGestureExclusion` only ever wins part of that fight.
    - **The swipe reveals an action button that has to be tapped**, it does not fire on release. Two deliberate
      actions, so a swipe in a pocket or mid-scroll can't start deleting music.
    - **The action depends on the screen**: inside a playlist it removes the song from that playlist and never
      touches the file; on Songs, artist and album screens it deletes the file through the T16 flow. That keeps
      PRD phase 7's rule that removing and deleting stay clearly apart.
  - Built on Material 3's `SwipeToDismissBox` / `AnchoredDraggable`, which are already on the classpath, so no new
    dependency and no hand-written gesture maths. One opt-in annotation if the API is still experimental.
  - Acceptance criteria:
    - Swiping a row from right to left slides it aside and holds it open, showing a red "Delete" or a neutral
      "Remove" button depending on the screen.
    - The row closes when the action is tapped, when the row itself is tapped, when the list scrolls, or when it is
      swiped back. Only one row stays open at a time.
    - "Delete" opens the same red confirmation as the menu, then the T16 delete flow. "Remove" takes the song out
      of that playlist only, and the file is untouched.
    - Swiping does nothing while selection mode is on, so it can't fight the T17 drag.
    - A half swipe that is released springs back, and the list still scrolls normally.
    - The three-dot menu keeps both actions, so nothing is only reachable by gesture.
  - Files: `ui/Components.kt` (a swipe wrapper around `SongRow`), `ui/SongsScreen.kt`, `ui/PlaylistsScreen.kt`
    (`PlaylistDetail` passes its remove action), `ui/ExploreScreen.kt`.
  - Verification: gestures are out of reach of JVM tests, so this is a phone check against the criteria above. The
    actions underneath are already covered: T16's tests for deleting, `removeSong` for playlists. The RTL mirror of
    the gesture is checked in T20's audit.
- **T18 Lyrics spike (timeboxed). Done; what the spike found:**
  - Media3 has no lyrics field and no USLT frame type, so unknown ID3 frames arrive as `BinaryFrame` raw bytes.
    `parseUslt` reads the payload (encoding byte, language, description, text) and is unit-tested.
  - `Format.toBundle()` drops track metadata, so a `MediaController` never sees it: the service reads the frame on
    `onTracksChanged` and publishes the text in the session extras, next to the sleep timer.
  - Still open, each its own task when wanted: MP4/M4A (`©lyr`) and FLAC tags, which Media3 doesn't map either;
    synchronised `SYLT` lyrics that scroll with playback; and `.lrc` sidecar files, which need an SAF folder grant
    because scoped storage hides non-media files.

**Checkpoint M4:** the user tests each feature on the phone.

### M7: CI/CD on free services (runs before M6, so the refactor is checked on every push)

The goal is a Play Store release that also works as a portfolio piece. Today every check runs by hand on one laptop:
the code is not on GitHub, `main` is 36 commits behind, release is signed with the debug key and `versionCode` is 1.

Decisions taken with the user: **public GitHub repo** (unlimited free Actions minutes, visible to recruiters);
automatic release goes **up to Play's internal testing**, with the public release a manual button press; there is
**no Play Console account yet** ($25, once), so T38 waits for it. Tool versions were checked against GitHub while
planning: `actions/checkout@v7`, `actions/setup-java@v6` (Temurin 25, as `gradle-daemon-jvm.properties` pins JDK 25),
`gradle/actions/setup-gradle@v6`, `actions/upload-artifact@v7`, `softprops/action-gh-release@v3`,
`r0adkll/upload-google-play@v1.1.5`.

- **T34 Put the project on GitHub (S; the user creates the repo). Absorbs T25.**
  - Before the first push, while history can still change safely: mark `gradlew` executable (stored as `100644`,
    so the first Linux build would fail with "Permission denied"); add `.gitattributes` keeping `gradlew` LF; extend
    `.gitignore` with keystores, `keystore.properties` and the Play service-account JSON; ask whether to switch the
    commit email to GitHub's private noreply address (every commit shows the personal address today); split the commit that
    mixed the shuffle fix with the icon layers (done).
  - Bring `main` up to date, then `git remote add origin` and `git push -u origin main` (`gh` isn't installed).
  - Acceptance: code and history on GitHub, `main` holds all the work, no key or password anywhere in it.
- **T35 CI on every push and pull request (S).**
  - `.github/workflows/ci.yml`: Java 25, Gradle cache, `./gradlew testDebugUnitTest lintDebug assembleDebug`; test and
    lint reports kept as artifacts when a run fails; older runs on the same branch cancelled. A `README.md` with the
    status badge.
  - Acceptance: a push shows a green check, and a deliberately broken test turns a pull request red.
- **T36 Real signing and version numbers (S; the user creates the key).**
  - An upload keystore made with one `keytool` command, backed up twice, never committed. A `release` signing config
    read from environment variables, falling back to the debug key when they are absent so local builds still work.
    `versionName` and `versionCode` both from the git tag via `VERSION_TAG` (`v1.2.3` → `1.2.3` / `10203`). Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
    `KEY_ALIAS`, `KEY_PASSWORD`.
  - Acceptance: with the secrets, the release build is signed with the upload key; without them it still builds.
- **T37 Tag → signed release on GitHub (S).**
  - `.github/workflows/release.yml` on a `v*` tag: decode the keystore, run the tests, `bundleRelease` and
    `assembleRelease`, attach the `.aab` and `.apk` to a GitHub Release with generated notes.
  - Acceptance: pushing `v1.0.0` produces a release whose APK installs on the phone and is signed with the upload key.
- **T38 Upload to Play internal testing (S; waits for the Play account).**
  - The user creates the Play Console account and the app, uploads the first bundle by hand (Play needs it to register
    the package and enrol Play App Signing), creates a Google Cloud service account with a JSON key, grants it
    release rights, and stores it as `PLAY_SERVICE_ACCOUNT_JSON`. `release.yml` gains an `upload-google-play` step
    to the `internal` track; closed testing (12 testers for 14 days on new personal accounts) and production stay
    manual.
  - Acceptance: a tag puts the build in Play's internal testing, installable from the tester link.
- **Checkpoint M7:** a push passes CI; tag `v1.0.0` and install its APK from the GitHub Release; after T38 the same
  tag appears in Play internal testing.
- Not covered: instrumented or UI tests (the app has none, and emulator tests on CI are slow). Gestures and playback
  are still checked on the phone at each checkpoint.

### M8: Synced lyrics (runs next, before M6)

The user wants lyrics the way YouTube Music shows them: the line being sung is highlighted, the view follows the
song, and tapping a line jumps there. The timings already reach the phone, and the app throws them away:
- LRCLIB returns `syncedLyrics` (`[01:02.50]line`) for many songs, but `wordsIn()` prefers `plainLyrics` and strips
  the timestamps off the synced version.
- A `.lrc` file is synced by definition, but `readLrc()` strips its timestamps too.
- So `lyrics.json`, the cache, only ever holds plain text.

What exists to build on:
- `HarmoniaApp` polls `controller.currentPosition` every 500 ms while playing.
- `FullPlayer` seeks through `onSeek(fraction)`, and `progressOf()` turns milliseconds into that fraction.
- The album art is a `weight(1f)` box between the top bar and the song info.

Decisions:
- **The lyrics take the album art's place inside the full player**, instead of a bottom sheet over it. The Lyrics
  chip switches between art and lyrics. The scrubber, play/pause and next/prev stay visible and working under the
  lyrics, so no controls are copied, and the area grows with the screen. The current sheet's contents move into
  that area: the "no lyrics", "not found" and "offline" messages and the "Find lyrics online" button. The view
  stays on lyrics when the song changes and goes back to the art when the player closes. *(The user chose this on
  2026-10-02 over a bigger bottom sheet with its own small controls.)*
- **One parser for every source.** The tag, the `.lrc` file and the online result all hand over raw text.
  `parseLrc()` returns timed lines when the text has timestamps; text without them shows as plain lines, as today.
  A tag holding LRC text, which downloaded files often have, then syncs as well with no extra code.
- **Synced beats plain online.** For a verified match (S3), synced lyrics win over plain ones. If `/api/get` only
  has plain lyrics, the same query's search results are checked for a synced copy before settling for plain.
- **The cache keeps the timings.** It moves to a new file, and the old `lyrics.json` is deleted because it only
  holds plain text. A song looked up before needs one more tap on "Find lyrics online".
- **A smooth highlight without polling more for the whole app.**
  - While the lyrics are on screen and playing, the view reads the position itself about every 100 ms, through a
    `() -> Long` that wraps `controller.currentPosition`. MediaController computes this locally, without a call to
    the service.
  - `derivedStateOf` turns the position into a line number, so the screen redraws only when the line changes.
  - The app-wide 500 ms tick stays as it is.
- **Tapping a line seeks to its time and plays**, so a paused song starts, because the line was tapped to be heard.
- **Scrolling by hand pauses the following.** Three seconds after the finger lifts, the view glides back to the
  current line.
- Not in scope:
  - Word-by-word karaoke. Word timings (`<00:12.50>`) are removed, and the whole line is highlighted.
  - ID3 `SYLT` frames, which are rare and binary.
  - M4A and FLAC lyrics tags, which are still open from T18.
  - A manual timing offset.

- **T39 Synced lyrics follow the song (M).**
  - `data/Lyrics.kt` gets `LyricLine(timeMs, text)` and two pure functions:
    - `parseLrc(text): List<LyricLine>` returns an empty list when the text has no timestamps. It must handle:
      - all timestamp forms: `[mm:ss]`, `[mm:ss.xx]`, `[mm:ss.xxx]` and `[mm:ss:xx]`;
      - several timestamps on one line (`[00:12.00][01:40.00]chorus`);
      - header lines (`[ar:]`, `[ti:]`, `[length:]`, …), which are skipped;
      - `[offset:±ms]`, which is applied;
      - word timings (`<00:12.50>`), which are removed;
      - blank lines, which are kept as instrumental gaps.

      The result is sorted by time.
    - `currentLine(lines, positionMs): Int` is the last line whose time has been reached, or -1 before the first.
  - `stripLrcTimestamps` goes away, because the plain view uses the parsed lines' text.
  - The sources keep their timings:
    - `readLrc` returns the raw file.
    - `wordsIn` and `lyricsIn` prefer `syncedLyrics`.
    - `fetchLyrics` looks for a synced copy in the search when `/get` only has plain lyrics.
    - The cache file is renamed, and the old one is deleted.
  - `ui/Player.kt` gets a `LyricsView` in place of the art, with three states:
    - **Synced:** a `LazyColumn` of lines. The current line is full white and bold, the others are dimmed, and the
      list scrolls the current line to about a third of the way down, with an animation.
    - **Plain:** scrollable text with a small "Not synced" note.
    - **None:** today's messages and the "Find lyrics online" button.

    The scroll resets when the song changes.
  - `ui/HarmoniaApp.kt` passes `position = { controller?.currentPosition ?: 0 }`.
  - Tests:
    - `LyricsTest` covers `parseLrc` (each format above, timestamps out of order, the offset) and `currentLine`
      (before the first line, exactly on a timestamp, after the last line).
    - `LyricsOnlineTest` covers synced chosen over plain, plain kept when there is no synced version, and
      instrumental still giving nothing.
  - Acceptance:
    - A song with synced online lyrics shows them line by line, and the highlight changes within about 0.3 s of
      the singer.
    - Dragging the scrubber moves the highlight and the view.
    - Plain-only lyrics still show.
    - The "find online" flow and its messages work as before.
  - Files: `data/Lyrics.kt`, `data/LyricsFile.kt`, `data/LyricsOnline.kt`, `ui/Player.kt`, `ui/HarmoniaApp.kt`, and
    the tests `LyricsTest` and `LyricsOnlineTest`.
- **T40 Tap a line to play from it (S).**
  - Each synced line can be tapped, with the accessibility label "Play from this line". A tap does
    `onSeek(progressOf(line.timeMs, song.duration))`, then plays if the song was paused. Plain lyrics have no
    times, so they stay untappable.
  - A finger drag on the list pauses the following. Three seconds after release, the view glides back to the
    current line.
  - Acceptance:
    - Tapping a line two minutes ahead jumps there, and the highlight lands on that line.
    - Tapping while paused starts playing from that line.
    - Scrolling away to read isn't pulled back while the finger is down, and the view returns 3 s after letting go.
  - Files: `ui/Player.kt`. The seek maths reuses `progressOf`, which is already tested.
- **T39 timing fix (found on the phone, 2026-10-02).** The highlight ran early on some songs and late on others.
  - Cause: LRCLIB keeps many uploads of one song, and their timings disagree by seconds. For Coldplay's "Yellow",
    the singing starts at 32.47, 33.8 or 35.66 s depending on the upload, and the app took whichever synced copy
    came first. Untagged files also sent the album "Unknown album", which matched an upload literally named that.
  - Fix:
    - `lyricsIn` takes the synced copy closest in length to the file.
    - The placeholder album is no longer sent.
    - The clock is read every 50 ms instead of 100 ms.
    - Synced lyrics get **Earlier / Later** buttons (0.5 s steps), saved per song in their own preferences file.
      This is because no automatic pick can know the file's own version, for example a video rip with a longer
      intro.
  - T40's tap-to-seek must add the song's saved shift to the line's time.
- **T39 title fix (found on the phone, 2026-10-02).** A song titled "Sign of the Times - Harry Styles" with the artist
  tag "Harry Styles" searched for the title "Sign of the Times Harry Styles" and found nothing (0 results on LRCLIB).
  - Fix: `titleWithout()` takes a known artist off either end of the title, as whole words only, before searching.
    The whole title is still tried next, for titles that really start with the artist's name ("Queen of the
    Night"). The title-only query also uses the cleaned title.
  - Checked live: the cleaned title finds 17 synced copies.
  - Missed at first: the user's file had no artist tag. Its name, "Harry Styles - Sign of the Times - Harry
    Styles", has the channel added after the title, so the artist sat inside the title guessed from the file name.
    The guessed title now has the artist taken off too.
- **T39 file-name order fix (found on the phone, 2026-10-02).** Files named "Title - Artist" with no artist tag were
  split the wrong way round.
  - "Viva la Vida - Coldplay" searched for the title "Coldplay" by the artist "Viva la Vida" and found nothing.
  - "The Scientist - Coldplay" showed the lyrics of **Clocks**. The title-only fallback searched for "Coldplay",
    and LRCLIB has mislabelled uploads (title "Coldplay", artist "Clocks", 308.6 s). That passed the ±3 s length
    check against the 309 s song.
  - Fix:
    - A split file name is tried both ways round (`fromFileName = true`), and such a guess needs the artist to
      match as well; the length alone no longer counts.
    - Half of a file name is never searched as the title alone. A real title tag still is.
    - "Not found" shows the file name as it reads.
  - Checked live: both songs find the right synced lyrics the other way round.
- **Checkpoint M8:** a phone check on the release build, with no lag while the list follows the song:
  - a song with synced online lyrics follows the music;
  - tapping a line jumps there;
  - the scrubber and next/prev still work with lyrics on screen;
  - changing songs resets the view;
  - a `.lrc` file is synced, if the user has one;
  - plain lyrics show as before;
  - the no-lyrics states work as before.

### M9: Alphabet index scroller (after M8, before M6)

Reaching a song near the end of a long A–Z list means a long scroll. The user wants the column of letters that
contact apps have: touch or slide a finger along it and the list jumps to that letter.

What exists to build on:
- With A–Z sort, `SongsScreen` already builds `Entry.Header(letter)` rows, so each letter's position in the list is
  known, and a jump is one `listState.scrollToItem(index)`.
- Drag-select is a long-press gesture on the `LazyColumn` itself, and `SwipeRow` handles horizontal swipes on rows.
- The heart and the "⋮" menu sit at the right end of every `SongRow`.

A bug found while reading, which this milestone depends on:
- The A–Z sort compares lowercased titles, but the heading for anything outside A–Z is "#".
- Digits and symbols sort **before** "a", while accented and non-Latin titles ("Élan", "夜に駆ける") sort **after**
  "z". A library with both gets **two "#" headings**.
- Both headings get the list key `"h#"`. Compose rejects duplicate keys, so that library would **crash the Songs
  tab**.
- An accented title also gets a "#" heading instead of its letter.

Decisions:
- **The bar sits beside the list, not on top of it.** The list loses about 24 dp of width while the bar shows, but
  the bar never covers the heart or "⋮" buttons and never steals a row's touches.
- **It shows only with A–Z sort** and at least two letter groups. Searching keeps it while the results still span
  two letters, and selection mode keeps it, because jumping while picking songs saves the same scrolling.
- **Only the letters the library has** are listed, spread evenly down the bar and capped at about 20 dp each. Every
  letter then does something, and a full column still fits a short screen.
- **The jump is instant** (`scrollToItem`, no animation), with a light haptic tick each time the letter under the
  finger changes. Touching jumps too, not just sliding.
- **"#" goes last**, as in the phone's contacts app, and accented letters count as their plain letter ("É" is "E").
- The bar lives in `SongsScreen.kt` for now. Artists and Albums in Explore can reuse it later if wanted.
- Not in scope: an index for Recent and Most played, which have no letter order; scrolling by the letters of the
  artist's name.

- **T41 A–Z index bar jumps to a letter (M).**
  - The grouping fix comes first, as pure tested functions:
    - `letterOf(title)` strips accents (`java.text.Normalizer`, available on every API level) and returns "A"–"Z",
      or "#" for anything else.
    - The A–Z sort orders by letter group first ("#" last), then by title, so each letter heading appears exactly
      once.
  - `AlphabetIndex` is a narrow column in a `Row` beside the `LazyColumn`:
    - It lists the letters present.
    - A touch or drag finds the letter under the finger (`letterAt(y, height, count)`, a clamped pure function) and
      calls `scrollToItem` on that letter's heading.
    - A haptic tick plays when the letter changes.
  - Tests in `SongsScreenTest`:
    - `letterOf`: a plain letter, an accented letter, a digit, a non-Latin title, an empty title.
    - The A–Z order: digit-led, accented and non-Latin titles together give one "#" heading, last, and every letter
      once.
    - `letterAt`: the top, the bottom, and past both ends.
  - Acceptance:
    - With A–Z sort, sliding down the bar jumps the list letter by letter, and touching "S" shows the S songs at
      the top.
    - The bar is gone in Recent and Most played.
    - Row swipes, drag-select, the heart and the "⋮" menu all work as before.
    - A library with "21 Guns", "Élan" and a Japanese title opens without a crash. "Élan" sits under E, and the
      other two share one "#" at the end.
  - Files: `ui/SongsScreen.kt`, `SongsScreenTest.kt`.
- **T42 Letter bubble and current-letter highlight (S).**
  - A large letter bubble appears beside the finger while it is on the bar, since the finger covers the bar's
    letter.
  - While the list scrolls normally, the bar highlights the letter of the group at the top, so the bar also shows
    where you are. This uses `derivedStateOf` on the first visible item, so it redraws only when the letter changes.
  - Accessibility: each letter is announced as "Jump to E" and can be activated with TalkBack.
  - Acceptance:
    - The bubble follows the finger and shows the letter jumped to.
    - The highlighted letter follows a normal scroll or fling.
    - TalkBack reads and activates the letters.
  - Files: `ui/SongsScreen.kt`. The pure "current letter from the first visible row" helper is tested in
    `SongsScreenTest`.
- **T41 redesign (user feedback, 2026-10-02): "I don't like how it's built."** The first bar was plain grey letters
  with no container, no active state and no feedback under the finger, which ignored the design system. It was
  rebuilt in `ui/AlphabetIndex.kt` and took T42 in with it.
  - **At rest:** a slim muted capsule, like an inactive sort pill (§5.4), with extra-bold letters (§5.8).
  - **While scrolling:** the group at the top of the list is lit sage on a `primary/15` dot, as the nav bar lights
    its tab (§5.2). It is read through `derivedStateOf`, so only the bar redraws.
  - **On touch:**
    - the capsule tints sage, and the letter under the finger pops with a spring;
    - a 56 dp sage bubble shows that letter beside the bar. It appears on the touched letter, then glides with the
      mini player's spring (340/30, §10).
  - **Accessibility:** each letter is a TalkBack button ("Jump to E", "Jump to other titles" for #).
  - **Checks:** a light and a dark `@Preview`, and `sectionAt` is unit-tested.
  - **Fixed after the phone check:** the letter sat about 4 dp below its lit dot. Material 3's default text style
    gives every `Text` a 24 sp line height, and the 16 dp dot cut that box short and drew it from the top. The
    letters now have `lineHeight = 10.sp`. The offset was worked out from Nunito's metrics: +3.76 dp before (+4.71
    dp pressed, matching the screenshot), −0.24 dp after.
- **Checkpoint M9:** on the phone, release build:
  - jump to Z and back to A, letter by letter, with no stutter;
  - switch to Recent and back;
  - drag-select and row swipes still work;
  - search results keep the bar while they span two letters.

### M6: MVVM migration (runs before M5, since T19 and T20 rework the same UI)

The `data/` review found the View calling the network directly (`ui/Player.kt` → `fetchLyrics`), a `@Composable`
inside `data/AlbumArt.kt`, repository and local data source mixed together in `object Library`, and no seam to fake
anything in a test. The app works, so this is a deliberate learning refactor towards the MVVM diagram: View →
ViewModel → Repository → Local/Remote DataSource.

Decisions taken with the user: **Hilt** for wiring, **one ViewModel per screen**, **StateFlow** for state. Four
slices, each one shippable, buildable and phone-checked on its own. Behaviour must not change until T33.

- **T30 Tidy `data/` (S, no behaviour change).**
  - `SampleData.kt` is three things at once: split it into the models plus `cleanTag` (domain), the display helpers
    `formatDuration`/`formatDate`/`gradientFor` (move to `ui/`), and the preview seed lists.
  - Move the `albumArtOf` composable out of `data/AlbumArt.kt` into `ui/`; `Song.gradient` becomes a UI extension so
    the model stops depending on Compose colours.
  - Acceptance criteria: no Compose import in `data/` except state holders; every test still passes; the app looks
    and behaves exactly as before.
- **T31 Hilt and the data sources (M).**
  - **First, prove the build**: this project has no Kotlin plugin of its own, it uses AGP 9.3.3's built-in Kotlin,
    so KSP (`2.2.10-2.0.2`) and Hilt (`2.60.1`) must be shown to compile here before anything else moves. If they
    don't, fall back to a hand-written container and keep the rest of the plan unchanged.
  - New `HarmoniaApp : Application` with `@HiltAndroidApp`, `@AndroidEntryPoint` on `MainActivity` and
    `PlaybackService` (the service is the awkward consumer: it runs with no activity).
  - `data/local/`: `MediaStoreSource` (the scan query), `LibraryStore` (`library.json`), `SettingsStore`
    (SharedPreferences), `ArtSource` (album art), `TagLyricsSource` (ID3 USLT), `LyricsStore` (`lyrics.json`).
    `data/remote/`: `LrcLibSource`. Each takes the application context once, instead of a `Context` parameter on
    every call (7 of them in `Library.kt` today).
  - Acceptance criteria: sources are classes with injected dependencies, `Library` and `Settings` delegate to them
    for now, and the app still scans, saves, plays and fetches lyrics exactly as before.
- **T32 Repositories (M).**
  - `MusicRepository` (songs, playlists, likes, play counts, scan, delete), `LyricsRepository` (tags, then cache,
    then network — so the View stops calling `fetchLyrics` itself), `SettingsRepository`.
  - `MusicRepository` keeps an id → song map, which also fixes the O(n·m) playlist lookups at
    `PlaylistsScreen.kt:111`.
  - `object Library` and `object Settings` are deleted; the UI and the service talk only to repositories.
  - Acceptance criteria: nothing outside `data/` mentions MediaStore, SharedPreferences, files or HTTP; a fake
    repository can be constructed in a test; the service still plays with the app closed.
- **T33 ViewModels and StateFlow (M–L).**
  - `SongsViewModel`, `PlaylistsViewModel`, `ExploreViewModel`, `SettingsViewModel`, `PlayerViewModel`, each
    `@HiltViewModel`, reached with `hiltViewModel()`, exposing `StateFlow` read by `collectAsStateWithLifecycle`.
  - `PlayerViewModel` owns the `MediaController` connection and the mirrored playback state, which is the bulk of
    today's 392-line `HarmoniaApp.kt`; that file shrinks to wiring.
  - Watch the state that is deliberately snapshot-like today: Liked Songs freezing its ids on open, the drag
    selection, and the permission flow.
  - Acceptance criteria: screens receive a ViewModel rather than a dozen callbacks; rotation keeps playback,
    selection and scroll state; every feature from M0 to M4 still works.
- **Checkpoint M6:** a full phone run: scan, play, notification and lock screen, queue, sleep timer, shuffle,
  album art, delete, drag-select, search-while-selecting, lyrics from tags and online, and everything surviving a
  restart.

### M5: Polish and release

- **T19 Light performance mode (S, PRD phase 5).**
  - A `LocalLiteMode` CompositionLocal turns off `enterAnimation`, shadows, the art "breathing" effect and the soundbar animation.
  - It defaults on when `ActivityManager.isLowRamDevice()` is true.
- **T20 Encoding repair and RTL audit (S–M, PRD phase 6).**
  - Repair mojibake in the scan mapping with `android.icu.text.CharsetDetector` (API 24+, no dependency). The detector is injected so the function can be unit-tested with a fake.
  - Check the scrubber, drag gestures and slides with `@Preview(locale = "ar")`.
- **T21 (optional) Indonesian translation.** Move hardcoded strings into `strings.xml` and add `values-in`. Do it one screen at a time.
- **T22 Release checklist.** Real keystore (the user's action), Play Console declarations for the foreground-service type and `READ_MEDIA_AUDIO`, a no-data privacy policy, and an R8 release smoke test.

- **T28 Optional online lyrics (M, beyond the PRD).**
  - T18 reads lyrics from a song's tags and most files carry none, so the sheet usually says "no lyrics". This is
    the first feature that would make Harmonia use the internet, so it is opt-in and the offline promise is
    reworded rather than quietly broken.
  - Decisions taken with the user: **off until switched on**, **nothing sent unless "Find lyrics online" is
    tapped** (no background lookups, no library scanning), and `.lrc` files handled separately in T29.
  - Source: **LRCLIB** (`https://lrclib.net/api/get?artist_name=&track_name=&album_name=&duration=`, falling back
    to `/api/search`), free, no key, no account, and checked live while planning. It asks for a `User-Agent`
    naming the app. **Any non-200 means "no lyrics right now"**: a nonsense track answered 503, not 404, so 404
    must not be special-cased and an outage has to look like a normal miss.
  - Acceptance criteria:
    - With the switch off, the sheet offers no lookup and the app sends nothing.
    - Turning the switch on first shows what will be sent (title, artist, album, length), to whom, and when.
    - A song with no lyrics of its own shows "Find lyrics online"; tapping it finds and shows them.
    - A song that has lyrics in its tags never offers the lookup and never fetches.
    - Anything found is saved on the phone, so the same song shows lyrics later with no network at all.
    - No network gives a clear "couldn't reach the lyrics service", a miss gives "no lyrics found", and neither
      hangs or crashes.
  - Files: `AndroidManifest.xml` (`INTERNET`), new `data/LyricsOnline.kt` (`HttpURLConnection` + `org.json`, no new
    dependency; pure `lyricsFrom`, `stripLrcTimestamps` and `lyricsUrl` are unit-tested), a `lyrics.json` cache in
    `filesDir`, `data/Settings.kt`, `ui/SettingsScreen.kt`, `ui/Player.kt`, `ui/Components.kt` (promise wording).
  - **T22 must be updated**: the privacy policy and Data safety form need the optional lookup, and the store
    listing must not present the lyrics as Harmonia's own content.
- **T29 `.lrc` lyrics files (S–M, after T28).**
  - What PRD phase 9 actually asked for: a `Song.lrc` beside the audio file. Scoped storage hides non-media files,
    so it needs a one-time folder grant via `ACTION_OPEN_DOCUMENT_TREE`.
  - Reuses `stripLrcTimestamps` from T28, and is the base for karaoke-style scrolling lyrics later.
  - Acceptance criteria: after granting a folder, a song with a matching `.lrc` shows those lyrics with no network;
    without a grant nothing changes; the grant survives a restart.

### Loose ends

Small things found while building, parked here so they aren't forgotten. Anything that comes up from now on gets
appended to this list rather than living in a chat message. T23 has to land before T22 (the release ships the icon),
T26 only works once the Play Store listing exists, and T24 and T25 can happen any time.

- **T23 Adaptive launcher icon (S, before T22).**
  - The icon is currently built wrong for Android 8+: the dark square is baked into the foreground layer while the
    background layer is white, so launchers that mask to a circle or squircle show a dark square with white edges,
    and the `monochrome` layer (the same full-colour image) makes an Android 13 themed icon a solid blob.
  - Acceptance criteria:
    - The foreground holds only the note on transparency, inside the middle 66% of the canvas.
    - The background layer is the dark slate `#1E2022`, not white.
    - `monochrome` is a silhouette of the note, or the line is removed.
    - No white edge on circle, squircle or teardrop masks, and the note is still readable as a themed icon.
    - The legacy `ic_launcher.webp` (API 24-25) keeps the full-bleed artwork.
  - The artwork itself is the user's to regenerate (Android Studio > New > Image Asset); the background colour
    change can land on its own first.
  - Files: `res/mipmap-anydpi-v26/ic_launcher*.xml`, `res/mipmap-*/ic_launcher_foreground.webp`,
    `res/values/ic_launcher_background.xml`.
- **T24 Delete the template leftovers (S).** Not `keepRules/rules.keep`: since the Retrofit change it holds the
  rule that keeps the lyrics DTO's fields through R8, so deleting it would break the release build.
  - From the over-engineering review: about 120 lines of Android Studio scaffolding that nothing uses.
  - `res/values/colors.xml` (seven unused template colours),     `res/xml/backup_rules.xml` and `data_extraction_rules.xml` with their two manifest attributes,
    `ExampleInstrumentedTest.kt` with its four `androidTestImplementation` lines, the `ui-test-manifest` line and
    the six now-unused `libs.versions.toml` entries, and the no-op `Modifier.alpha(1f)` in a preview.
  - Acceptance criteria: `testDebugUnitTest`, `lintDebug` and `assembleRelease` all still pass, the app still
    launches, and backup behaviour is unchanged (no rules file means the same defaults).
- **T25 Git hygiene (S). Handled inside T34, before the first push.**
  - Done in T34: the commit that carried the shuffle ANR fix *and* the launcher icon layers (staged when it was made)
    is now two commits, each one concern.
  - The branch is still called `m2-playback` although it now holds M1 to M4. Rename it, or merge it into `main`.
  - Acceptance criteria: each commit touches one concern, and the branch builds after the rewrite.
  - Nothing is pushed, so the rewrite is safe; confirm with the user before rewriting history.
- **T26 "Rate Harmonia" opens the store listing (S, after T22).**
  - The Settings row is inert today because there is nothing to rate yet.
  - Acceptance criteria:
    - Tapping it opens `market://details?id=com.rfaizm.harmoniamusic`.
    - It falls back to the `https://play.google.com/store/apps/details?id=...` page when no store app handles it.
    - Nothing crashes on a device with no Play Store at all.
  - Files: `SettingsScreen.kt`.

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Nothing can be tested on a device from this machine | High | Keep tasks small, with a user device check at every checkpoint. Put logic in pure functions and unit-test those. |
| Media3 service and permission edge cases across API 24 to 37 | High | T6 goes first in M2. Rely on Media3 defaults and don't customise the notification. |
| Scoped-storage delete behaves differently on each API level | Med | T16 is isolated with three explicit branches. |
| Lyrics are blocked by storage rules | Med | Timeboxed spike, embedded lyrics first. |
| Synced lyrics run early or late (a radio edit timed against the album version) | Med | S3's ±3 s length check already filters other versions. A manual offset can follow if it shows up on the phone. |
| A slide on the A–Z bar at the right edge is taken as the system Back gesture | Low | Back is a horizontal swipe inwards, and the bar reads vertical slides. The bar keeps the list's 8 dp margin from the edge. Checked on the phone at Checkpoint M9. |
| The lyrics view fights the user's finger, or stutters on a low-end phone | Med | T40 pauses the following while scrolling. The line number goes through `derivedStateOf`, so only a line change redraws. Judge on the release build. |

## Open questions (defaults in brackets; they don't block M0 to M3)

- Crossfade on track change: drop it and keep only the fade-in on resume? [drop]
- The Settings toggles "Gapless" and "Crossfade" have nothing to control once Media3 is in (gapless is always on). Remove them? [remove]
- On API 30+ the system already shows its own delete confirmation. Skip the app's red dialog there, to avoid confirming twice? [skip on 30+]
- Should the UI be translated to Indonesian (T21)? [not now]
- M8: should the old cache of plain online lyrics be dropped, so those songs can be fetched again with timings?
  [drop it]

## How this plan is kept

- This file holds the plan; `tasks/todo.md` tracks what is done, with a line per task.
- Anything found along the way that isn't part of the current task goes into **Loose ends** above, with acceptance
  criteria, instead of staying in a chat message.
- Each task ends with `testDebugUnitTest` and `assembleDebug`, plus `lintDebug` at a checkpoint, and the user runs
  the release build on a phone at every checkpoint.
