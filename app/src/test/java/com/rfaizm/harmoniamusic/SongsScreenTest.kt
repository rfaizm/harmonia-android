package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.ui.azOrder
import com.rfaizm.harmoniamusic.ui.edgeScrollSpeed
import com.rfaizm.harmoniamusic.ui.letterAt
import com.rfaizm.harmoniamusic.ui.letterOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongsScreenTest {
    // T41: the A–Z groups the index bar jumps between

    @Test
    fun aTitleIsFiledUnderItsFirstLetterWithAccentsIgnored() {
        assertEquals("Y", letterOf("Yellow"))
        assertEquals("Y", letterOf("yellow"))
        assertEquals("E", letterOf("Élan"))
        assertEquals("E", letterOf("éclair"))
    }

    @Test
    fun anythingElseIsFiledUnderHash() {
        assertEquals("#", letterOf("21 Guns"))
        assertEquals("#", letterOf("夜に駆ける"))
        assertEquals("#", letterOf(""))
    }

    @Test
    fun everyLetterGroupAppearsOnceWithHashLast() {
        // Digits sort before "a" and accented or non-Latin titles after "z", so sorting by title alone gave two "#"
        // groups, and their two headings shared one list key, which crashes a LazyColumn.
        val songs = listOf("Zebra", "21 Guns", "élan", "夜に駆ける", "apple", "Yellow", "Eagle").mapIndexed { i, t -> song(i, t) }

        val order = azOrder(songs)

        assertEquals(listOf("A", "E", "E", "Y", "Z", "#", "#"), order.map { it.first })
        assertEquals(listOf("apple", "Eagle", "élan", "Yellow", "Zebra"), order.take(5).map { it.second.title })
    }

    @Test
    fun theFingerPicksTheLetterItIsOn() {
        assertEquals(0, letterAt(y = 0f, height = 270, count = 27))
        assertEquals(1, letterAt(y = 15f, height = 270, count = 27))
        assertEquals(26, letterAt(y = 269f, height = 270, count = 27))
    }

    @Test
    fun slidingPastEitherEndOfTheBarKeepsTheEndLetter() {
        assertEquals(0, letterAt(y = -40f, height = 270, count = 27))
        assertEquals(26, letterAt(y = 400f, height = 270, count = 27))
    }

    private fun song(id: Int, title: String) = Song(id, title, "Artist", "Album", 2024, 200)

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
