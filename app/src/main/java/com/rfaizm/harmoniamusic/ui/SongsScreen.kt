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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import com.rfaizm.harmoniamusic.data.Playlist
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.ui.theme.border
import com.rfaizm.harmoniamusic.ui.theme.destructive
import com.rfaizm.harmoniamusic.ui.theme.card
import com.rfaizm.harmoniamusic.ui.theme.muted
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground
import java.text.Normalizer

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
    onDelete: (Song) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sortBy by rememberSaveable { mutableStateOf(SortBy.AZ) }
    var selected by remember { mutableStateOf(emptySet<Int>()) }
    var sheetOpen by remember { mutableStateOf(false) }
    val selectionMode = selected.isNotEmpty()
    fun toggle(id: Int) { selected = if (id in selected) selected - id else selected + id }

    BackHandler(selectionMode) { selected = emptySet() }

    // Drag to select (PRD phase 3 "swipe"). The long press belongs to this gesture alone: while the row also
    // handled it, the row consumed the event first and the drag detector was cancelled before it could start.
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    // Only one row sits open, and scrolling or starting a selection closes it (T27).
    var openRow by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf<Song?>(null) }
    LaunchedEffect(listState.isScrollInProgress, selectionMode) { if (listState.isScrollInProgress || selectionMode) openRow = null }
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragBase by remember { mutableStateOf(emptySet<Int>()) }
    var scrollSpeed by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(dragFrom != null) {
        while (dragFrom != null) {
            if (scrollSpeed != 0f) listState.scrollBy(scrollSpeed)
            withFrameNanos { } // one step per frame, so the speed doesn't depend on how fast events arrive
        }
    }

    // derivedStateOf re-runs when the song list itself changes (like, play count), not just on query/sort.
    val entries by remember(songs) { derivedStateOf {
        val q = query.trim().lowercase()
        val filtered = songs.filter {
            q.isEmpty() || it.displayTitle.lowercase().contains(q) || it.displayArtist.lowercase().contains(q)
        }
        // A–Z carries each song's letter group, so the headings come from the same rule as the order.
        val sorted = when (sortBy) {
            SortBy.AZ -> azOrder(filtered)
            SortBy.Recent -> filtered.sortedByDescending { it.id }.map { null to it }
            SortBy.MostPlayed -> filtered.sortedByDescending { it.playCount }.map { null to it }
        }
        buildList {
            var last: String? = null
            sorted.forEachIndexed { i, (letter, s) ->
                if (letter != null && letter != last) add(Entry.Header(letter)).also { last = letter }
                add(Entry.Item(s, i))
            }
        }
    } }
    // Where each letter's heading sits, for the index bar (T41).
    val headings = remember(entries) { entries.withIndex().mapNotNull { (i, e) -> (e as? Entry.Header)?.let { it.letter to i } } }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 6.dp)) {
                // Search stays put while selecting: picking 15 songs out of a long library shouldn't mean scrolling
                // to each one (PRD phase 3 is about saving taps).
                SearchBar(query, { query = it })
                AnimatedContent(selectionMode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "songsHeader") { selecting ->
                    if (selecting) {
                        SelectionBar(
                            count = selected.size,
                            filtered = query.isNotBlank(),
                            onClose = { selected = emptySet() },
                            // Adds what the search is showing, so selections made under an earlier search survive.
                            onSelectAll = { selected = selected + entries.mapNotNull { (it as? Entry.Item)?.song?.id } },
                        )
                    } else {
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SortBy.entries.forEach { Pill(it.label, sortBy == it) { sortBy = it } }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = if (selectionMode) 96.dp else 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // Only runs after a long press, so ordinary scrolling and flinging are untouched.
                        .pointerInput(entries) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { start ->
                                    val row = listState.rowAt(start.y)
                                    // The press itself still selects that one song, exactly as it used to.
                                    (entries.getOrNull(row ?: -1) as? Entry.Item)?.let {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        toggle(it.song.id)
                                    }
                                    dragBase = selected // after the toggle, so sliding back returns to it
                                    dragFrom = row
                                },
                                onDragEnd = { dragFrom = null; scrollSpeed = 0f },
                                onDragCancel = { dragFrom = null; scrollSpeed = 0f },
                            ) { change, _ ->
                                val from = dragFrom ?: return@detectDragGesturesAfterLongPress
                                scrollSpeed = edgeScrollSpeed(change.position.y, size.height)
                                val to = listState.rowAt(change.position.y) ?: return@detectDragGesturesAfterLongPress
                                // Rebuilt from the selection the drag started with, so sliding back de-selects again.
                                val covered = (minOf(from, to)..maxOf(from, to)).mapNotNull { i ->
                                    (entries.getOrNull(i) as? Entry.Item)?.song?.id
                                }
                                selected = dragBase + covered
                            }
                        }
                ) {
                    if (entries.isEmpty()) item {
                        EmptyState(Icons.Rounded.SearchOff, "No matches", "Nothing in your library matches “$query”.")
                    }
                    items(entries, key = { if (it is Entry.Item) it.song.id else "h" + (it as Entry.Header).letter }) { e ->
                        when (e) {
                            is Entry.Header -> LetterDivider(e.letter)
                            is Entry.Item -> SwipeRow(
                                open = openRow == e.song.id,
                                enabled = !selectionMode,
                                label = "Delete",
                                icon = Icons.Rounded.DeleteForever,
                                tint = colors.destructive,
                                onOpenChange = { openRow = if (it) e.song.id else null },
                                onAction = { confirmDelete = e.song },
                            ) { SongRow(
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
                                onDelete = { confirmDelete = e.song },
                                modifier = Modifier.animateItem(),
                            ) }
                        }
                    }
                }
                if (sortBy == SortBy.AZ && headings.size >= 2) {
                    AlphabetIndex(
                        letters = headings.map { it.first },
                        onPick = { i -> scope.launch { listState.scrollToItem(headings[i].second) } },
                        // Clear of the "Add N songs" button while selecting.
                        modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp, bottom = if (selectionMode) 80.dp else 0.dp),
                    )
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

    confirmDelete?.let { song ->
        // PRD phase 7: the warning comes from the screen now, because the menu and the swipe both ask for it.
        DeleteFileDialog(song, onDismiss = { confirmDelete = null }, onConfirm = { confirmDelete = null; onDelete(song) })
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

/** The row under the finger, letter headers included; null when the finger is past the last row. */
private fun LazyListState.rowAt(y: Float): Int? =
    layoutInfo.visibleItemsInfo.firstOrNull { y >= it.offset && y < it.offset + it.size }?.index

/** Pixels to scroll per frame while a selection drag sits near an edge; faster the closer it gets. */
internal fun edgeScrollSpeed(y: Float, height: Int): Float {
    val zone = height * 0.12f
    return when {
        zone <= 0f -> 0f
        y < zone -> -(zone - y) / zone * 24f
        y > height - zone -> (y - (height - zone)) / zone * 24f
        else -> 0f
    }
}

/**
 * T41: the letters the list has, down its right side, beside the rows rather than over their buttons. Touching or
 * sliding along it jumps the list straight to that letter's heading, with a light tick each time the letter changes.
 */
@Composable
private fun AlphabetIndex(letters: List<String>, onPick: (index: Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val pick by rememberUpdatedState(onPick)
    Column(
        modifier
            .width(24.dp)
            .heightIn(max = 20.dp * letters.size) // a few letters stay close together instead of spreading out
            .fillMaxHeight()
            .pointerInput(letters) {
                awaitEachGesture {
                    var last = -1
                    fun touch(y: Float) {
                        val i = letterAt(y, size.height, letters.size)
                        if (i == last) return
                        last = i
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pick(i)
                    }
                    val down = awaitFirstDown()
                    down.consume()
                    touch(down.position.y)
                    drag(down.id) { change ->
                        change.consume()
                        touch(change.position.y)
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(letter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.mutedForeground)
            }
        }
    }
}

@Composable
private fun SelectionBar(count: Int, filtered: Boolean, onClose: () -> Unit, onSelectAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
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
            Text(if (filtered) "Select matches" else "Select all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
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

/** The A–Z group a title is filed under: its first letter with any accent taken off ("Élan" is E), or "#". */
internal fun letterOf(title: String): String {
    // Only the first character is normalised, so a long list stays cheap to sort.
    val first = Normalizer.normalize(title.take(1), Normalizer.Form.NFD).firstOrNull()?.uppercaseChar()
    return if (first != null && first in 'A'..'Z') first.toString() else "#"
}

/**
 * The A–Z order, by group and then by title, with "#" last as in the phone's contacts. Sorting by title alone put
 * digits before "a" and accented or non-Latin titles after "z": two "#" groups whose headings shared a list key,
 * which crashes the list (T41).
 */
internal fun azOrder(songs: List<Song>): List<Pair<String, Song>> = songs
    .map { letterOf(it.displayTitle) to it }
    .sortedWith(compareBy({ it.first == "#" }, { it.first }, { it.second.displayTitle.lowercase() }))

/** Which of [count] evenly spread letters a finger at [y] is on; past either end it keeps the end letter. */
internal fun letterAt(y: Float, height: Int, count: Int): Int =
    if (height <= 0 || count <= 0) 0 else (y / height * count).toInt().coerceIn(0, count - 1)
