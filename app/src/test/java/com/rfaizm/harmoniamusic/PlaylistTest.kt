package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.Playlist
import com.rfaizm.harmoniamusic.data.addSongs
import com.rfaizm.harmoniamusic.data.removeSong
import com.rfaizm.harmoniamusic.data.removeSongEverywhere
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistTest {
    @Test
    fun addingToAnExistingNameMergesAndSkipsDuplicates() {
        val playlists = mutableListOf(Playlist(1, "Focus Flow", listOf(3, 6), 5, "2026-07-28"))

        playlists.addSongs("focus flow", listOf(6, 9, 3, 12), "2026-09-19")

        assertEquals(1, playlists.size)
        assertEquals(listOf(3, 6, 9, 12), playlists[0].songIds)
    }

    @Test
    fun addingToANewNameCreatesAPlaylistWithTheNextId() {
        val playlists = mutableListOf(Playlist(4, "Road Trip", listOf(0), 8, "2026-06-03"))

        playlists.addSongs("Night Drive", listOf(7, 2), "2026-09-19")

        assertEquals(2, playlists.size)
        assertEquals(Playlist(5, "Night Drive", listOf(7, 2), 4, "2026-09-19"), playlists[1])
    }

    @Test
    fun removingASongLeavesTheRestInOrder() {
        val playlists = mutableListOf(Playlist(1, "Focus Flow", listOf(3, 6, 9), 5, "2026-07-28"))

        playlists.removeSong(playlistId = 1, songId = 6)

        assertEquals(listOf(3, 9), playlists[0].songIds)
    }

    @Test
    fun aDeletedSongLeavesEveryPlaylistItWasIn() {
        val playlists = mutableListOf(
            Playlist(1, "Focus Flow", listOf(3, 6, 9), 5, "2026-07-28"),
            Playlist(2, "Road Trip", listOf(6, 7), 8, "2026-06-03"),
            Playlist(3, "Evening Wind-down", listOf(3, 9), 2, "2026-08-12"),
        )

        playlists.removeSongEverywhere(songId = 6)

        assertEquals(listOf(3, 9), playlists[0].songIds)
        assertEquals(listOf(7), playlists[1].songIds)
        assertEquals(listOf(3, 9), playlists[2].songIds) // untouched
    }
}
