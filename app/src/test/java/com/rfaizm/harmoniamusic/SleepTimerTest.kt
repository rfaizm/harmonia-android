package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.ui.SleepOption
import com.rfaizm.harmoniamusic.ui.SleepTimer
import com.rfaizm.harmoniamusic.ui.countdownText
import com.rfaizm.harmoniamusic.ui.sleepTimerOf
import com.rfaizm.harmoniamusic.ui.countdownParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

/** M10: the time left on the sleep timer, as the chip and the dialog show it. */
class SleepTimerTest {
    @Test
    fun theCountdownReadsMinutesAndSeconds() {
        assertEquals("12:34", countdownText(754_000))
        assertEquals("1:00", countdownText(60_000))
        assertEquals("0:00", countdownText(0))
    }

    @Test
    fun aCountdownPastAnHourShowsTheHours() {
        assertEquals("1:05:09", countdownText(3_909_000))
    }

    @Test
    fun theCountdownRoundsUpSoItReadsZeroOnlyWhenTheMusicStops() {
        assertEquals("0:01", countdownText(400))
        assertEquals("1:00", countdownText(59_001))
    }

    @Test
    fun aRunningTimerCountsAsLeftEvenIfTheTickIsLate() {
        assertEquals("0:00", countdownText(-300))
    }

    @Test
    fun talkBackHearsTheTimeLeftInWords() {
        // "12:34" alone is read like a clock time, which is when the timer would end, not how long is left.
        // The screen turns these into "12 minutes 34 seconds left" in the phone's language.
        assertEquals(listOf(MINUTES to 12L, SECONDS to 34L), countdownParts(754_000))
        assertEquals(listOf(MINUTES to 1L, SECONDS to 1L), countdownParts(61_000))
        assertEquals(listOf(HOURS to 1L, MINUTES to 5L), countdownParts(3_909_000))
        assertEquals(listOf(HOURS to 2L), countdownParts(7_200_000))
        assertEquals(listOf(SECONDS to 1L), countdownParts(400))
    }

    @Test
    fun theServicesExtrasBecomeTheRunningTimer() {
        assertEquals(SleepTimer(SleepOption.Min30, endsAt = 123_456L), sleepTimerOf(minutes = 30, endsAt = 123_456L))
        assertEquals(SleepTimer(SleepOption.EndOfTrack, endsAt = 0L), sleepTimerOf(minutes = SLEEP_END_OF_TRACK, endsAt = 0L))
    }

    @Test
    fun noTimerOnceItHasFired() {
        assertNull(sleepTimerOf(minutes = 0, endsAt = 0L))
    }
}
