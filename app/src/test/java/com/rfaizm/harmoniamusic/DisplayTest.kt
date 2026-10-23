package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.SettingsRepository.Theme
import com.rfaizm.harmoniamusic.ui.artistLine
import com.rfaizm.harmoniamusic.ui.playerSubtitle
import com.rfaizm.harmoniamusic.ui.themeIsDark
import com.rfaizm.harmoniamusic.ui.tileLetter
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

    // T47: a tile without cover art shows a letter, so a library of untagged files isn't a wall of music notes.

    @Test
    fun aTileShowsTheFirstLetterWithAccentsFolded() {
        assertEquals("Y", tileLetter("yellow"))
        assertEquals("E", tileLetter("Élan"))
    }

    @Test
    fun aTitleWithNoLetterKeepsTheMusicNote() {
        // "#" on a tile would say nothing, so numbers, symbols and other scripts keep the icon.
        assertNull(tileLetter("21 Guns"))
        assertNull(tileLetter("夜に駆ける"))
        assertNull(tileLetter(null))
    }

    // T51: the app follows the phone's theme until the user picks one in Settings.

    @Test
    fun systemFollowsThePhone() {
        assertEquals(true, themeIsDark(Theme.System, systemDark = true))
        assertEquals(false, themeIsDark(Theme.System, systemDark = false))
    }

    @Test
    fun aChosenThemeWinsOverThePhone() {
        assertEquals(false, themeIsDark(Theme.Light, systemDark = true))
        assertEquals(true, themeIsDark(Theme.Dark, systemDark = false))
    }
}
