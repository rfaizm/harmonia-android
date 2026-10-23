package com.rfaizm.harmoniamusic.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import com.rfaizm.harmoniamusic.data.LyricsRepository
import com.rfaizm.harmoniamusic.data.MusicRepository
import com.rfaizm.harmoniamusic.data.SettingsRepository
import com.rfaizm.harmoniamusic.data.SettingsRepository.Key
import com.rfaizm.harmoniamusic.data.Song
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rfaizm.harmoniamusic.ui.theme.HarmoniaTheme
import com.rfaizm.harmoniamusic.ui.theme.border
import com.rfaizm.harmoniamusic.ui.theme.card
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground
import kotlinx.coroutines.launch

private val AUDIO_PERMISSION =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

enum class Tab(val label: String, val icon: ImageVector) {
    Songs("Songs", Icons.Rounded.MusicNote),
    Playlists("Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
    Artists("Artists", Icons.Rounded.Groups),
    Settings("Settings", Icons.Rounded.Settings),
}

/**
 * Root of the UI (GUIDELINE §16). It keeps only what is about the screen itself: the tab, the full-player flag and
 * the permission flow. The library and the switches come from the repositories, and playback, mirrored from the
 * service, from [PlayerViewModel].
 */
@Composable
fun HarmoniaApp(music: MusicRepository, settings: SettingsRepository, lyricsRepository: LyricsRepository) {
    val flags by settings.flags.collectAsStateWithLifecycle()
    val darkMode = flags.getValue(Key.DarkMode)
    val songs by music.songs.collectAsStateWithLifecycle()
    val playlists by music.playlists.collectAsStateWithLifecycle()
    val loaded by music.loaded.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.Songs) }
    var fullPlayer by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // Connected while the app is visible; the service keeps playing after the ViewModel lets go.
    val player: PlayerViewModel = hiltViewModel()
    val playback by player.state.collectAsStateWithLifecycle()
    LifecycleStartEffect(player) {
        player.connect()
        onStopOrDispose { player.disconnect() }
    }
    val active = songs.firstOrNull { it.id == playback.activeId }

    fun like(s: Song) {
        music.toggleLike(s.id)
    }
    // T16: the file is gone, so take the song out of the queue (Media3 moves on by itself) and the library.
    val deleteSong = rememberSongDeleter(music) { song ->
        player.dropFromQueue(song.id)
        music.remove(song.id)
    }
    fun addTo(name: String, ids: Collection<Int>) {
        music.addToPlaylist(name, ids)
    }

    // Status bar icons follow the app theme; the full player is always dark.
    val activity = LocalContext.current as? ComponentActivity
    val darkBars = darkMode || fullPlayer
    LaunchedEffect(darkBars) {
        val style = if (darkBars) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    // PRD phase 1: our explainer shows first; the system prompt only appears once "Grant access" is tapped.
    fun hasPermission() = ContextCompat.checkSelfPermission(context, AUDIO_PERMISSION) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(hasPermission()) }
    // "Don't ask again": the system won't prompt any more, so the button opens app settings instead.
    // ponytail: dismissing the prompt without choosing also reads as blocked on API 30+; costs one detour via settings.
    var blocked by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        blocked = !ok && activity?.shouldShowRequestPermissionRationale(AUDIO_PERMISSION) == false
    }
    LifecycleResumeEffect(Unit) {
        granted = hasPermission() // picks up a grant made in system settings
        onPauseOrDispose {}
    }
    fun requestPermission() {
        if (blocked) context.startActivity(
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        )
        else permissionLauncher.launch(AUDIO_PERMISSION)
    }

    // First scan once permission is there; the repository outlives the activity, so rotation doesn't rescan.
    // Saved likes, play counts and playlists are read first, so the scan can merge them in.
    LaunchedEffect(granted) { if (granted && !loaded) music.ensureLoaded() }
    val scope = rememberCoroutineScope()
    var rescanning by remember { mutableStateOf(false) }
    fun rescan() {
        if (!granted || rescanning) return
        scope.launch {
            rescanning = true
            try { music.scan() } finally { rescanning = false }
        }
    }

    HarmoniaTheme(darkTheme = darkMode) {
        Box(Modifier.fillMaxSize().background(colors.background)) {
            Column(Modifier.fillMaxSize()) {
                TopBar(songs.size, darkMode) { settings[Key.DarkMode] = !darkMode }
                AnimatedContent(
                    tab,
                    transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(160)) },
                    modifier = Modifier.weight(1f),
                    label = "tab"
                ) { t ->
                    when {
                        t == Tab.Settings -> SettingsScreen(settings, onRescan = ::rescan)
                        !granted -> PermissionEmptyState(blocked, ::requestPermission)
                        !loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                        songs.isEmpty() -> NoMusicEmptyState(rescanning, ::rescan)
                        t == Tab.Songs -> SongsScreen(hiltViewModel(), playback.activeId, playback.isPlaying, player::playFrom, deleteSong)
                        t == Tab.Playlists -> PlaylistsScreen(hiltViewModel(), playback.activeId, playback.isPlaying, player::playFrom, deleteSong)
                        else -> ExploreScreen(songs, playlists, playback.activeId, playback.isPlaying, player::playFrom, ::like, ::addTo, deleteSong)
                    }
                }
                AnimatedVisibility(
                    active != null,
                    enter = slideInVertically(spring(dampingRatio = 0.81f, stiffness = 340f)) { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    active?.let {
                        MiniPlayer(it, playback.isPlaying, progressOf(playback.positionMs, it.duration), onToggle = player::togglePlay, onNext = player::next, onOpen = { fullPlayer = true })
                    }
                }
                BottomNav(tab) { tab = it }
            }

            AnimatedVisibility(
                fullPlayer && active != null,
                enter = slideInVertically(spring(dampingRatio = 0.98f, stiffness = 300f)) { it },
                exit = slideOutVertically { it },
            ) {
                active?.let {
                    FullPlayer(
                        song = it, isPlaying = playback.isPlaying, progress = progressOf(playback.positionMs, it.duration),
                        shuffle = playback.shuffle, repeatMode = playback.repeatMode, volume = playback.volume, muted = playback.muted,
                        sleep = playback.sleep, lyrics = playback.lyrics, playerArtist = playback.playerArtist,
                        settings = settings, lyricsRepository = lyricsRepository,
                        position = player::position,
                        onClose = { fullPlayer = false },
                        onToggle = player::togglePlay,
                        onNext = player::next,
                        onPrev = player::previous,
                        onSeek = { f -> player.seekTo(f, it.duration) },
                        onShuffle = player::toggleShuffle,
                        onRepeat = player::cycleRepeat,
                        onVolume = player::setVolume,
                        onMute = player::toggleMute,
                        onSleep = player::setSleep,
                        onSleepAdjust = player::adjustSleep,
                        onLike = { like(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(songCount: Int, darkMode: Boolean, onToggleTheme: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Harmonia", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp)
            Text("$songCount songs · local library", fontSize = 11.sp, color = colors.mutedForeground, modifier = Modifier.padding(top = 2.dp))
        }
        CircleIcon(
            if (darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
            if (darkMode) "Switch to light mode" else "Switch to dark mode",
            onToggleTheme,
            modifier = Modifier.border(1.dp, colors.border, CircleShape),
            size = 36.dp, iconSize = 16.dp, tint = colors.onBackground, background = colors.card,
        )
    }
}

@Composable
private fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.background(colors.card.copy(alpha = 0.95f)).navigationBarsPadding()) {
        HorizontalDivider(color = colors.border)
        Row {
            Tab.entries.forEach { t ->
                val active = t == current
                val scale by animateFloatAsState(if (active) 1f else 0.92f, label = "navScale")
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(t) }
                        .padding(top = 8.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        Modifier
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(CircleShape)
                            .background(if (active) colors.primary.copy(alpha = 0.15f) else colors.card.copy(alpha = 0f))
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(t.icon, null, tint = if (active) colors.primary else colors.mutedForeground, modifier = Modifier.size(22.dp))
                    }
                    Text(t.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp, color = if (active) colors.primary else colors.mutedForeground)
                }
            }
        }
    }
}
