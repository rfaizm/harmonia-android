package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.LyricsQuery
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.guessFromFileName
import com.rfaizm.harmoniamusic.data.lyricsQueries
import com.rfaizm.harmoniamusic.data.matches
import com.rfaizm.harmoniamusic.data.titleWithout
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
    fun aTaglessFileTriesThePlayersArtistThenTheFileNameBothWays() {
        val queries = lyricsQueries(tagless, playerArtist = "Coldplay (Official)")

        assertEquals(
            listOf(
                LyricsQuery("Coldplay", "Yellow"),          // what the player read, taken out of the title we have
                LyricsQuery("Coldplay", "Coldplay Yellow"), // the title whole, in case it really starts that way
                LyricsQuery("Yellow", "Coldplay", fromFileName = true), // the file name read the other way round
            ),
            queries,
        )
    }

    // Which half of the file name is the artist

    @Test
    fun aFileNamedTitleFirstIsTriedTheOtherWayRoundToo() {
        // "Title - Artist" is common too, and searching the artist as the title found nothing for this file.
        val song = Song(6, "Viva la Vida - Coldplay", "Unknown artist", "Unknown album", 0, 242, fileName = "Viva la Vida - Coldplay.mp3")

        assertEquals(
            listOf(LyricsQuery("Viva la Vida", "Coldplay", fromFileName = true), LyricsQuery("Coldplay", "Viva la Vida", fromFileName = true)),
            lyricsQueries(song, playerArtist = null),
        )
    }

    @Test
    fun theArtistComesOffATitleGuessedFromTheFileNameToo() {
        // Some downloaders add the channel name after the video title, so the artist ends up in the title part.
        val song = Song(
            9, "Harry Styles - Sign of the Times - Harry Styles", "Unknown artist", "Unknown album", 0, 341,
            fileName = "Harry Styles - Sign of the Times - Harry Styles.mp3",
        )

        assertEquals(LyricsQuery("Harry Styles", "Sign of the Times", fromFileName = true), lyricsQueries(song, playerArtist = null).first())
    }

    @Test
    fun halfOfAFileNameIsNeverSearchedAsTheTitleAlone() {
        // Either half may be the artist, and the title "Coldplay" alone found a mislabelled upload of Clocks.
        val song = Song(7, "The Scientist - Coldplay", "Unknown artist", "Unknown album", 0, 309, fileName = "The Scientist - Coldplay.mp3")

        assertTrue(lyricsQueries(song, playerArtist = null).none { it.artist == null })
    }

    @Test
    fun aRealTitleTagIsStillSearchedAlone() {
        val song = Song(8, "The Scientist", "Unknown artist", "Unknown album", 0, 309, fileName = "track01.mp3")

        assertEquals(LyricsQuery(null, "The Scientist"), lyricsQueries(song, playerArtist = null).last())
    }

    // The artist inside the title

    @Test
    fun theArtistComesOffEitherEndOfTheTitle() {
        // Downloaded songs are often titled "Artist - Title" or "Title - Artist", and the service only knows the title.
        val titleFirst = Song(3, "Sign of the Times - Harry Styles", "Harry Styles", "Unknown album", 0, 341)
        val artistFirst = Song(4, "Harry Styles - Sign of the Times", "Harry Styles", "Unknown album", 0, 341)

        assertEquals(LyricsQuery("Harry Styles", "Sign of the Times"), lyricsQueries(titleFirst, playerArtist = null).first())
        assertEquals(LyricsQuery("Harry Styles", "Sign of the Times"), lyricsQueries(artistFirst, playerArtist = null).first())
    }

    @Test
    fun aTitleThatReallyStartsWithTheArtistsNameIsTriedWholeToo() {
        val song = Song(5, "Queen of the Night", "Queen", "Unknown album", 0, 200)

        assertEquals(
            listOf(LyricsQuery("Queen", "of the Night"), LyricsQuery("Queen", "Queen of the Night")),
            lyricsQueries(song, playerArtist = null).take(2),
        )
    }

    @Test
    fun onlyTheArtistAsWholeWordsComesOff() {
        assertEquals("Museum", titleWithout("Muse", "Museum"))
        assertEquals("Adele's Song", titleWithout("Adele", "Adele's Song"))
        assertEquals("Weezer", titleWithout("Weezer", "Weezer")) // a self-titled song keeps its name
        assertEquals("Hello", titleWithout("adele", "ADELE | Hello"))
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
    fun aGuessFromTheFileNameNeedsTheArtistToMatchToo() {
        // Real LRCLIB uploads swap the fields: title "Coldplay", artist "Clocks", 308.6 s, nearly the length of
        // "The Scientist". Read the wrong way round, that file name would take it on the length alone.
        val mislabelled = LyricsDto(trackName = "Coldplay", artistName = "Clocks", duration = 308.610612)
        val right = LyricsDto(trackName = "The Scientist", artistName = "Coldplay", duration = 309.0)

        assertFalse(matches(mislabelled, LyricsQuery("The Scientist", "Coldplay", fromFileName = true), durationSec = 309))
        assertTrue(matches(right, LyricsQuery("Coldplay", "The Scientist", fromFileName = true), durationSec = 309))
    }

    @Test
    fun anEntryWithNothingToConfirmItIsRejected() {
        val unknown = LyricsDto(trackName = "Yellow", artistName = null, duration = null)

        assertFalse(matches(unknown, LyricsQuery(null, "Yellow"), durationSec = 269))
    }
}
