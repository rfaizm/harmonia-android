package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.LyricsQuery
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.guessFromFileName
import com.rfaizm.harmoniamusic.data.lyricsQueries
import com.rfaizm.harmoniamusic.data.matches
import com.rfaizm.harmoniamusic.data.remote.dto.LyricsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC.md S1 (file name), S2 (the player's own tags) and S3 (verify before showing). */
class LyricsMatchTest {
    private val tagless = Song(1, "Coldplay - Yellow", "Unknown artist", "Unknown album", 0, 269, fileName = "Coldplay - Yellow.mp3")

    // S1: the file name

    @Test
    fun readsTheArtistAndTitleFromAFileName() {
        assertEquals("Coldplay" to "Yellow", guessFromFileName("Coldplay - Yellow.mp3"))
    }

    @Test
    fun dropsDownloadSiteJunkAndBracketsFromTheName() {
        assertEquals("Coldplay" to "Yellow", guessFromFileName("y2mate.com - Coldplay - Yellow (Official Video).mp3"))
    }

    @Test
    fun acceptsTheLongerDashesPeopleType() {
        assertEquals("Coldplay" to "Yellow", guessFromFileName("Coldplay – Yellow.mp3"))
    }

    @Test
    fun aNameWithNoSeparatorOrOnlyATrackNumberIsNoGuess() {
        assertNull(guessFromFileName("Yellow.mp3"))
        assertNull(guessFromFileName("01 - Yellow.mp3"))
        assertNull(guessFromFileName(""))
    }

    // S2 and the order things are tried in

    @Test
    fun aSongsOwnTagsAreTriedFirst() {
        val tagged = Song(2, "Yellow", "Coldplay", "Parachutes", 2000, 269, fileName = "track.mp3")

        assertEquals(LyricsQuery("Coldplay", "Yellow"), lyricsQueries(tagged, playerArtist = null).first())
    }

    @Test
    fun aTaglessFileTriesThePlayersArtistThenTheFileNameThenTheTitleAlone() {
        val queries = lyricsQueries(tagless, playerArtist = "Coldplay (Official)")

        assertEquals(
            listOf(
                LyricsQuery("Coldplay", "Coldplay Yellow"), // what the player read, with the title we have
                LyricsQuery("Coldplay", "Yellow"),          // what the file name says
                LyricsQuery(null, "Yellow"),                // last resort, only trusted if the length matches
            ),
            queries,
        )
    }

    @Test
    fun thePlaceholderIsNeverSearchedFor() {
        val queries = lyricsQueries(tagless, playerArtist = "Unknown artist")

        assertTrue(queries.none { it.artist == "Unknown artist" })
    }

    // S3: verify before showing

    @Test
    fun anotherSongWithASimilarTitleIsRejected() {
        // The live search for "Yellow" ranks this first; showing it would be another artist's song.
        val wrong = LyricsDto(trackName = "Yellow Yellow", artistName = "FANTASTICS from EXILE TRIBE", duration = 237.0)

        assertFalse(matches(wrong, LyricsQuery(null, "Yellow"), durationSec = 269))
    }

    @Test
    fun theSameTitleBySomeoneElseMustAlsoMatchTheLength() {
        val cover = LyricsDto(trackName = "Yellow", artistName = "Someone Else", duration = 301.0)

        assertFalse(matches(cover, LyricsQuery("Coldplay", "Yellow"), durationSec = 269))
        assertTrue(matches(cover.copy(duration = 270.5), LyricsQuery(null, "Yellow"), durationSec = 269))
    }

    @Test
    fun theRightArtistIsTrustedEvenWhenTheLengthDiffers() {
        // A live version or a video rip runs longer, but the words are the same song's.
        val live = LyricsDto(trackName = "Yellow (Live)", artistName = "Coldplay", duration = 291.0)

        assertTrue(matches(live, LyricsQuery("Coldplay", "Yellow"), durationSec = 269))
    }

    @Test
    fun anEntryWithNothingToConfirmItIsRejected() {
        val unknown = LyricsDto(trackName = "Yellow", artistName = null, duration = null)

        assertFalse(matches(unknown, LyricsQuery(null, "Yellow"), durationSec = 269))
    }
}
