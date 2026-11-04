package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.ScanSummary
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.scanSummary
import org.junit.Assert.assertEquals
import org.junit.Test

/** L2: what a rescan found, for the message after Settings › Rescan library. Songs are matched by their id. */
class ScanSummaryTest {
    private fun songs(vararg ids: Int) = ids.map { Song(it, "t$it", "a", "b", 0, 200) }

    @Test fun nothingChanged() {
        assertEquals(ScanSummary(total = 3, added = 0, removed = 0), scanSummary(songs(1, 2, 3), songs(1, 2, 3)))
    }

    @Test fun newAndGoneSongsAreCountedSeparately() {
        assertEquals(ScanSummary(total = 4, added = 2, removed = 1), scanSummary(songs(1, 2, 3), songs(2, 3, 4, 5)))
    }

    @Test fun aSongWhoseTagsChangedIsNotNew() {
        val retagged = songs(1).map { it.copy(title = "Better title") }
        assertEquals(ScanSummary(total = 1, added = 0, removed = 0), scanSummary(songs(1), retagged))
    }
}
