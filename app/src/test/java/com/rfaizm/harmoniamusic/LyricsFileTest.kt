package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.lrcNameFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsFileTest {
    @Test
    fun theLyricsFileSitsBesideTheSongUnderTheSameName() {
        assertEquals("Morning Light.lrc", lrcNameFor("Morning Light.mp3"))
        assertEquals("Salt_and_Stone.lrc", lrcNameFor("Salt_and_Stone.flac"))
    }

    @Test
    fun onlyTheLastDotIsTheExtension() {
        assertEquals("Live.At.The.Park.lrc", lrcNameFor("Live.At.The.Park.m4a"))
    }

    @Test
    fun handlesOddFileNames() {
        assertEquals("NoExtension.lrc", lrcNameFor("NoExtension"))
        assertEquals("SHOUTING.lrc", lrcNameFor("SHOUTING.MP3"))
        assertNull(lrcNameFor("")) // a song scanned without a file name can't be matched
    }
}
