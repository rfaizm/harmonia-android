package com.rfaizm.harmoniamusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfaizm.harmoniamusic.data.LIKED_GRADIENT
import com.rfaizm.harmoniamusic.data.Playlist
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.formatDate
import com.rfaizm.harmoniamusic.data.formatDuration
import com.rfaizm.harmoniamusic.data.gradientFor
import com.rfaizm.harmoniamusic.ui.theme.border
import com.rfaizm.harmoniamusic.ui.theme.card
import com.rfaizm.harmoniamusic.ui.theme.destructive
import com.rfaizm.harmoniamusic.ui.theme.muted
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground

private const val LIKED_ID = -1

@Composable
fun PlaylistsScreen(
    songs: List<Song>,
    playlists: List<Playlist>,
    activeId: Int?,
    isPlaying: Boolean,
    onPlay: (song: Song, queue: List<Song>) -> Unit,
    onLike: (Song) -> Unit,
    onAddToPlaylist: (name: String, ids: Collection<Int>) -> Unit,
    onCreate: (String) -> Unit,
    onRemove: (playlistId: Int, songId: Int) -> Unit,
    onDelete: (Playlist) -> Unit,
    onDeleteSong: (Song) -> Unit,
) {
    var openId by rememberSaveable { mutableStateOf<Int?>(null) }
    BackHandler(openId != null) { openId = null }

    AnimatedContent(openId, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "playlists") { id ->
        if (id == null) {
            PlaylistList(songs, playlists, onOpen = { openId = it }, onPlay = onPlay, onCreate = onCreate, onDelete = onDelete)
        } else {
            val playlist = if (id == LIKED_ID) null else playlists.firstOrNull { it.id == id }
            // Liked Songs snapshots its ids on open, so un-liking keeps the song (faded) until reopened — PRD phase 8.
            // Real playlists stay live so "Remove from playlist" shows immediately.
            val likedSnapshot = remember(id) { songs.filter { it.liked }.map { it.id } }
            val ids = playlist?.songIds ?: likedSnapshot
            PlaylistDetail(
                name = playlist?.name ?: "Liked Songs",
                createdAt = playlist?.createdAt,
                gradient = playlist?.gradient ?: LIKED_GRADIENT,
                icon = if (playlist == null) Icons.Rounded.Favorite else Icons.AutoMirrored.Rounded.QueueMusic,
                songs = ids.mapNotNull { sid -> songs.firstOrNull { it.id == sid } },
                playlists = playlists,
                fadeUnliked = playlist == null,
                activeId = activeId,
                isPlaying = isPlaying,
                onBack = { openId = null },
                onPlay = onPlay,
                onLike = onLike,
                onAddToPlaylist = onAddToPlaylist,
                onDeleteSong = onDeleteSong,
                onRemove = playlist?.let { p -> { s: Song -> onRemove(p.id, s.id) } },
            )
        }
    }
}

