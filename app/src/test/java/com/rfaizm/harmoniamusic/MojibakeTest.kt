package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.guessCharset
import com.rfaizm.harmoniamusic.data.repairMojibake
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.Charset
import java.util.Locale

class MojibakeTest {
    /** What a tag looks like after its bytes were read as Latin-1. */
    private fun garble(text: String, charset: String) = String(text.toByteArray(Charset.forName(charset)), Charsets.ISO_8859_1)

    private val never: (ByteArray) -> String? = { error("detector should not run") }

    @Test fun plainTextIsLeftAlone() {
        assertEquals("Bohemian Rhapsody", repairMojibake("Bohemian Rhapsody", never))
        assertEquals("Привет", repairMojibake("Привет", never)) // already decoded fine
    }

    @Test fun utf8ReadAsLatin1IsFixedWithoutTheDetector() {
        assertEquals("Café del Mar", repairMojibake(garble("Café del Mar", "UTF-8"), never))
        assertEquals("夜に駆ける", repairMojibake(garble("夜に駆ける", "UTF-8"), never))
    }

    @Test fun legacyCodePagesAreFixedWithTheDetector() {
        assertEquals("Группа крови", repairMojibake(garble("Группа крови", "windows-1251")) { "windows-1251" })
        assertEquals("夜に駆ける", repairMojibake(garble("夜に駆ける", "Shift_JIS")) { "Shift_JIS" })
    }

    @Test fun realAccentsSurviveAWrongGuess() {
        // One accent in a Latin name is real text, even if the detector calls it Cyrillic.
        assertEquals("Beyoncé", repairMojibake("Beyoncé") { "windows-1251" })
        assertEquals("Mötley Crüe", repairMojibake("Mötley Crüe") { "ISO-8859-1" })
    }

    @Test fun noGuessOrABadNameKeepsTheTag() {
        val garbled = garble("Группа крови", "windows-1251")
        assertEquals(garbled, repairMojibake(garbled) { null })
        assertEquals(garbled, repairMojibake(garbled) { "not-a-charset" })
    }

    @Test fun theGuessFollowsThePhoneLanguage() {
        val cyrillic = "Группа крови".toByteArray(Charset.forName("windows-1251"))
        assertEquals("windows-1251", guessCharset(cyrillic, Locale("ru")))
        assertEquals("Shift_JIS", guessCharset("夜に駆ける".toByteArray(Charset.forName("Shift_JIS")), Locale.JAPANESE))
        assertEquals("Big5", guessCharset("月亮代表我的心".toByteArray(Charset.forName("Big5")), Locale.TAIWAN))
        assertEquals("GBK", guessCharset("月亮代表我的心".toByteArray(Charset.forName("GBK")), Locale.CHINA))
        assertNull(guessCharset(cyrillic, Locale.ENGLISH)) // no code page to guess for English
    }

    @Test fun aGuessThatDoesNotDecodeIsDropped() {
        // A lone Shift_JIS lead byte is not valid Shift_JIS.
        assertNull(guessCharset(byteArrayOf(0x41, 0x82.toByte()), Locale.JAPANESE))
    }

    @Test fun endToEndOnARussianPhone() {
        val garbled = garble("Кино", "windows-1251")
        assertEquals("Кино", repairMojibake(garbled) { guessCharset(it, Locale("ru")) })
    }
}
