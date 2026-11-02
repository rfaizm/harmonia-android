package com.rfaizm.harmoniamusic.ui

import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfaizm.harmoniamusic.MAX_SLEEP_MS
import com.rfaizm.harmoniamusic.MIN_SLEEP_MS
import com.rfaizm.harmoniamusic.SLEEP_END_OF_TRACK
import com.rfaizm.harmoniamusic.SLEEP_STEP_MS
import com.rfaizm.harmoniamusic.ui.theme.HarmoniaTheme
import com.rfaizm.harmoniamusic.ui.theme.card
import com.rfaizm.harmoniamusic.ui.theme.muted
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground
import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rfaizm.harmoniamusic.R

/** PRD phase 7. [SLEEP_END_OF_TRACK] finishes the current song instead of counting minutes. */
enum class SleepOption(val minutes: Int) {
    Min15(15),
    Min30(30),
    Min45(45),
    Min60(60),
    EndOfTrack(SLEEP_END_OF_TRACK),
}

@Composable
private fun SleepOption.label() =
    if (this == SleepOption.EndOfTrack) stringResource(R.string.sleep_end_of_track) else stringResource(R.string.sleep_minutes, minutes)

/** The option the service reports it is running, or null for "Off" (0 means none, including "just fired"). */
internal fun sleepOptionOf(minutes: Int) = SleepOption.entries.firstOrNull { it.minutes == minutes }

/**
 * The timer the service is running (M10): what was picked, and the `elapsedRealtime` moment it pauses the music, 0
 * for end of track. The service owns the deadline, so the chip and the dialog can't drift from when the music stops.
 */
data class SleepTimer(val option: SleepOption, val endsAt: Long)

internal fun sleepTimerOf(minutes: Int, endsAt: Long): SleepTimer? = sleepOptionOf(minutes)?.let { SleepTimer(it, endsAt) }

/** "12:34", or "1:05:09" past an hour. Rounded up, so it reads 0:00 only once the music has stopped. */
internal fun countdownText(msLeft: Long): String {
    val (h, m, s) = hms(msLeft)
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** The two largest units of what's left, e.g. 12 minutes and 34 seconds; a zero second unit is dropped. */
internal fun countdownParts(msLeft: Long): List<Pair<TimeUnit, Long>> {
    val (h, m, s) = hms(msLeft)
    return when {
        h > 0 -> listOfNotNull(HOURS to h, (MINUTES to m).takeIf { m > 0 })
        m > 0 -> listOfNotNull(MINUTES to m, (SECONDS to s).takeIf { s > 0 })
        else -> listOf(SECONDS to s)
    }
}

/** What TalkBack reads: "12:34" alone sounds like a clock time, which is when the timer ends, not what's left. */
@Composable
private fun spokenCountdown(msLeft: Long): String {
    val parts = countdownParts(msLeft).map { (unit, n) ->
        val id = when (unit) { HOURS -> R.plurals.hours; MINUTES -> R.plurals.minutes; else -> R.plurals.seconds }
        pluralStringResource(id, n.toInt(), n.toInt())
    }
    return stringResource(R.string.sleep_left, parts.joinToString(" "))
}

private fun hms(msLeft: Long): Triple<Long, Long, Long> {
    val total = (msLeft.coerceAtLeast(0) + 999) / 1000
    return Triple(total / 3600, total % 3600 / 60, total % 60)
}

/** Time left until [endsAt], updated on each whole second of the countdown so the digits change in step with it. */
@Composable
private fun rememberMsLeft(endsAt: Long): Long {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(endsAt) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay((endsAt - now).mod(1000L).takeIf { it > 0 } ?: 1000L)
        }
    }
    return endsAt - now
}

/**
 * The full player's sleep chip. With no timer it offers the options; with one running it counts down
 * ("Sleep · 12:34") and opens [SleepTimerDialog]. [songMsLeft] is what's left of the song, for "End of track";
 * [onAdjust] moves a running timer by the dialog's − / + steps.
 */
