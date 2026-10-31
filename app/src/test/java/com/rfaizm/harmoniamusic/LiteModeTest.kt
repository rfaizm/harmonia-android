package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.SettingsRepository.Key
import com.rfaizm.harmoniamusic.data.defaultOf
import org.junit.Assert.assertEquals
import org.junit.Test

class LiteModeTest {
    @Test fun liteModeStartsOnOnlyForLowRamPhones() {
        assertEquals(true, defaultOf(Key.LiteMode, lowRam = true))
        assertEquals(false, defaultOf(Key.LiteMode, lowRam = false))
    }

    @Test fun otherSwitchesIgnoreRam() {
        for (key in Key.entries - Key.LiteMode) {
            assertEquals(key.default, defaultOf(key, lowRam = true))
            assertEquals(key.default, defaultOf(key, lowRam = false))
        }
    }
}
