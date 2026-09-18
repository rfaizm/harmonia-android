package com.rfaizm.harmoniamusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfaizm.harmoniamusic.data.Playlist
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.ui.theme.border
import com.rfaizm.harmoniamusic.ui.theme.card
import com.rfaizm.harmoniamusic.ui.theme.muted
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground

private enum class SortBy(val label: String) { AZ("A–Z"), Recent("Recent"), MostPlayed("Most played") }

private sealed interface Entry {
    data class Header(val letter: String) : Entry
    data class Item(val song: Song, val index: Int) : Entry
}

@Composable
fun SongsScreen(
    songs: List<Song>,
    playlists: List<Playlist>,
    activeId: Int?,
    isPlaying: Boolean,
    onPlay: (song: Song, queue: List<Song>) -> Unit,
    onLike: (Song) -> Unit,
    onAddToPlaylist: (name: String, ids: Collection<Int>) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sortBy by rememberSaveable { mutableStateOf(SortBy.AZ) }
    var selected by remember { mutableStateOf(emptySet<Int>()) }
    var sheetOpen by remember { mutableStateOf(false) }
    val selectionMode = selected.isNotEmpty()
    fun toggle(id: Int) { selected = if (id in selected) selected - id else selected + id }

    BackHandler(selectionMode) { selected = emptySet() }

    // derivedStateOf re-runs when the song list itself changes (like, play count), not just on query/sort.
    val entries by remember(songs) { derivedStateOf {
        val q = query.trim().lowercase()
        val filtered = songs.filter {
            q.isEmpty() || it.displayTitle.lowercase().contains(q) || it.displayArtist.lowercase().contains(q)
        }
        val sorted = when (sortBy) {
            SortBy.AZ -> filtered.sortedBy { it.displayTitle.lowercase() }
            SortBy.Recent -> filtered.sortedByDescending { it.id }
            SortBy.MostPlayed -> filtered.sortedByDescending { it.playCount }
        }
        buildList {
            var last: String? = null
            sorted.forEachIndexed { i, s ->
                if (sortBy == SortBy.AZ) {
                    val c = s.displayTitle.firstOrNull()?.uppercaseChar()
                    val letter = if (c != null && c in 'A'..'Z') c.toString() else "#"
                    if (letter != last) add(Entry.Header(letter)).also { last = letter }
                }
                add(Entry.Item(s, i))
            }
        }
    } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AnimatedContent(selectionMode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "songsHeader") { selecting ->
                if (selecting) {
                    SelectionBar(
                        count = selected.size,
                        onClose = { selected = emptySet() },
                        onSelectAll = { selected = songs.map { it.id }.toSet() },
                    )
                } else {
                    Column(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 6.dp)) {
                        SearchBar(query, { query = it })
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SortBy.entries.forEach { Pill(it.label, sortBy == it) { sortBy = it } }
                        }
                    }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = if (selectionMode) 96.dp else 12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (entries.isEmpty()) item {
                    EmptyState(Icons.Rounded.SearchOff, "No matches", "Nothing in your library matches “$query”.")
                }
                items(entries, key = { if (it is Entry.Item) it.song.id else "h" + (it as Entry.Header).letter }) { e ->
                    when (e) {
                        is Entry.Header -> LetterDivider(e.letter)
                        is Entry.Item -> SongRow(
                            song = e.song,
                            index = e.index,
                            active = e.song.id == activeId,
                            isPlaying = isPlaying,
                            playlists = playlists,
                            onClick = { if (selectionMode) toggle(e.song.id) else onPlay(e.song, entries.mapNotNull { (it as? Entry.Item)?.song }) },
                            onLike = { onLike(e.song) },
                            onAddToPlaylist = { onAddToPlaylist(it.name, listOf(e.song.id)) },
                            selectionMode = selectionMode,
                            selected = e.song.id in selected,
                            onLongClick = { toggle(e.song.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            selectionMode,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = { sheetOpen = true },
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                text = { Text("Add ${selected.size} ${if (selected.size == 1) "song" else "songs"}", fontWeight = FontWeight.Bold) },
            )
        }
    }

    if (sheetOpen) {
        AddToPlaylistSheet(
            count = selected.size,
            playlists = playlists,
            onDismiss = { sheetOpen = false },
            onPick = { onAddToPlaylist(it, selected); sheetOpen = false; selected = emptySet() },
        )
    }
}

@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, onSelectAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircleIcon(Icons.Rounded.Close, "Cancel selection", onClose, size = 36.dp, iconSize = 18.dp, tint = colors.onBackground, background = colors.card)
        Text("$count selected", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Row(
            Modifier.clip(CircleShape).background(colors.primary.copy(alpha = 0.12f)).clickable(onClick = onSelectAll).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.DoneAll, null, tint = colors.primary, modifier = Modifier.size(15.dp))
            Text("Select all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
        }
    }
}

/** PRD phase 3: inline naming in a bottom sheet; duplicate names offer a merge instead of an error. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToPlaylistSheet(count: Int, playlists: List<Playlist>, onDismiss: () -> Unit, onPick: (name: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val existing = playlists.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.card) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding()) {
            Text("Add $count ${if (count == 1) "song" else "songs"} to…", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text("Name a new playlist or pick an existing one.", fontSize = 13.sp, color = colors.mutedForeground, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.muted).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    if (name.isEmpty()) Text("New playlist name…", fontSize = 14.sp, color = colors.mutedForeground)
                    BasicTextField(
                        name, { name = it }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = colors.onBackground),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                CircleIcon(
                    Icons.Rounded.Check, "Create playlist", { if (name.isNotBlank()) onPick(name.trim()) }, size = 32.dp, iconSize = 15.dp,
                    tint = colors.onPrimary, background = if (name.isBlank()) colors.primary.copy(alpha = 0.4f) else colors.primary
                )
            }

            AnimatedVisibility(existing != null) {
                Row(
                    Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(colors.primary.copy(alpha = 0.10f)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Info, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Text(
                        "“${existing?.name}” already exists. Tap ✓ to merge these songs into it.",
                        fontSize = 12.sp, color = colors.onBackground, lineHeight = 17.sp
                    )
                }
            }

            SectionHeading("Existing playlists", Modifier.padding(top = 20.dp, bottom = 8.dp))
            Column(Modifier.clip(RoundedCornerShape(16.dp)).border(1.dp, colors.border, RoundedCornerShape(16.dp))) {
                playlists.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(p.name) }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AlbumArt(p.gradient, 40.dp, icon = Icons.AutoMirrored.Rounded.PlaylistAdd)
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("${p.songIds.size} songs", fontSize = 12.sp, color = colors.mutedForeground)
                        }
                    }
                }
            }
        }
    }
}