@Composable
internal fun SleepTimerChip(sleep: SleepTimer?, songMsLeft: Long, onSelect: (SleepOption?) -> Unit, onAdjust: (deltaMs: Long) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var dialogOpen by remember { mutableStateOf(false) }
    // A timer that fires or is switched off elsewhere closes the dialog, and the next timer doesn't reopen it.
    LaunchedEffect(sleep == null) { if (sleep == null) dialogOpen = false }

    Box {
        when {
            sleep == null -> ExtraChip(Icons.Rounded.Bedtime, stringResource(R.string.sleep_timer)) { menuOpen = true }
            sleep.option == SleepOption.EndOfTrack -> ExtraChip(Icons.Rounded.Bedtime, stringResource(R.string.sleep_chip_end_of_track), active = true) { dialogOpen = true }
            else -> ExtraChip(Icons.Rounded.Bedtime, stringResource(R.string.sleep_chip_countdown, countdownText(rememberMsLeft(sleep.endsAt))), active = true) { dialogOpen = true }
        }
        DropdownMenu(menuOpen, { menuOpen = false }, shape = RoundedCornerShape(16.dp), containerColor = colors.card) {
            SleepOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label(), fontSize = 14.sp, color = colors.onSurface) },
                    onClick = { onSelect(option); menuOpen = false }
                )
            }
        }
    }
    if (dialogOpen && sleep != null) {
        SleepTimerDialog(sleep, songMsLeft, onAdjust, onTurnOff = { onSelect(null); dialogOpen = false }, onDismiss = { dialogOpen = false })
    }
}

/**
 * M10: how long until the sleep timer pauses the music. Built like the app's other dialogs (`DeleteFileDialog`): a
 * tinted icon circle, an extra-bold title and muted text, with the time left as large sage digits, since type does
 * the heavy lifting here (GUIDELINE §1). Nunito's digits all share one width, so the countdown doesn't jiggle.
 */
@Composable
private fun SleepTimerDialog(
    sleep: SleepTimer,
    songMsLeft: Long,
    onAdjust: (deltaMs: Long) -> Unit,
    onTurnOff: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val endOfTrack = sleep.option == SleepOption.EndOfTrack
    val msLeft = if (endOfTrack) songMsLeft else rememberMsLeft(sleep.endsAt)
    val caption = when {
        endOfTrack -> stringResource(R.string.sleep_pauses_at_end)
        msLeft <= 60_000 -> stringResource(R.string.sleep_fading)
        // DateFormat follows the phone's 12- or 24-hour setting.
        else -> stringResource(R.string.sleep_pauses_at, DateFormat.getTimeFormat(context).format(Date(System.currentTimeMillis() + msLeft)))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(Modifier.size(48.dp).background(colors.primary.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Bedtime, null, tint = colors.primary)
            }
        },
        title = { Text(stringResource(R.string.sleep_timer), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val spoken = spokenCountdown(msLeft)
                Text(
                    countdownText(msLeft),
                    fontSize = 48.sp, lineHeight = 52.sp, fontWeight = FontWeight.ExtraBold, color = colors.primary,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
                )
                Text(
                    caption, fontSize = 14.sp, color = colors.mutedForeground, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
                // "End of track" has no deadline to move. "−" stops at a minute left, "+" at three hours (T44).
                if (!endOfTrack) Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdjustPill(stringResource(R.string.sleep_minus_5), stringResource(R.string.sleep_remove_5), enabled = msLeft > MIN_SLEEP_MS) { onAdjust(-SLEEP_STEP_MS) }
                    AdjustPill(stringResource(R.string.sleep_plus_5), stringResource(R.string.sleep_add_5), enabled = msLeft < MAX_SLEEP_MS) { onAdjust(SLEEP_STEP_MS) }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)) {
                Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onTurnOff) { Text(stringResource(R.string.turn_off), color = colors.mutedForeground) } },
    )
}

/** A muted pill like the inactive sort pills (GUIDELINE §5.4), sized for a thumb. */
@Composable
private fun AdjustPill(text: String, spoken: String, enabled: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .alpha(if (enabled) 1f else 0.4f) // the whole pill fades when it can't be used
            .clip(CircleShape)
            .background(colors.muted)
            .clickable(enabled = enabled, role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = spoken }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F4F0, heightDp = 480)
@Composable
private fun SleepTimerDialogPreview() = HarmoniaTheme(darkTheme = false) {
    SleepTimerDialog(SleepTimer(SleepOption.Min30, SystemClock.elapsedRealtime() + 754_000), songMsLeft = 0, onAdjust = {}, onTurnOff = {}, onDismiss = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2022, heightDp = 480)
@Composable
private fun SleepTimerDialogDarkPreview() = HarmoniaTheme(darkTheme = true) {
    SleepTimerDialog(SleepTimer(SleepOption.EndOfTrack, 0), songMsLeft = 133_000, onAdjust = {}, onTurnOff = {}, onDismiss = {})
}
