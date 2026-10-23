package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.ui.Entry
import com.rfaizm.harmoniamusic.ui.SortBy
import com.rfaizm.harmoniamusic.ui.albumsOf
import com.rfaizm.harmoniamusic.ui.artistsOf
import com.rfaizm.harmoniamusic.ui.resolve
import com.rfaizm.harmoniamusic.ui.songEntries
import org.junit.Assert.assertEquals
import org.junit.Test

/** M6 (T33): what the Songs and Playlists screens show, worked out in their ViewModels as pure functions. */
class SongsViewModelTest {
    private val songs = listOf(
        Song(3, "Yellow", "Coldplay", "Parachutes", 2000, 269, playCount = 2),
        Song(1, "Clocks", "Coldplay", "A Rush of Blood", 2002, 307, playCount = 9),
        Song(2, "Adore You", "Harry Styles", "Fine Line", 2019, 207),
    )

    @Test
    fun aToZGroupsTheSongsUnderOneHeadingPerLetter() {
        assertEquals(
            listOf("A", "Adore You", "C", "Clocks", "Y", "Yellow"),
            songEntries(songs, query = "", sortBy = SortBy.AZ).map { it.label() },
        )
    }

    @Test
    fun recentAndMostPlayedHaveNoHeadings() {
        assertEquals(listOf("Yellow", "Adore You", "Clocks"), songEntries(songs, "", SortBy.Recent).map { it.label() })
        assertEquals(listOf("Clocks", "Yellow", "Adore You"), songEntries(songs, "", SortBy.MostPlayed).map { it.label() })
    }

    @Test
    fun searchMatchesTheTitleOrTheArtistInAnyCase() {
        assertEquals(listOf("C", "Clocks", "Y", "Yellow"), songEntries(songs, query = "  COLDplay ", sortBy = SortBy.AZ).map { it.label() })
        assertEquals(listOf("Yellow"), songEntries(songs, query = "yell", sortBy = SortBy.Recent).map { it.label() })
    }

    @Test
    fun eachRowKnowsItsPlaceInTheSortedList() {
        // SongRow staggers its entrance by this number, so headings must not count.
        val items = songEntries(songs, "", SortBy.AZ).filterIsInstance<Entry.Item>()

        assertEquals(listOf(0, 1, 2), items.map { it.index })
    }

    @Test
    fun aPlaylistResolvesInItsOwnOrderAndSkipsDeletedSongs() {
        val byId = songs.associateBy { it.id }

        assertEquals(listOf("Adore You", "Yellow"), resolve(listOf(2, 99, 3), byId).map { it.title })
    }

    @Test
    fun exploreListsEachArtistOnceInAlphabeticalOrder() {
        val withLowercase = songs + Song(4, "Golden", "coldplay", "Golden", 2001, 200)

        assertEquals(
            listOf("Coldplay" to listOf("Yellow", "Clocks"), "coldplay" to listOf("Golden"), "Harry Styles" to listOf("Adore You")),
            artistsOf(withLowercase).map { (artist, list) -> artist to list.map { it.title } },
        )
    }

    @Test
    fun exploreListsEachAlbumOnceInAlphabeticalOrder() {
        assertEquals(listOf("A Rush of Blood", "Fine Line", "Parachutes"), albumsOf(songs).map { it.first })
    }

    private fun Entry.label() = when (this) {
        is Entry.Header -> letter
        is Entry.Item -> song.title
    }
}
