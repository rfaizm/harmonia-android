package com.rfaizm.harmoniamusic

import com.google.gson.Gson
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.artistQuery
import com.rfaizm.harmoniamusic.data.lyricsIn
import com.rfaizm.harmoniamusic.data.remote.dto.LyricsDto
import com.rfaizm.harmoniamusic.data.stripLrcTimestamps
import com.rfaizm.harmoniamusic.data.wordsIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsOnlineTest {
    @Test
    fun takesThePlainLyricsWhenTheyAreThere() {
        val entry = LyricsDto(plainLyrics = "Line one\nLine two", syncedLyrics = "[00:12.00]Line one")

        assertEquals("Line one\nLine two", wordsIn(entry))
    }

    @Test
    fun fallsBackToTheSyncedLyricsWithoutTheirTimestamps() {
        val entry = LyricsDto(plainLyrics = "", syncedLyrics = "[00:12.00]Line one\n[00:15.30]Line two")

        assertEquals("Line one\nLine two", wordsIn(entry))
    }

    @Test
    fun takesTheFirstSearchResultThatActuallyHasLyrics() {
        // Most results carry nothing at all, so reading result zero finds nothing most of the time.
        val results = listOf(
            LyricsDto(plainLyrics = null, syncedLyrics = null),
            LyricsDto(instrumental = true),
            LyricsDto(plainLyrics = "The third one has them"),
        )

        assertEquals("The third one has them", lyricsIn(results))
    }

    @Test
    fun anEmptyOrInstrumentalAnswerHasNoLyrics() {
        assertNull(lyricsIn(emptyList()))
        assertNull(wordsIn(null))
        assertNull(wordsIn(LyricsDto(instrumental = true, plainLyrics = "ignored")))
        assertNull(wordsIn(LyricsDto(plainLyrics = "   ")))
    }

    @Test
    fun gsonMapsTheServicesNullsOntoTheDto() {
        // The service really does answer with JSON nulls, and this is the same Gson the app uses, so the test
        // can't drift from the phone the way hand-parsing did.
        val json = """
            [{"id":1,"trackName":"Yellow","instrumental":false,"plainLyrics":null,"syncedLyrics":null},
             {"id":2,"trackName":"Yellow","instrumental":false,"plainLyrics":"Look at the stars"}]
        """.trimIndent()

        val results = Gson().fromJson(json, Array<LyricsDto>::class.java).toList()

        assertNull(results[0].plainLyrics)
        assertEquals("Look at the stars", lyricsIn(results))
    }

    @Test
    fun aFileWithNoArtistTagSearchesByTitleAlone() {
        // Sending "Unknown artist" finds four wrong songs instead of the right one, so it is left out.
        assertNull(artistQuery(Song(2, "Yellow", "Unknown artist", "Unknown album", 0, 269)))
        assertEquals("Coldplay", artistQuery(Song(3, "Yellow", "Coldplay", "Parachutes", 2000, 269)))
    }

    @Test
    fun stripsTimestampsAndLrcHeaderLines() {
        val lrc = "[ar:Aurelia Vance]\n[ti:Quiet Harbour]\n[00:01.00]First\n[00:04.25]Second\n"

        assertEquals("First\nSecond", stripLrcTimestamps(lrc))
    }
}
