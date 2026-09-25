package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.ui.swipeSettlesOpen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeRowTest {
    @Test
    fun aSwipePastHalfTheActionStaysOpen() {
        assertTrue(swipeSettlesOpen(offset = -60f, width = 96f, velocity = 0f))
    }

    @Test
    fun aShortSwipeSpringsBack() {
        assertFalse(swipeSettlesOpen(offset = -20f, width = 96f, velocity = 0f))
    }

    @Test
    fun aQuickFlickOpensWithoutCoveringTheWholeWidth() {
        assertTrue(swipeSettlesOpen(offset = -12f, width = 96f, velocity = -1200f))
    }

    @Test
    fun aFlickBackClosesEvenFromWideOpen() {
        assertFalse(swipeSettlesOpen(offset = -90f, width = 96f, velocity = 1500f))
    }
}
