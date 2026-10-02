# Harmonia

[![CI](https://github.com/rfaizm/harmonia-android/actions/workflows/ci.yml/badge.svg)](https://github.com/rfaizm/harmonia-android/actions/workflows/ci.yml)

An offline music player for Android that plays the songs already on your phone. No account, no ads, no streaming.

## Features

- Scans the phone's music and cleans up messy tags (for example "Song (Official Video) [y2mate]").
- Playlists: long-press a song, drag over more songs to select them, then add them all at once.
- Background playback with notification, lock-screen and headset controls.
- Smart shuffle that avoids playing two songs by the same artist in a row.
- Sleep timer that fades the music out over the last minute.
- Lyrics from the song's tags, from a `.lrc` file beside it, or (only if you turn it on) from [LRCLIB](https://lrclib.net).
- Likes, play counts, swipe actions, and light and dark themes.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Media3 (ExoPlayer + MediaSession) · Retrofit · GitHub Actions

## Build

You need JDK 25 and the Android SDK.

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug
```

Every push to `main` and every pull request runs the unit tests, lint and a debug build on GitHub Actions.
