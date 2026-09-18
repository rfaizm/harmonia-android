package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.cleanTag
import org.junit.Assert.assertEquals
import org.junit.Test

class CleanTagTest {
    @Test
    fun cleansMessyTags() {
        assertEquals("Morning Light", cleanTag("Morning_Light (Official Audio) [y2mate.com]"))
        assertEquals("Harbour Lights", cleanTag("y2mate.com - Harbour Lights"))
        assertEquals("Slow River", cleanTag("Slow River feat. Mira"))
        assertEquals("Moonlit Garden", cleanTag("Moonlit_Garden - prod. Kaito"))
        assertEquals("夜の散歩", cleanTag("夜の散歩"))
    }
}
