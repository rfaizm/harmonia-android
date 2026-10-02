package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.LyricLine
import com.rfaizm.harmoniamusic.data.currentLine
import com.rfaizm.harmoniamusic.data.parseLrc
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

    @Test
    fun readsTheTimeOfEachLrcLineAndSkipsTheHeader() {
        val lrc = "[ar:Coldplay]\n[ti:Yellow]\n[00:33.10]Look at the stars\n[00:37.5]Look how they shine for you\n[01:02.123]And everything you do"

        assertEquals(
            listOf(LyricLine(33_100, "Look at the stars"), LyricLine(37_500, "Look how they shine for you"), LyricLine(62_123, "And everything you do")),
            parseLrc(lrc),
        )
    }

    @Test
    fun aLineWithSeveralTimesIsSungAtEachOfThem() {
        // A chorus is often written once, with every time it comes back.
        val lrc = "[00:10.00]Verse\n[00:20.00][01:00.00]Chorus\n[00:40.00]Bridge"

        assertEquals(
            listOf(10_000L to "Verse", 20_000L to "Chorus", 40_000L to "Bridge", 60_000L to "Chorus"),
            parseLrc(lrc).map { it.timeMs to it.text },
        )
    }

    @Test
    fun theOtherTimeFormatsAndWordTimingsAreUnderstood() {
        // [mm:ss] with no fraction, [mm:ss:xx] with a colon, and enhanced LRC's per-word <mm:ss.xx> marks.
        val lrc = "[00:05]Plain\n[00:07:50]Colon\n[00:09.00]<00:09.00>Word <00:09.40>by <00:09.80>word"

        assertEquals(listOf(LyricLine(5_000, "Plain"), LyricLine(7_500, "Colon"), LyricLine(9_000, "Word by word")), parseLrc(lrc))
    }

    @Test
    fun theOffsetTagMovesEveryLine() {
        // LRC's rule: a positive offset shows the lyrics earlier.
        assertEquals(listOf(LyricLine(9_500, "Line")), parseLrc("[offset:+500]\n[00:10.00]Line"))
        assertEquals(listOf(LyricLine(10_250, "Line")), parseLrc("[offset:-250]\n[00:10.00]Line"))
    }

    @Test
    fun aBlankTimedLineIsAPauseInTheSinging() {
        assertEquals(
            listOf(LyricLine(1_000, "Sung"), LyricLine(4_000, ""), LyricLine(9_000, "Again")),
            parseLrc("[00:01.00]Sung\n[00:04.00]\n[00:09.00]Again"),
        )
    }

    @Test
    fun lyricsWithoutTimesAreNotSynced() {
        assertEquals(emptyList<LyricLine>(), parseLrc("Look at the stars\nLook how they shine for you"))
    }

    @Test
    fun theCurrentLineIsTheLastOneAlreadyReached() {
        val lines = listOf(LyricLine(10_000, "One"), LyricLine(20_000, "Two"), LyricLine(30_000, "Three"))

        assertEquals(-1, currentLine(lines, 9_999)) // the intro, before anything is sung
        assertEquals(0, currentLine(lines, 10_000))
        assertEquals(1, currentLine(lines, 29_999))
        assertEquals(2, currentLine(lines, 300_000))
    }
}
