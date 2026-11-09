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
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Alignment
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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.rfaizm.harmoniamusic.BuildConfig
import com.rfaizm.harmoniamusic.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfaizm.harmoniamusic.data.SettingsRepository.Key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rfaizm.harmoniamusic.ui.theme.border
import com.rfaizm.harmoniamusic.ui.theme.card
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground

@Composable
fun SettingsScreen(
    settings: SettingsViewModel,
    rescanning: Boolean,
    onRescan: () -> Unit,
    sleep: SleepTimer?,
    hasSong: Boolean,
    onSleep: (SleepOption?) -> Unit,
) {
    val flags by settings.flags.collectAsStateWithLifecycle()
    val lyricsFolder by settings.lyricsFolder.collectAsStateWithLifecycle()
    var confirmOnlineLyrics by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // PRD phase 9 "Lirik Lokal": scoped storage hides .lrc files, so the user points at their music folder once.
    val pickLyricsFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
        if (folder != null) {
            settings.grantLyricsFolder(folder)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.settings_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 14.dp))

        SettingsSection(stringResource(R.string.settings_library)) {
            // L2: a spinner while it runs; HarmoniaApp shows what it found when it ends.
            SettingsRow(
                Icons.Rounded.Schedule, stringResource(R.string.settings_rescan),
                stringResource(if (rescanning) R.string.scan_in_progress else R.string.settings_rescan_sub), onClick = onRescan,
            ) {
                if (rescanning) CircularProgressIndicator(Modifier.size(16.dp), color = colors.primary, strokeWidth = 2.dp) else Chevron()
            }
            Divider()
            // Flipping it changes what the scan keeps, so it rescans and shows L2's message (L4).
            val hideShort = flags.getValue(Key.HideShortClips)
            val setHideShort = { on: Boolean -> if (!rescanning) { settings[Key.HideShortClips] = on; onRescan() } }
            SettingsRow(Icons.Rounded.Tune, stringResource(R.string.settings_filters), stringResource(R.string.settings_filters_sub), onClick = { setHideShort(!hideShort) }) {
                HarmoniaSwitch(hideShort, setHideShort)
            }
        }

        // Crossfade and gapless switches are gone: Media3 is always gapless, and crossfade was dropped (tasks/plan.md).
        SettingsSection(stringResource(R.string.settings_playback)) {
            ToggleRow(settings, flags, Icons.Rounded.Shuffle, stringResource(R.string.settings_smart_shuffle), stringResource(R.string.settings_smart_shuffle_sub), Key.SmartShuffle)
            Divider()
            SleepTimerRow(sleep, hasSong, onSleep)
            Divider()
            SettingsRow(
                Icons.Rounded.Lyrics,
                stringResource(R.string.settings_lyrics_folder),
                lyricsFolder?.let { stringResource(R.string.settings_lyrics_folder_set, folderLabel(it) ?: stringResource(R.string.settings_lyrics_folder_fallback)) } ?: stringResource(R.string.settings_lyrics_folder_unset),
                onClick = { pickLyricsFolder.launch(null) },
            ) { Chevron() }
            Divider()
            // The one switch that lets the app reach the network, so turning it on asks first.
            SettingsRow(
                Icons.Rounded.Language,
                stringResource(R.string.settings_online_lyrics),
                stringResource(R.string.settings_online_lyrics_sub),
                onClick = { if (flags.getValue(Key.OnlineLyrics)) settings[Key.OnlineLyrics] = false else confirmOnlineLyrics = true },
            ) {
                HarmoniaSwitch(flags.getValue(Key.OnlineLyrics)) { on ->
                    if (on) confirmOnlineLyrics = true else settings[Key.OnlineLyrics] = false
                }
            }
        }

        SettingsSection(stringResource(R.string.settings_audio_focus)) {
            ToggleRow(settings, flags, Icons.Rounded.Notifications, stringResource(R.string.settings_ducking), stringResource(R.string.settings_ducking_sub), Key.Ducking)
            Divider()
            ToggleRow(settings, flags, Icons.Rounded.Headphones, stringResource(R.string.settings_unplug), stringResource(R.string.settings_unplug_sub), Key.PauseOnUnplug)
        }

        SettingsSection(stringResource(R.string.settings_display)) {
            LanguageRow(settings)
            Divider()
            ToggleRow(settings, flags, Icons.Rounded.Speed, stringResource(R.string.settings_lite), stringResource(R.string.settings_lite_sub), Key.LiteMode)
            Divider()
            ToggleRow(settings, flags, Icons.Rounded.BlurOn, stringResource(R.string.settings_lock_privacy), stringResource(R.string.settings_lock_privacy_sub), Key.LockPrivacy)
        }

        SettingsSection(stringResource(R.string.settings_about)) {
            SettingsRow(Icons.Rounded.Star, stringResource(R.string.settings_rate), stringResource(R.string.settings_rate_sub), onClick = { openRatePage(context) }) { Chevron() }
            Divider()
            SettingsRow(Icons.Rounded.Info, stringResource(R.string.settings_about), stringResource(R.string.settings_version, BuildConfig.VERSION_NAME))
        }
    }

    if (confirmOnlineLyrics) {
        AlertDialog(
            onDismissRequest = { confirmOnlineLyrics = false },
            containerColor = colors.card,
            title = { Text(stringResource(R.string.online_lyrics_title), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
            text = {
                Text(
                    stringResource(R.string.online_lyrics_body),
                    fontSize = 14.sp, color = colors.mutedForeground, lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton({ settings[Key.OnlineLyrics] = true; confirmOnlineLyrics = false }) { Text(stringResource(R.string.turn_on)) }
            },
            dismissButton = { TextButton({ confirmOnlineLyrics = false }) { Text(stringResource(R.string.cancel), color = colors.mutedForeground) } },
        )
    }
}

/**
 * L3: the same timer as the player's sleep chip, so one set here shows there and the other way round. The second
 * line follows it live. With nothing loaded there's nothing to pause, so a tap says so instead of opening the menu.
 */
@Composable
private fun SleepTimerRow(sleep: SleepTimer?, hasSong: Boolean, onSelect: (SleepOption?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val resources = LocalResources.current
    val status = when {
        sleep == null -> stringResource(R.string.settings_sleep_timer_sub)
        sleep.option == SleepOption.EndOfTrack -> stringResource(R.string.sleep_pauses_at_end)
        else -> stringResource(R.string.sleep_pauses_in, countdownText(rememberMsLeft(sleep.endsAt)))
    }
    Box {
        SettingsRow(Icons.Rounded.Bedtime, stringResource(R.string.sleep_timer), status, onClick = {
            if (hasSong || sleep != null) open = true
            else Toast.makeText(context, resources.getString(R.string.sleep_needs_song), Toast.LENGTH_SHORT).show()
        }) { Chevron() }
        DropdownMenu(open, { open = false }, Modifier.align(Alignment.CenterEnd), shape = RoundedCornerShape(16.dp), containerColor = colors.card) {
            sleepMenuItems(running = sleep != null).forEach { option ->
                DropdownMenuItem(
                    text = {
                        if (option == null) Text(stringResource(R.string.turn_off), fontSize = 14.sp, color = colors.mutedForeground)
                        else Text(option.label(), fontSize = 14.sp, color = colors.onSurface)
                    },
                    trailingIcon = { if (option != null && option == sleep?.option) Icon(Icons.Rounded.Check, null, tint = colors.primary) },
                    onClick = { open = false; onSelect(option) },
                )
            }
        }
    }
}

/** L1: the app's own language, picked from a menu; AppCompat redraws the app in it. */
@Composable
private fun LanguageRow(settings: SettingsViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        SettingsRow(Icons.Rounded.Translate, stringResource(R.string.settings_language), stringResource(settings.language.label), onClick = { open = true }) { Chevron() }
        DropdownMenu(open, { open = false }, Modifier.align(Alignment.CenterEnd), shape = RoundedCornerShape(16.dp), containerColor = colors.card) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(stringResource(language.label), fontSize = 14.sp, color = colors.onSurface) },
                    trailingIcon = { if (language == settings.language) Icon(Icons.Rounded.Check, null, tint = colors.primary) },
                    onClick = { open = false; settings.setLanguage(language) },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(settings: SettingsViewModel, flags: Map<Key, Boolean>, icon: ImageVector, label: String, sub: String, key: Key) =
    SettingsRow(icon, label, sub, onClick = { settings[key] = !flags.getValue(key) }) { HarmoniaSwitch(flags.getValue(key)) { settings[key] = it } }

/** The tail of a document-tree uri is the readable part, e.g. "primary:Music". */
/** Where "Rate Harmonia" goes, in order (L5). F-Droid and the Play Store both handle market:// links. */
internal val rateLinks = listOf("market://details?id=com.rfaizm.harmoniamusic", "https://github.com/rfaizm/harmonia-android")

/** Opens the first link something on the phone can handle; with nothing, says so. */
private fun openRatePage(context: Context) {
    val opened = rateLinks.any { link ->
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }
    if (!opened) Toast.makeText(context, context.getString(R.string.rate_no_app), Toast.LENGTH_SHORT).show()
}

private fun folderLabel(uri: String) = Uri.decode(uri.substringAfterLast("%3A").substringAfterLast(':')).ifBlank { null }

@Composable
private fun Divider() = HorizontalDivider(color = colors.border)

@Composable
private fun Chevron() = Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.mutedForeground, modifier = Modifier.size(16.dp))