@Composable
private fun PlaylistList(
    songs: List<Song>,
    playlists: List<Playlist>,
    onOpen: (Int) -> Unit,
    onPlay: (song: Song, queue: List<Song>) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (Playlist) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    fun save() { if (newName.isNotBlank()) onCreate(newName.trim()); newName = ""; creating = false }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Playlists", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Row(
                    Modifier.clip(CircleShape).background(colors.primary.copy(alpha = 0.12f)).clickable { creating = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, tint = colors.primary, modifier = Modifier.size(13.dp))
                    Text("New", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                }
            }
        }
        item {
            AnimatedVisibility(creating, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Row(
                    Modifier.padding(bottom = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.card)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        if (newName.isEmpty()) Text("Playlist name…", fontSize = 14.sp, color = colors.mutedForeground)
                        BasicTextField(
                            newName, { newName = it }, singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = colors.onBackground),
                            cursorBrush = SolidColor(colors.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { save() }),
                            modifier = Modifier.fillMaxWidth().onKeyEvent {
                                if (it.key == Key.Escape) { newName = ""; creating = false; true } else false
                            }
                        )
                    }
                    CircleIcon(Icons.Rounded.Check, "Save playlist", ::save, size = 26.dp, iconSize = 13.dp, tint = colors.onPrimary, background = colors.primary)
                    CircleIcon(Icons.Rounded.Close, "Cancel", { newName = ""; creating = false }, size = 26.dp, iconSize = 13.dp, tint = colors.onBackground, background = colors.muted)
                }
            }
        }
        item { LikedCard(songs.count { it.liked }) { onOpen(LIKED_ID) } }
        items(playlists, key = { it.id }) { p ->
            PlaylistCard(
                p, songs, onOpen = { onOpen(p.id) },
                onPlay = {
                    val q = p.songIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
                    q.firstOrNull()?.let { onPlay(it, q) }
                },
                onDelete = { onDelete(p) }, modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun LikedCard(count: Int, onClick: () -> Unit) {
    Row(
        Modifier.padding(bottom = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(gradientBrush(LIKED_GRADIENT))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Favorite, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Liked Songs", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Text("$count songs", fontSize = 14.sp, color = Color.White.copy(alpha = 0.65f))
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PlaylistCard(p: Playlist, songs: List<Song>, onOpen: () -> Unit, onPlay: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.padding(bottom = 12.dp).fillMaxWidth().clip(shape).background(colors.card).border(1.dp, colors.border, shape).clickable(onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth().height(80.dp).background(gradientBrush(p.gradient)).padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (p.songIds.isEmpty()) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
            } else {
                p.songIds.take(4).forEach { id ->
                    AlbumArt(gradientFor(id), 32.dp, Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp)), RoundedCornerShape(8.dp), songId = id)
                }
            }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                val total = p.songIds.sumOf { id -> songs.firstOrNull { it.id == id }?.duration ?: 0 }
                Text("${p.songIds.size} songs · ${formatDuration(total)} · ${formatDate(p.createdAt)}", fontSize = 12.sp, color = colors.mutedForeground)
            }
            CircleIcon(Icons.Rounded.PlayCircle, "Play ${p.name}", onPlay, size = 32.dp, iconSize = 18.dp, tint = colors.primary, background = colors.primary.copy(alpha = 0.10f))
            CircleIcon(Icons.Rounded.Delete, "Delete ${p.name}", onDelete, size = 32.dp, iconSize = 15.dp, tint = colors.mutedForeground, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/** Also used for artist and album detail in [ExploreScreen]. */
@Composable
fun PlaylistDetail(
    name: String,
    createdAt: String?,
    gradient: Pair<Color, Color>,
    icon: ImageVector,
    songs: List<Song>,
    playlists: List<Playlist>,
    fadeUnliked: Boolean,
    activeId: Int?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlay: (song: Song, queue: List<Song>) -> Unit,
    onLike: (Song) -> Unit,
    onAddToPlaylist: (name: String, ids: Collection<Int>) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onRemove: ((Song) -> Unit)?,
) {
    val listState = rememberLazyListState()
    // Only one row sits open, and scrolling closes it (T27).
    var openRow by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf<Song?>(null) }
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openRow = null }

    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Column(
                Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 12.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)).background(gradientBrush(gradient)).padding(16.dp)
            ) {
                Row(
                    Modifier.clip(CircleShape).clickable(onClick = onBack).padding(end = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Back", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    Text("Back", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        val total = songs.sumOf { it.duration }
                        Text("${songs.size} songs · ${formatDuration(total)}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.65f))
                        if (createdAt != null) Text("Created ${formatDate(createdAt)}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.45f))
                    }
                }
                Row(
                    Modifier.padding(top = 16.dp).clip(CircleShape).background(Color.White)
                        .clickable { songs.firstOrNull()?.let { onPlay(it, songs) } }.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = gradient.first, modifier = Modifier.size(18.dp))
                    Text("Play", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = gradient.first)
                }
            }
        }
        itemsIndexed(songs, key = { _, s -> s.id }) { i, s ->
            // Inside a playlist the swipe takes the song out of it; on an artist or album screen it deletes the file.
            SwipeRow(
                open = openRow == s.id,
                enabled = true,
                label = if (onRemove != null) "Remove" else "Delete",
                icon = if (onRemove != null) Icons.Rounded.RemoveCircleOutline else Icons.Rounded.DeleteForever,
                tint = if (onRemove != null) colors.mutedForeground else colors.destructive,
                onOpenChange = { openRow = if (it) s.id else null },
                onAction = { if (onRemove != null) onRemove(s) else confirmDelete = s },
            ) {
                SongRow(
                    song = s, index = i, active = s.id == activeId, isPlaying = isPlaying, playlists = playlists,
                    onClick = { onPlay(s, songs) }, onLike = { onLike(s) },
                    onAddToPlaylist = { onAddToPlaylist(it.name, listOf(s.id)) }, showIndex = true,
                    onDelete = { confirmDelete = s },
                    onRemoveFromPlaylist = onRemove?.let { remove -> { remove(s) } },
                    modifier = Modifier.padding(horizontal = 8.dp).alpha(if (fadeUnliked && !s.liked) 0.4f else 1f),
                )
            }
        }
    }

    confirmDelete?.let { song ->
        DeleteFileDialog(song, onDismiss = { confirmDelete = null }, onConfirm = { confirmDelete = null; onDeleteSong(song) })
    }
}
