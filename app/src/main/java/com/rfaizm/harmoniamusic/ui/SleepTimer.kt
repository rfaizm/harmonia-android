package com.rfaizm.harmoniamusic.ui

import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfaizm.harmoniamusic.SLEEP_END_OF_TRACK
import com.rfaizm.harmoniamusic.ui.theme.HarmoniaTheme
import com.rfaizm.harmoniamusic.ui.theme.card
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground
import kotlinx.coroutines.delay
import java.util.Date

/** PRD phase 7. [SLEEP_END_OF_TRACK] finishes the current song instead of counting minutes. */
enum class SleepOption(val label: String, val minutes: Int) {
    Min15("15 min", 15),
    Min30("30 min", 30),
    Min45("45 min", 45),
    Min60("60 min", 60),
    EndOfTrack("End of track", SLEEP_END_OF_TRACK),
}

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

/** What TalkBack reads: "12:34" alone sounds like a clock time, which is when the timer ends, not what's left. */
internal fun spokenCountdown(msLeft: Long): String {
    val (h, m, s) = hms(msLeft)
    fun unit(n: Long, name: String) = if (n == 1L) "1 $name" else "$n ${name}s"
    val parts = when {
        h > 0 -> listOfNotNull(unit(h, "hour"), m.takeIf { it > 0 }?.let { unit(it, "minute") })
        m > 0 -> listOfNotNull(unit(m, "minute"), s.takeIf { it > 0 }?.let { unit(it, "second") })
        else -> listOf(unit(s, "second"))
    }
    return parts.joinToString(" ") + " left"
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
 * ("Sleep · 12:34") and opens [SleepTimerDialog]. [songMsLeft] is what's left of the song, for "End of track".
 */
@Composable
internal fun SleepTimerChip(sleep: SleepTimer?, songMsLeft: Long, onSelect: (SleepOption?) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var dialogOpen by remember { mutableStateOf(false) }
    // A timer that fires or is switched off elsewhere closes the dialog, and the next timer doesn't reopen it.
    LaunchedEffect(sleep == null) { if (sleep == null) dialogOpen = false }

    Box {
        when {
            sleep == null -> ExtraChip(Icons.Rounded.Bedtime, "Sleep timer") { menuOpen = true }
            sleep.option == SleepOption.EndOfTrack -> ExtraChip(Icons.Rounded.Bedtime, "Sleep · End of track", active = true) { dialogOpen = true }
            else -> ExtraChip(Icons.Rounded.Bedtime, "Sleep · ${countdownText(rememberMsLeft(sleep.endsAt))}", active = true) { dialogOpen = true }
        }
        DropdownMenu(menuOpen, { menuOpen = false }, shape = RoundedCornerShape(16.dp), containerColor = colors.card) {
            SleepOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, fontSize = 14.sp, color = colors.onSurface) },
                    onClick = { onSelect(option); menuOpen = false }
                )
            }
        }
    }
    if (dialogOpen && sleep != null) {
        SleepTimerDialog(sleep, songMsLeft, onTurnOff = { onSelect(null); dialogOpen = false }, onDismiss = { dialogOpen = false })
    }
}

/**
 * M10: how long until the sleep timer pauses the music. Built like the app's other dialogs (`DeleteFileDialog`): a
 * tinted icon circle, an extra-bold title and muted text, with the time left as large sage digits, since type does
 * the heavy lifting here (GUIDELINE §1). Nunito's digits all share one width, so the countdown doesn't jiggle.
 */
@Composable
private fun SleepTimerDialog(sleep: SleepTimer, songMsLeft: Long, onTurnOff: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val endOfTrack = sleep.option == SleepOption.EndOfTrack
    val msLeft = if (endOfTrack) songMsLeft else rememberMsLeft(sleep.endsAt)
    val caption = when {
        endOfTrack -> "Pauses when this song ends"
        msLeft <= 60_000 -> "Fading out…"
        // DateFormat follows the phone's 12- or 24-hour setting.
        else -> "Pauses at ${DateFormat.getTimeFormat(context).format(Date(System.currentTimeMillis() + msLeft))} · fades out over the last minute"
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
        title = { Text("Sleep timer", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    countdownText(msLeft),
                    fontSize = 48.sp, lineHeight = 52.sp, fontWeight = FontWeight.ExtraBold, color = colors.primary,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = spokenCountdown(msLeft) },
                )
                Text(
                    caption, fontSize = 14.sp, color = colors.mutedForeground, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onTurnOff) { Text("Turn off", color = colors.mutedForeground) } },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F4F0, heightDp = 480)
@Composable
private fun SleepTimerDialogPreview() = HarmoniaTheme(darkTheme = false) {
    SleepTimerDialog(SleepTimer(SleepOption.Min30, SystemClock.elapsedRealtime() + 754_000), songMsLeft = 0, onTurnOff = {}, onDismiss = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2022, heightDp = 480)
@Composable
private fun SleepTimerDialogDarkPreview() = HarmoniaTheme(darkTheme = true) {
    SleepTimerDialog(SleepTimer(SleepOption.EndOfTrack, 0), songMsLeft = 133_000, onTurnOff = {}, onDismiss = {})
}
