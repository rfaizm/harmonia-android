# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Harmonia: an offline local-music player for Android, written in Kotlin with Jetpack Compose and Material 3. There is a single `:app` module (package `com.rfaizm.harmoniamusic`).

- `PRD.md` holds the feature spec, in Indonesian, split into phases 1–9: scanning, metadata cleanup, batch playlist creation, audio focus, low-end devices, i18n/RTL, small UX details, likes and Favorites, and advanced playback.
- `GUIDELINE.md` is the design system: colors, typography, spacing, component specs and motion. It is written in React/Tailwind/lucide/framer-motion terms and has to be translated into Compose (see below).

**The UI is done; real logic is being added one milestone at a time.** `tasks/plan.md` holds the plan and `tasks/todo.md` tracks progress (M0–M5). The library is real now: permission flow plus a MediaStore scan. Playback runs on Media3 (M2 in progress) and there is no persistence yet (M3). Don't build ahead of the current task.

## Commands

`java` is not on PATH. Use Android Studio's bundled JDK:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # Git Bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease          # R8 enabled, signed with the debug key
./gradlew :app:testDebugUnitTest        # JVM unit tests
./gradlew :app:testDebugUnitTest --tests "com.rfaizm.harmoniamusic.CleanTagTest"
./gradlew :app:lintDebug
```

This machine has no emulator or connected device, so UI can't be checked from here. Rely on `@Preview`s, or ask the user to run the app. Judge performance on the **release** variant: Compose debug builds are much slower, and the user once reported "lag" that was largely down to the debug build.

## Architecture

- **Repositories (M6, T32):** the app talks to three `@Singleton` repositories in `data/`, never to storage directly.
  - `MusicRepository` publishes the songs, playlists and the loaded flag as `StateFlow`s, and changes them only through named methods (`toggleLike`, `addToPlaylist`, `remove`, …), each of which saves. It also holds likes, play counts, the scan, album art, file deletion and the session a media button resumes. It lives for the whole process, so rotation doesn't rescan and the playback service sees the same songs as the screens.
  - `SettingsRepository` publishes the switches (`flags`) and the `.lrc` folder grant as `StateFlow`s, and keeps each song's lyrics timing. Its `onChange` tells the service when to re-apply settings.
  - `LyricsRepository` holds the `.lrc` file and the saved online lyrics, and the online lookup that only runs when the user taps.
- **Hilt wiring:**
  - `HarmoniaApplication` is the `@HiltAndroidApp`. Hilt 2.60.1 runs on KSP 2.3.12 under AGP 9's built-in Kotlin.
  - `MainActivity` and `PlaybackService` are `@AndroidEntryPoint` and get the repositories injected.
  - `MainActivity` passes them into `HarmoniaApp`, and provides `LocalMusic` so album art deep in the tree can reach the library. Previews have no `LocalMusic` and show gradients.
- **The repositories' I/O is done by `@Singleton` sources:**
  - `data/local/`: `MediaStoreSource` (also the song URI and the delete calls), `LibraryStore`, `SessionStore`, `SettingsStore`, `ArtSource`, `LyricsStore` and `LrcFolderSource`.
  - `data/remote/`: `ApiService`, built by the `ApiConfig` Hilt module. `fetchLyrics` takes the service, so tests pass a fake (`FetchLyricsTest`).
- **Guard tests:** `DataLayerTest` keeps UI code and I/O out of the rest of `data/`, and `ArchitectureTest` keeps all I/O out of everything outside `data/`.
- **ViewModels (M6, T33):** each is a `@HiltViewModel`, reached with `hiltViewModel()` and read with `collectAsStateWithLifecycle()`.
  - `PlayerViewModel` connects to the playback service while the app is visible, mirrors it into one `PlaybackState`, and carries every player command (volume through `AudioManager`).
  - `SongsViewModel` works out the list (`songEntries`) and keeps the search and sort in `SavedStateHandle`. The search is Compose state, not a flow, so the text field's cursor doesn't jump.
  - `PlaylistsViewModel` resolves playlists through an id map (`resolve`), and `ExploreViewModel` groups artists and albums (`artistsOf`/`albumsOf`). Both are tested pure functions.
  - `SettingsViewModel` covers the Settings tab.
  - `ArchitectureTest` fails if a screen imports the media session or `AudioManager`.
- **Screen state stays in the composables:** `HarmoniaApp` keeps the tab, the full-player flag and the permission flow, and the screens keep their selection, the open playlist and the Liked snapshot. The full player's lyrics view still takes `SettingsRepository` and `LyricsRepository` directly.
- **Screens:** `SongsScreen`, `PlaylistsScreen` (the list and the detail view are switched by local state, not by navigation), `ExploreScreen` (Artists/Albums tab), `SettingsScreen` and `Player.kt` (mini player plus the full-screen player overlay). Shared building blocks are in `ui/Components.kt`.
- **Theme mapping** (`ui/theme/Theme.kt`): the guideline's CSS tokens are mapped onto the M3 `ColorScheme` (card→surface, muted→surfaceVariant, muted-foreground→onSurfaceVariant, border→outline, accent→tertiary, destructive→error). Extension aliases (`colors.card`, `colors.muted`, `colors.mutedForeground`, `colors.border`, `colors.destructive`) keep the guideline's names in code. `colors` is a `@Composable` getter for `MaterialTheme.colorScheme`, defined in `Components.kt`.
- **`HarmoniaTheme` provides `LocalContentColor = onBackground`.** The root is a plain `Box`, not a `Surface`, so without this any `Text` with no explicit color renders black in dark mode.
- **Guideline→Compose translation:**
  - lucide icons → `material-icons-extended` Rounded icons. The version is pinned at 1.7.8 in `libs.versions.toml` because the library isn't in the Compose BOM.
  - Nunito is bundled in `res/font/nunito_{400..900}.ttf`. These files cover Latin characters only; other scripts fall back to the system font.
  - framer-motion springs → Compose `spring(dampingRatio = damping / (2·√stiffness), stiffness)`.
  - Tailwind sizes are written inline as `dp`/`sp`.
- **Data** (`data/Models.kt`): `Song`/`Playlist` models, `cleanTag()` (the GUIDELINE §11 regex plus y2mate stripping) and the playlist helpers (`addSongs`/`removeSong`). `Song.displayTitle` and `displayArtist` cache `cleanTag()` once per instance. Use those in UI code, not `cleanTag()` directly.
- **How data looks** lives in `ui/`, not `data/` (T30, guarded by `DataLayerTest`):
  - `ui/Display.kt` holds `GRADIENTS` (album art is a gradient chosen by `id % 12`), the `Song.gradient`/`Playlist.gradient` extensions, `formatDuration`/`formatDate` and `albumArtOf`.
  - `ui/PreviewData.kt` holds the seed lists, which only `@Preview`s and tests use.
- **`data/` imports no Compose at all** (`DataLayerTest`). Every repository change produces a new immutable list, so anything keyed on a list with `remember(list)` recomputes when it changes.
- **`Modifier.enterAnimation()`** runs once per item (`rememberSaveable`). It staggers rows that first appear in the same frame (grouped by `withFrameMillis`), so a list opening or a sort change cascades, while a row scrolled in on its own fades in with no delay. Don't gate it on the list index: rows change index when sorting or searching, and gating on it reset their state, which caused the Songs-tab glitches.

## PRD behaviors already reflected in the UI

- Long-press a song to enter selection mode, then the "Add N songs" button opens a bottom sheet with inline naming; if the name already exists, it offers to merge (phase 3).
- In the song menu, "Delete file" is kept far from "Remove from playlist", with a different icon and a red confirmation dialog (phase 7).
- Liked Songs takes a snapshot of its ids when opened, so a song you unlike stays in the list, faded, until you reopen it (phase 8).
- On first launch an explainer screen appears before the system permission prompt. After a permanent denial its button opens app settings. An empty library shows a pull-to-refresh empty state (phase 1).

## Constraints

- minSdk is 24: avoid `java.time` and other API 26+ calls, or add core library desugaring.
- The release build type uses `signingConfigs.debug` so it can be installed locally. A real keystore is needed before a Play Store release.
