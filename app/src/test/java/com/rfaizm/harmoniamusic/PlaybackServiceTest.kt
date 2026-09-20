package com.rfaizm.harmoniamusic

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
