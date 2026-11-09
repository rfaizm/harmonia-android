package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.SettingsRepository.Key
import com.rfaizm.harmoniamusic.data.local.selectionFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** L4: Settings › "Hide clips shorter than 30 seconds" decides what the MediaStore query keeps. */
class ShortClipFilterTest {
    @Test fun onHidesClipsUnder30Seconds() {
        assertEquals("is_music != 0 AND duration >= 30000", selectionFor(hideShort = true))
    }

    @Test fun offKeepsEveryLength() {
        assertEquals("is_music != 0", selectionFor(hideShort = false))
    }

    @Test fun onByDefaultSoNothingChangesForAnyone() {
        assertTrue(Key.HideShortClips.default)
    }
}
