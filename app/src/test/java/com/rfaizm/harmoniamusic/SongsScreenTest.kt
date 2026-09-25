package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.ui.edgeScrollSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongsScreenTest {
    @Test
    fun aSelectionDragInTheMiddleOfTheListDoesNotScroll() {
        assertEquals(0f, edgeScrollSpeed(y = 500f, height = 1000), 0f)
    }

    @Test
    fun draggingNearTheTopScrollsBackUp() {
        assertTrue(edgeScrollSpeed(y = 20f, height = 1000) < 0f)
    }

    @Test
    fun draggingNearTheBottomScrollsDown() {
        assertTrue(edgeScrollSpeed(y = 980f, height = 1000) > 0f)
    }

    @Test
    fun theCloserToTheEdgeTheFasterItScrolls() {
        assertTrue(edgeScrollSpeed(y = 5f, height = 1000) < edgeScrollSpeed(y = 100f, height = 1000))
        assertTrue(edgeScrollSpeed(y = 995f, height = 1000) > edgeScrollSpeed(y = 900f, height = 1000))
    }
}
