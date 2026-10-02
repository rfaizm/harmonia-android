package com.rfaizm.harmoniamusic.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.rfaizm.harmoniamusic.PlaybackService
import com.rfaizm.harmoniamusic.LYRICS
import com.rfaizm.harmoniamusic.SLEEP_COMMAND
import com.rfaizm.harmoniamusic.SLEEP_MINUTES
import com.rfaizm.harmoniamusic.data.Library
import com.rfaizm.harmoniamusic.data.Settings
import com.rfaizm.harmoniamusic.data.Settings.Key
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.addSongs
import com.rfaizm.harmoniamusic.data.removeSong
import com.rfaizm.harmoniamusic.toMediaItem
import com.rfaizm.harmoniamusic.ui.theme.HarmoniaTheme
import com.rfaizm.harmoniamusic.ui.theme.border
import com.rfaizm.harmoniamusic.ui.theme.card
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val AUDIO_PERMISSION =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

enum class Tab(val label: String, val icon: ImageVector) {
    Songs("Songs", Icons.Rounded.MusicNote),
    Playlists("Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
    Artists("Artists", Icons.Rounded.Groups),
    Settings("Settings", Icons.Rounded.Settings),
}

/**
 * Root state holder (GUIDELINE §16). Songs and playlists live in [Library]; everything else is UI state here.
 * Playback lives in [PlaybackService]; [activeId] and [isPlaying] mirror it through a MediaController.
 * Likes, play counts and playlists are saved by [Library], the switches by [Settings], the queue by the service.
 */
@Composable
fun HarmoniaApp() {
    val darkMode = Settings[Key.DarkMode]
    val songs = Library.songs
    val playlists = Library.playlists
    var tab by rememberSaveable { mutableStateOf(Tab.Songs) }
    // Saved so rotation doesn't flash the mini player away while the controller reconnects.
    var activeId by rememberSaveable { mutableStateOf<Int?>(null) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var fullPlayer by rememberSaveable { mutableStateOf(false) }
    var shuffle by rememberSaveable { mutableStateOf(false) }
    var repeatMode by rememberSaveable { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var volume by remember { mutableFloatStateOf(0f) }
    var muted by remember { mutableStateOf(false) }
    // Mirrors the service's timer, so the chip goes back to "Off" by itself once the timer fires.
    var sleep by remember { mutableStateOf<SleepOption?>(null) }
    // Read from the song's tags by the service; track metadata never reaches a controller on its own.
    var lyrics by remember { mutableStateOf<String?>(null) }
    // The artist the player read from the file, which MediaStore sometimes misses (SPEC.md S2).
    var playerArtist by remember { mutableStateOf<String?>(null) }
    val active = songs.firstOrNull { it.id == activeId }

    // Connected while the app is visible; the service keeps playing after it's released.
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    // Media3 controllers drop device-volume commands during local playback, so volume is set through
    // AudioManager; the new level comes back through sync(), same as a hardware key press.
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    LifecycleStartEffect(Unit) {
        val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
            .setListener(object : MediaController.Listener {
                override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
                    sleep = sleepOptionOf(extras.getInt(SLEEP_MINUTES))
                    lyrics = extras.getString(LYRICS)
                }
            })
            .buildAsync()
        future.addListener({
            // Cancelled or already released if the app was stopped before the connection finished.
            val c = runCatching { future.get() }.getOrNull()?.takeIf { it.isConnected } ?: return@addListener
            fun sync() {
                activeId = c.currentMediaItem?.mediaId?.toIntOrNull()
                isPlaying = !Util.shouldShowPlayButton(c)
                positionMs = c.currentPosition
                shuffle = c.shuffleModeEnabled
                repeatMode = c.repeatMode
                volume = c.deviceVolume / c.deviceInfo.maxVolume.coerceAtLeast(1).toFloat()
                muted = c.isDeviceMuted
                playerArtist = c.mediaMetadata.artist?.toString()
            }
            c.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) = sync()
            })
            sync()
            sleep = sleepOptionOf(c.sessionExtras.getInt(SLEEP_MINUTES)) // a timer may already be running
            lyrics = c.sessionExtras.getString(LYRICS)
            controller = c
        }, ContextCompat.getMainExecutor(context))
        onStopOrDispose {
            controller = null
            MediaController.releaseFuture(future)
        }
    }

    // Seeks, track changes and pauses arrive through sync(); only the steady tick has to be polled.
    LaunchedEffect(controller, isPlaying) {
        val c = controller ?: return@LaunchedEffect
        while (isPlaying) {
            positionMs = c.currentPosition
            delay(500)
        }
    }

    // Next/prev walk the list the song was tapped from: the sorted Songs tab, a playlist, or Liked.
    fun playFrom(s: Song, list: List<Song>) {
        val c = controller ?: return
        c.setMediaItems(list.map { it.toMediaItem() }, list.indexOfFirst { it.id == s.id }, 0)
        c.prepare()
        c.play()
    }
    fun like(s: Song) {
        val i = songs.indexOfFirst { it.id == s.id }
        songs[i] = songs[i].copy(liked = !songs[i].liked)
        Library.save(context)
    }
    // T16: the file is gone, so take the song out of the queue (Media3 moves on by itself) and the library.
    val deleteSong = rememberSongDeleter { song ->
        controller?.let { c ->
            (c.mediaItemCount - 1 downTo 0)
                .filter { c.getMediaItemAt(it).mediaId == song.id.toString() }
                .forEach(c::removeMediaItem)
        }
        Library.remove(context, song.id)
    }
    fun addTo(name: String, ids: Collection<Int>) {
        playlists.addSongs(name, ids, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        Library.save(context)
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

    // First scan once permission is there; Library outlives the activity, so rotation doesn't rescan.
    // Saved likes, play counts and playlists are read first, so the scan can merge them in.
    LaunchedEffect(granted) { if (granted && !Library.loaded) Library.ensureLoaded(context) }
    val scope = rememberCoroutineScope()
    var rescanning by remember { mutableStateOf(false) }
    fun rescan() {
        if (!granted || rescanning) return
        scope.launch {
            rescanning = true
            try { Library.scan(context) } finally { rescanning = false }
        }
    }

    HarmoniaTheme(darkTheme = darkMode) {
        Box(Modifier.fillMaxSize().background(colors.background)) {
            Column(Modifier.fillMaxSize()) {
                TopBar(songs.size, darkMode) { Settings[Key.DarkMode] = !darkMode }
                AnimatedContent(
                    tab,
                    transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(160)) },
                    modifier = Modifier.weight(1f),
                    label = "tab"
                ) { t ->
                    when {
                        t == Tab.Settings -> SettingsScreen(onRescan = ::rescan)
                        !granted -> PermissionEmptyState(blocked, ::requestPermission)
                        !Library.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                        songs.isEmpty() -> NoMusicEmptyState(rescanning, ::rescan)
                        t == Tab.Songs -> SongsScreen(songs, playlists, activeId, isPlaying, ::playFrom, ::like, ::addTo, deleteSong)
                        t == Tab.Playlists -> PlaylistsScreen(
                            songs, playlists, activeId, isPlaying, ::playFrom, ::like, ::addTo,
                            onCreate = { addTo(it, emptyList()) },
                            onRemove = { playlistId, songId -> playlists.removeSong(playlistId, songId); Library.save(context) },
                            onDelete = { playlists.remove(it); Library.save(context) },
                            onDeleteSong = deleteSong,
                        )
                        else -> ExploreScreen(songs, playlists, activeId, isPlaying, ::playFrom, ::like, ::addTo, deleteSong)
                    }
                }
                AnimatedVisibility(
                    active != null,
                    enter = slideInVertically(spring(dampingRatio = 0.81f, stiffness = 340f)) { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    active?.let {
                        MiniPlayer(it, isPlaying, progressOf(positionMs, it.duration), onToggle = { Util.handlePlayPauseButtonAction(controller) }, onNext = { controller?.seekToNext() }, onOpen = { fullPlayer = true })
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
                        song = it, isPlaying = isPlaying, progress = progressOf(positionMs, it.duration),
                        shuffle = shuffle, repeatMode = repeatMode, volume = volume, muted = muted, sleep = sleep, lyrics = lyrics, playerArtist = playerArtist,
                        position = { controller?.currentPosition ?: 0L },
                        onClose = { fullPlayer = false },
                        onToggle = { Util.handlePlayPauseButtonAction(controller) },
                        onNext = { controller?.seekToNext() },
                        onPrev = { controller?.seekToPrevious() },
                        onSeek = { f -> controller?.seekTo((f * it.duration * 1000).toLong()) },
                        // Explicit `c.`: inside run {} the local repeatMode/shuffle vars would shadow the controller's.
                        onShuffle = { controller?.let { c -> c.shuffleModeEnabled = !c.shuffleModeEnabled } },
                        onRepeat = { controller?.let { c -> c.repeatMode = nextRepeatMode(c.repeatMode) } },
                        onVolume = { f ->
                            if (audio.isStreamMute(STREAM_MUSIC)) audio.adjustStreamVolume(STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                            audio.setStreamVolume(STREAM_MUSIC, (f * audio.getStreamMaxVolume(STREAM_MUSIC)).roundToInt(), 0)
                        },
                        onMute = { audio.adjustStreamVolume(STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0) },
                        onSleep = { option ->
                            // No local echo: the label changes when the service confirms, and only it knows when a timer ends.
                            controller?.sendCustomCommand(
                                SessionCommand(SLEEP_COMMAND, Bundle.EMPTY),
                                bundleOf(SLEEP_MINUTES to (option?.minutes ?: 0)),
                            )
                        },
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

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HarmoniaAppPreview() = HarmoniaApp()
