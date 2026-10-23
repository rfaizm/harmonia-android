package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.ui.artistLine
import com.rfaizm.harmoniamusic.ui.playerSubtitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** M11 (T46): a library full of untagged files shouldn't read "Unknown artist" on every row. */
class DisplayTest {
    private fun song(artist: String, album: String) = Song(1, "Yellow", artist, album, 2000, 269)

    @Test
    fun aKnownArtistIsShown() {
        assertEquals("Coldplay", artistLine(song("Coldplay", "Parachutes")))
    }

    @Test
    fun anUnknownArtistShowsNothingRatherThanThePlaceholder() {
        assertNull(artistLine(song("Unknown artist", "Parachutes")))
        assertNull(artistLine(song("", "Parachutes"))) // a blank tag becomes the placeholder too
    }

    @Test
    fun thePlayerSubtitleKeepsOnlyWhatIsKnown() {
        assertEquals("Coldplay · Parachutes", playerSubtitle(song("Coldplay", "Parachutes")))
        assertEquals("Parachutes", playerSubtitle(song("Unknown artist", "Parachutes")))
        assertEquals("Coldplay", playerSubtitle(song("Coldplay", "Unknown album")))
        assertNull(playerSubtitle(song("Unknown artist", "Unknown album")))
    }
}
