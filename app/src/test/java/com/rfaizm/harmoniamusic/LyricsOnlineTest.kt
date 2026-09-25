package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.lyricsFrom
import com.rfaizm.harmoniamusic.data.lyricsUrl
import com.rfaizm.harmoniamusic.data.stripLrcTimestamps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsOnlineTest {
    @Test
    fun takesThePlainLyricsWhenTheyAreThere() {
        val json = """{"plainLyrics":"Line one\nLine two","syncedLyrics":"[00:12.00]Line one","instrumental":false}"""

        assertEquals("Line one\nLine two", lyricsFrom(json))
    }

    @Test
    fun fallsBackToTheSyncedLyricsWithoutTheirTimestamps() {
        val json = """{"plainLyrics":"","syncedLyrics":"[00:12.00]Line one\n[00:15.30]Line two","instrumental":false}"""

        assertEquals("Line one\nLine two", lyricsFrom(json))
    }

    @Test
    fun readsTheFirstResultOfASearch() {
        val json = """[{"plainLyrics":"From the search","instrumental":false}]"""

        assertEquals("From the search", lyricsFrom(json))
    }

    @Test
    fun aMissAnInstrumentalOrRubbishHasNoLyrics() {
        assertNull(lyricsFrom("""[]"""))
        assertNull(lyricsFrom("""{"instrumental":true,"plainLyrics":""}"""))
        assertNull(lyricsFrom("""{"plainLyrics":"   "}"""))
        assertNull(lyricsFrom("not json at all"))
    }

    @Test
    fun stripsTimestampsAndLrcHeaderLines() {
        val lrc = "[ar:Aurelia Vance]\n[ti:Quiet Harbour]\n[00:01.00]First\n[00:04.25]Second\n"

        assertEquals("First\nSecond", stripLrcTimestamps(lrc))
    }

    @Test
    fun theLookupUrlEscapesWhatItSends() {
        val song = Song(1, "Salt & Stone", "Marlowe Kei", "Coastline", 2024, 204)

        val url = lyricsUrl(song)

        assertTrue(url, url.startsWith("https://lrclib.net/api/get?"))
        assertTrue(url, "Salt+%26+Stone" in url || "Salt%20%26%20Stone" in url)
        assertTrue(url, "duration=204" in url)
    }
}
