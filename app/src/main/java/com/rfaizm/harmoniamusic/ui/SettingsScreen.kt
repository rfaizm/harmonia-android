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
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfaizm.harmoniamusic.data.Settings
import com.rfaizm.harmoniamusic.data.Settings.Key
import com.rfaizm.harmoniamusic.ui.theme.border
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground

@Composable
fun SettingsScreen(onRescan: () -> Unit) {
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
            SettingsRow(Icons.Rounded.Info, "About", "Version 1.0 · works fully offline")
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, sub: String, key: Key) =
    SettingsRow(icon, label, sub, onClick = { Settings[key] = !Settings[key] }) { HarmoniaSwitch(Settings[key]) { Settings[key] = it } }

@Composable
private fun Divider() = HorizontalDivider(color = colors.border)

@Composable
private fun Chevron() = Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.mutedForeground, modifier = Modifier.size(16.dp))
