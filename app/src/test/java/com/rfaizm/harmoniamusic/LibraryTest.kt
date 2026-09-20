package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.Playlist
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.countPlay
import com.rfaizm.harmoniamusic.data.keepUserState
import com.rfaizm.harmoniamusic.data.parseState
import com.rfaizm.harmoniamusic.data.stateJson
import com.rfaizm.harmoniamusic.data.toSong
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryTest {
    @Test
    fun blankTitleFallsBackToTheFileNameWithoutExtension() {
        val song = toSong(42, title = " ", artist = "Aurelia Vance", album = "Quiet Hours", year = 2023, durationMs = 214_900, fileName = "Morning_Light.mp3")

        assertEquals(Song(42, "Morning_Light", "Aurelia Vance", "Quiet Hours", 2023, 214), song)
    }

    @Test
    fun missingAndUnknownTagsBecomeReadablePlaceholders() {
        val song = toSong(7, title = null, artist = "<unknown>", album = null, year = 0, durationMs = 61_000, fileName = null)

        assertEquals(Song(7, "Unknown title", "Unknown artist", "Unknown album", 0, 61), song)
    }

    @Test
    fun rescanKeepsLikesAndPlayCountsButTakesFreshTags() {
        val old = listOf(Song(1, "Old Tag", "X", "A", 2020, 100, liked = true, playCount = 5))
        val fresh = listOf(Song(1, "Fixed Tag", "X", "A", 2020, 100), Song(2, "New Song", "Y", "B", 2021, 200))

        val merged = keepUserState(fresh, old)

        assertEquals(
            listOf(Song(1, "Fixed Tag", "X", "A", 2020, 100, liked = true, playCount = 5), Song(2, "New Song", "Y", "B", 2021, 200)),
            merged,
        )
    }

    @Test
    fun likesPlayCountsAndPlaylistsSurviveASaveAndLoad() {
        val songs = listOf(
            Song(1, "A", "X", "Album", 2020, 100, liked = true, playCount = 5),
            Song(2, "B", "Y", "Album", 2021, 200),
        )
        val playlists = listOf(Playlist(3, "Focus Flow", listOf(2, 1), 5, "2026-07-28"))

        val (savedSongs, savedPlaylists) = parseState(stateJson(songs, playlists))

        // Tags come back from MediaStore, so only the user's own state is stored and merged onto a fresh scan.
        assertEquals(songs, keepUserState(fresh = songs.map { it.copy(liked = false, playCount = 0) }, old = savedSongs))
        assertEquals(playlists, savedPlaylists)
    }

    @Test
    fun aCorruptSaveFileLoadsAsEmptyInsteadOfCrashing() {
        assertEquals(emptyList<Song>() to emptyList<Playlist>(), parseState("{ truncated"))
    }

    @Test
    fun countPlayBumpsOnlyTheMatchingSong() {
        val songs = mutableListOf(Song(1, "A", "X", "A", 2020, 100), Song(2, "B", "Y", "B", 2021, 200, playCount = 3))

        songs.countPlay(2)

        assertEquals(listOf(0, 4), songs.map { it.playCount })
    }

    @Test
    fun countPlayIgnoresASongNoLongerInTheLibrary() {
        // The player can still hold a song a rescan removed; counting it must not crash the service.
        val songs = mutableListOf(Song(1, "A", "X", "A", 2020, 100))

        songs.countPlay(99)

        assertEquals(listOf(Song(1, "A", "X", "A", 2020, 100)), songs)
    }
}
