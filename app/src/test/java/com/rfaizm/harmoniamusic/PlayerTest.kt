package com.rfaizm.harmoniamusic

import androidx.media3.common.Player
import com.rfaizm.harmoniamusic.ui.SleepOption
import com.rfaizm.harmoniamusic.ui.sleepOptionOf
import com.rfaizm.harmoniamusic.ui.nextRepeatMode
import com.rfaizm.harmoniamusic.ui.progressOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerTest {
    @Test
    fun repeatButtonCyclesOffThenAllThenOne() {
        assertEquals(Player.REPEAT_MODE_ALL, nextRepeatMode(Player.REPEAT_MODE_OFF))
        assertEquals(Player.REPEAT_MODE_ONE, nextRepeatMode(Player.REPEAT_MODE_ALL))
        assertEquals(Player.REPEAT_MODE_OFF, nextRepeatMode(Player.REPEAT_MODE_ONE))
    }

    @Test
    fun progressIsPositionOverTheSongLength() {
        assertEquals(0.25f, progressOf(positionMs = 50_000, durationSec = 200), 0.0001f)
    }

    @Test
    fun progressClampsWhenPlaybackRunsPastTheMediaStoreDuration() {
        // MediaStore rounds durations, so the player can report a position a little past the end.
        assertEquals(1f, progressOf(positionMs = 201_500, durationSec = 200), 0f)
    }

    @Test
    fun theSleepChipShowsWhicheverTimerTheServiceIsRunning() {
        assertEquals(SleepOption.Min15, sleepOptionOf(15))
        assertEquals(SleepOption.EndOfTrack, sleepOptionOf(SLEEP_END_OF_TRACK))
    }

    @Test
    fun theSleepChipShowsOffOnceTheTimerHasFired() {
        // The service reports 0 when the timer fires or is switched off; the chip must stop claiming one is set.
        assertNull(sleepOptionOf(0))
    }
}
