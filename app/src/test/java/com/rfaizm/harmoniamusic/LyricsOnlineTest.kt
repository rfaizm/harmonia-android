package com.rfaizm.harmoniamusic

import com.google.gson.Gson
import com.rfaizm.harmoniamusic.data.lyricsIn
import com.rfaizm.harmoniamusic.data.remote.dto.LyricsDto
import com.rfaizm.harmoniamusic.data.wordsIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsOnlineTest {
    @Test
    fun prefersTheSyncedLyricsAndKeepsTheirTimes() {
        // The times are what lets the player follow the song (T39).
        val entry = LyricsDto(plainLyrics = "Line one\nLine two", syncedLyrics = "[00:12.00]Line one\n[00:15.30]Line two")

        assertEquals("[00:12.00]Line one\n[00:15.30]Line two", wordsIn(entry))
    }

    @Test
    fun fallsBackToThePlainLyricsWhenNothingIsSynced() {
        assertEquals("Line one", wordsIn(LyricsDto(plainLyrics = "Line one", syncedLyrics = null)))
        assertEquals("Line one", wordsIn(LyricsDto(plainLyrics = "Line one", syncedLyrics = "  ")))
    }

    @Test
    fun aSyncedResultBeatsAnEarlierPlainOne() {
        // The exact lookup can answer plain only while the search holds a synced copy of the same song.
        val results = listOf(LyricsDto(plainLyrics = "Plain"), LyricsDto(plainLyrics = "Plain too", syncedLyrics = "[00:01.00]Synced"))

        assertEquals("[00:01.00]Synced", lyricsIn(results))
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
        assertNull(wordsIn(LyricsDto(instrumental = true, plainLyrics = "ignored", syncedLyrics = "[00:01.00]ignored")))
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
}
