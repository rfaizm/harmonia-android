package com.rfaizm.harmoniamusic.ui

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.rfaizm.harmoniamusic.data.SEED_PLAYLISTS
import com.rfaizm.harmoniamusic.data.SEED_SONGS
import com.rfaizm.harmoniamusic.data.Playlist
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.addSongs
import com.rfaizm.harmoniamusic.data.removeSong
import com.rfaizm.harmoniamusic.ui.theme.HarmoniaTheme
import com.rfaizm.harmoniamusic.ui.theme.border
import com.rfaizm.harmoniamusic.ui.theme.card
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Tab(val label: String, val icon: ImageVector) {
    Songs("Songs", Icons.Rounded.MusicNote),
    Playlists("Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
    Artists("Artists", Icons.Rounded.Groups),
    Settings("Settings", Icons.Rounded.Settings),
}

/**
 * UI-only shell: all state is in-memory seed data (GUIDELINE §16).
 * ponytail: no playback, scanning or persistence yet — wire a MediaStore repo + Media3 player here next.
 */
@Composable
fun HarmoniaApp() {
    var darkMode by rememberSaveable { mutableStateOf(true) }
    val songs = remember { mutableStateListOf<Song>().apply { addAll(SEED_SONGS) } }
    val playlists = remember { mutableStateListOf<Playlist>().apply { addAll(SEED_PLAYLISTS) } }
    var tab by rememberSaveable { mutableStateOf(Tab.Songs) }
    var activeId by rememberSaveable { mutableStateOf<Int?>(null) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var fullPlayer by rememberSaveable { mutableStateOf(false) }
    var shuffle by rememberSaveable { mutableStateOf(false) }
    var repeatMode by rememberSaveable { mutableIntStateOf(0) }
    // Ids of the list the current song was started from; next/prev walk this, not the whole library.
    // ponytail: plain remember (a big queue would bloat the saved-state Bundle); Media3 owns the queue from T6.
    var queue by remember { mutableStateOf(emptyList<Int>()) }
    val active = songs.firstOrNull { it.id == activeId }

    fun play(s: Song) {
        val i = songs.indexOfFirst { it.id == s.id }
        songs[i] = s.copy(playCount = s.playCount + 1)
        activeId = s.id
        isPlaying = true
    }
    fun playFrom(s: Song, list: List<Song>) {
        queue = list.map { it.id }
        play(s)
    }
    fun like(s: Song) {
        val i = songs.indexOfFirst { it.id == s.id }
        songs[i] = songs[i].copy(liked = !songs[i].liked)
    }
    fun addTo(name: String, ids: Collection<Int>) =
        playlists.addSongs(name, ids, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    fun step(delta: Int) {
        val q = queue.mapNotNull { id -> songs.firstOrNull { it.id == id } }.ifEmpty { songs }
        if (q.isEmpty()) return
        val i = q.indexOfFirst { it.id == activeId }
        play(q[if (shuffle) q.indices.random() else (i + delta).mod(q.size)])
    }

    // Status bar icons follow the app theme; the full player is always dark.
    val activity = LocalContext.current as? ComponentActivity
    val darkBars = darkMode || fullPlayer
    LaunchedEffect(darkBars) {
        val style = if (darkBars) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    HarmoniaTheme(darkTheme = darkMode) {
        Box(Modifier.fillMaxSize().background(colors.background)) {
            Column(Modifier.fillMaxSize()) {
                TopBar(songs.size, darkMode) { darkMode = !darkMode }
                AnimatedContent(
                    tab,
                    transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(160)) },
                    modifier = Modifier.weight(1f),
                    label = "tab"
                ) { t ->
                    when (t) {
                        Tab.Songs -> SongsScreen(songs, playlists, activeId, isPlaying, ::playFrom, ::like, ::addTo)
                        Tab.Playlists -> PlaylistsScreen(
                            songs, playlists, activeId, isPlaying, ::playFrom, ::like, ::addTo,
                            onCreate = { addTo(it, emptyList()) },
                            onRemove = { playlistId, songId -> playlists.removeSong(playlistId, songId) },
                            onDelete = { playlists.remove(it) },
                        )
                        Tab.Artists -> ExploreScreen(songs, playlists, activeId, isPlaying, ::playFrom, ::like, ::addTo)
                        Tab.Settings -> SettingsScreen()
                    }
                }
                AnimatedVisibility(
                    active != null,
                    enter = slideInVertically(spring(dampingRatio = 0.81f, stiffness = 340f)) { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    active?.let {
                        MiniPlayer(it, isPlaying, 0.32f, onToggle = { isPlaying = !isPlaying }, onNext = { step(1) }, onOpen = { fullPlayer = true })
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
                        song = it, isPlaying = isPlaying, shuffle = shuffle, repeatMode = repeatMode,
                        onClose = { fullPlayer = false },
                        onToggle = { isPlaying = !isPlaying },
                        onNext = { step(1) },
                        onPrev = { step(-1) },
                        onShuffle = { shuffle = !shuffle },
                        onRepeat = { repeatMode = (repeatMode + 1) % 3 },
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
