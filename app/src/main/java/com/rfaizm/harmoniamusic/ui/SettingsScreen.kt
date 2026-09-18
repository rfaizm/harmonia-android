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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfaizm.harmoniamusic.ui.theme.border
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground

@Composable
fun SettingsScreen(onRescan: () -> Unit) {
    var crossfade by rememberSaveable { mutableStateOf(true) }
    var gapless by rememberSaveable { mutableStateOf(true) }
    var smartShuffle by rememberSaveable { mutableStateOf(true) }
    var ducking by rememberSaveable { mutableStateOf(true) }
    var pauseOnUnplug by rememberSaveable { mutableStateOf(true) }
    var lightMode by rememberSaveable { mutableStateOf(false) }
    var lockPrivacy by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 14.dp))

        SettingsSection("Library") {
            SettingsRow(Icons.Rounded.Schedule, "Rescan library", "Find new or removed songs", onClick = onRescan) { Chevron() }
            Divider()
            SettingsRow(Icons.Rounded.Tune, "Filters", "Hide clips shorter than 30 seconds", onClick = {}) { Chevron() }
        }

        SettingsSection("Playback") {
            ToggleRow(Icons.Rounded.SwapHoriz, "Crossfade", "Smooth 2s blend between tracks", crossfade) { crossfade = it }
            Divider()
            ToggleRow(Icons.Rounded.GraphicEq, "Gapless playback", "No silence between album tracks", gapless) { gapless = it }
            Divider()
            ToggleRow(Icons.Rounded.Shuffle, "Smart shuffle", "Spread out songs by the same artist", smartShuffle) { smartShuffle = it }
            Divider()
            SettingsRow(Icons.Rounded.Bedtime, "Sleep timer", "Off · fades out over the last minute", onClick = {}) { Chevron() }
        }

        SettingsSection("Audio focus") {
            ToggleRow(Icons.Rounded.Notifications, "Lower volume for notifications", "Duck music instead of pausing", ducking) { ducking = it }
            Divider()
            ToggleRow(Icons.Rounded.Headphones, "Pause when headphones unplug", "Resume on speaker at 30% volume", pauseOnUnplug) { pauseOnUnplug = it }
        }

        SettingsSection("Display & privacy") {
            ToggleRow(Icons.Rounded.Speed, "Light performance mode", "Turn off blur and heavy effects", lightMode) { lightMode = it }
            Divider()
            ToggleRow(Icons.Rounded.BlurOn, "Lock screen privacy", "Blur album art on the lock screen", lockPrivacy) { lockPrivacy = it }
        }

        SettingsSection("About") {
            SettingsRow(Icons.Rounded.Star, "Rate Harmonia", "Enjoying the quiet? Let us know", onClick = {}) { Chevron() }
            Divider()
            SettingsRow(Icons.Rounded.Info, "About", "Version 1.0 · works fully offline")
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) =
    SettingsRow(icon, label, sub, onClick = { onChange(!value) }) { HarmoniaSwitch(value, onChange) }

@Composable
private fun Divider() = HorizontalDivider(color = colors.border)

@Composable
private fun Chevron() = Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.mutedForeground, modifier = Modifier.size(16.dp))
