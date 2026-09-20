package com.rfaizm.harmoniamusic

import androidx.media3.common.C
import com.rfaizm.harmoniamusic.data.SEED_SONGS
import com.rfaizm.harmoniamusic.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlaybackServiceTest {
    @Test
    fun aBrokenSongSkipsToTheNextOne() {
        assertTrue(shouldSkipAfterError(errorsInARow = 1, queueSize = 5, hasNext = true))
    }

    @Test
    fun anAllBrokenQueueOnRepeatStopsAfterTryingEachSongOnce() {
        // Repeat-all always has a next song, so only the error count can end the loop.
        val skipped = (1..10).takeWhile { shouldSkipAfterError(errorsInARow = it, queueSize = 3, hasNext = true) }

        assertEquals(listOf(1, 2), skipped) // errors 1 and 2 skip ahead; the 3rd song's error stops
    }

    @Test
    fun aBrokenLastSongWithRepeatOffStops() {
        assertFalse(shouldSkipAfterError(errorsInARow = 1, queueSize = 5, hasNext = false))
    }

    @Test
    fun duckingOnKeepsMusicPlayingQuietlyForNotifications() {
        assertEquals(C.AUDIO_CONTENT_TYPE_MUSIC, audioAttributesFor(ducking = true).contentType)
    }

    @Test
    fun duckingOffPausesForNotificationsInstead() {
        // Media3 ducks for every content type but speech, so that flag is the only lever for this setting.
        assertEquals(C.AUDIO_CONTENT_TYPE_SPEECH, audioAttributesFor(ducking = false).contentType)
    }

    @Test
    fun playAfterUnplugCapsTheSpeakerAtThirtyPercent() {
        assertEquals(5, speakerSafeVolume(current = 12, max = 15)) // 30% of 15 steps is 4.5, rounded to 5
    }

    @Test
    fun playAfterUnplugNeverRaisesAnAlreadyQuietSpeaker() {
        assertEquals(3, speakerSafeVolume(current = 3, max = 15))
    }

    @Test
    fun theResumedQueueStaysOnTheSameSong() {
        val library = listOf(song(1), song(2), song(3))

        val (queue, index) = resumeQueue(ids = listOf(1, 2, 3), index = 2, songs = library)

        assertEquals(library, queue)
        assertEquals(2, index)
    }

    @Test
    fun aSongDeletedSinceLastTimeShiftsTheResumedIndex() {
        // Saved song 1 is gone after a rescan, so song 3 is now at index 1, not 2.
        val (queue, index) = resumeQueue(ids = listOf(1, 2, 3), index = 2, songs = listOf(song(2), song(3)))

        assertEquals(listOf(song(2), song(3)), queue)
        assertEquals(1, index)
    }

    @Test
    fun aQueueWhoseSongsAreAllGoneResumesNothing() {
        val (queue, index) = resumeQueue(ids = listOf(1, 2), index = 1, songs = emptyList())

        assertEquals(emptyList<Song>(), queue)
        assertEquals(0, index)
    }

    @Test
    fun theSleepTimerPlaysAtFullVolumeUntilTheLastMinute() {
        assertEquals(1f, fadeVolume(msLeft = 90_000), 0f)
        assertEquals(1f, fadeVolume(msLeft = 60_000), 0f)
    }

    @Test
    fun theSleepTimerFadesOutOverTheLastMinute() {
        assertEquals(0.25f, fadeVolume(msLeft = 30_000), 0.001f) // squared curve, so half way is a quarter as loud
        assertEquals(0f, fadeVolume(msLeft = 0), 0f)
    }

    @Test
    fun theSleepTimerStaysSilentIfTheTickRunsLate() {
        assertEquals(0f, fadeVolume(msLeft = -500), 0f)
    }

    @Test
    fun smartShufflePlaysEverySongExactlyOnce() {
        val artists = SEED_SONGS.map { it.displayArtist }

        val order = smartShuffle(artists, first = 0, random = Random(1))

        assertEquals(artists.indices.toList(), order.sorted())
    }

    @Test
    fun smartShuffleNeverPlacesTwoSongsByTheSameArtistInARow() {
        val artists = SEED_SONGS.map { it.displayArtist }

        repeat(1000) { seed ->
            val order = smartShuffle(artists, first = seed % artists.size, random = Random(seed.toLong()))
            val sameArtistInARow = order.zipWithNext().count { (a, b) -> artists[a] == artists[b] }

            assertEquals("seed $seed", 0, sameArtistInARow)
        }
    }

    @Test
    fun smartShuffleStartsWithTheSongThatIsPlaying() {
        val artists = SEED_SONGS.map { it.displayArtist }

        assertEquals(7, smartShuffle(artists, first = 7, random = Random(2)).first())
    }

    private fun song(id: Int) = Song(id, "Song $id", "Artist", "Album", 2024, 200)
}
