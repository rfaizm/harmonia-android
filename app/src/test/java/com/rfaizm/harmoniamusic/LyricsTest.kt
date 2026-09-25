package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.parseUslt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** USLT payload: encoding byte, 3-letter language, null-terminated description, then the lyrics. */
class LyricsTest {
    @Test
    fun readsPlainLyricsWithNoDescription() {
        val payload = byteArrayOf(0) + "eng".toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0) +
            "Hello\nWorld".toByteArray(Charsets.ISO_8859_1)

        assertEquals("Hello\nWorld", parseUslt(payload))
    }

    @Test
    fun skipsTheDescriptionAndReadsUtf8() {
        val payload = byteArrayOf(3) + "ind".toByteArray(Charsets.ISO_8859_1) +
            "Lirik".toByteArray(Charsets.UTF_8) + byteArrayOf(0) +
            "Baris satu\nBaris dua".toByteArray(Charsets.UTF_8)

        assertEquals("Baris satu\nBaris dua", parseUslt(payload))
    }

    @Test
    fun readsUtf16WhereTheTerminatorIsTwoBytes() {
        val payload = byteArrayOf(1) + "eng".toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0, 0) +
            "Halo dunia".toByteArray(Charsets.UTF_16)

        assertEquals("Halo dunia", parseUslt(payload))
    }

    @Test
    fun aTruncatedOrEmptyFrameHasNoLyrics() {
        assertNull(parseUslt(byteArrayOf(0)))
        assertNull(parseUslt(byteArrayOf(0) + "eng".toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0)))
        assertNull(parseUslt(byteArrayOf(9) + "eng".toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0, 65)))
    }
}
