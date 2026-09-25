package com.rfaizm.harmoniamusic.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfaizm.harmoniamusic.data.Settings
import com.rfaizm.harmoniamusic.data.Settings.Key
import com.rfaizm.harmoniamusic.ui.theme.border
import com.rfaizm.harmoniamusic.ui.theme.card
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground

@Composable
fun SettingsScreen(onRescan: () -> Unit) {
    var confirmOnlineLyrics by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 14.dp))

        SettingsSection("Library") {
            SettingsRow(Icons.Rounded.Schedule, "Rescan library", "Find new or removed songs", onClick = onRescan) { Chevron() }
            Divider()
            SettingsRow(Icons.Rounded.Tune, "Filters", "Hide clips shorter than 30 seconds", onClick = {}) { Chevron() }
        }

        // Crossfade and gapless switches are gone: Media3 is always gapless, and crossfade was dropped (tasks/plan.md).
        SettingsSection("Playback") {
            ToggleRow(Icons.Rounded.Shuffle, "Smart shuffle", "Spread out songs by the same artist", Key.SmartShuffle)
            Divider()
            SettingsRow(Icons.Rounded.Bedtime, "Sleep timer", "Off · fades out over the last minute", onClick = {}) { Chevron() }
            Divider()
            // The one switch that lets the app reach the network, so turning it on asks first.
            SettingsRow(
                Icons.Rounded.Language,
                "Look up lyrics online",
                "Off · otherwise only lyrics saved inside a file are shown",
                onClick = { if (Settings[Key.OnlineLyrics]) Settings[Key.OnlineLyrics] = false else confirmOnlineLyrics = true },
            ) {
                HarmoniaSwitch(Settings[Key.OnlineLyrics]) { on ->
                    if (on) confirmOnlineLyrics = true else Settings[Key.OnlineLyrics] = false
                }
            }
        }

        SettingsSection("Audio focus") {
            ToggleRow(Icons.Rounded.Notifications, "Lower volume for notifications", "Duck music instead of pausing", Key.Ducking)
            Divider()
            ToggleRow(Icons.Rounded.Headphones, "Pause when headphones unplug", "Resume on speaker at 30% volume", Key.PauseOnUnplug)
        }

        SettingsSection("Display & privacy") {
            ToggleRow(Icons.Rounded.Speed, "Light performance mode", "Turn off blur and heavy effects", Key.LiteMode)
            Divider()
            ToggleRow(Icons.Rounded.BlurOn, "Lock screen privacy", "Blur album art on the lock screen", Key.LockPrivacy)
        }

        SettingsSection("About") {
            SettingsRow(Icons.Rounded.Star, "Rate Harmonia", "Enjoying the quiet? Let us know", onClick = {}) { Chevron() }
            Divider()
            SettingsRow(Icons.Rounded.Info, "About", "Version 1.0 · offline by default")
        }
    }

    if (confirmOnlineLyrics) {
        AlertDialog(
            onDismissRequest = { confirmOnlineLyrics = false },
            containerColor = colors.card,
            title = { Text("Look up lyrics online?", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
            text = {
                Text(
                    "When you tap “Find lyrics online” on a song, Harmonia sends that song's title, artist, album and " +
                        "length to lrclib.net and shows what comes back. Nothing else leaves your phone, and nothing is sent " +
                        "until you tap it.",
                    fontSize = 14.sp, color = colors.mutedForeground, lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton({ Settings[Key.OnlineLyrics] = true; confirmOnlineLyrics = false }) { Text("Turn on") }
            },
            dismissButton = { TextButton({ confirmOnlineLyrics = false }) { Text("Cancel", color = colors.mutedForeground) } },
        )
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, sub: String, key: Key) =
    SettingsRow(icon, label, sub, onClick = { Settings[key] = !Settings[key] }) { HarmoniaSwitch(Settings[key]) { Settings[key] = it } }

@Composable
private fun Divider() = HorizontalDivider(color = colors.border)

@Composable
private fun Chevron() = Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.mutedForeground, modifier = Modifier.size(16.dp))
