package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.ui.SleepOption
import com.rfaizm.harmoniamusic.ui.SleepTimer
import com.rfaizm.harmoniamusic.ui.countdownText
import com.rfaizm.harmoniamusic.ui.sleepTimerOf
import com.rfaizm.harmoniamusic.ui.spokenCountdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
        assertEquals("12 minutes 34 seconds left", spokenCountdown(754_000))
        assertEquals("1 minute 1 second left", spokenCountdown(61_000))
        assertEquals("1 hour 5 minutes left", spokenCountdown(3_909_000))
        assertEquals("2 hours left", spokenCountdown(7_200_000))
        assertEquals("1 second left", spokenCountdown(400))
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
