package com.rfaizm.harmoniamusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfaizm.harmoniamusic.data.Playlist
import com.rfaizm.harmoniamusic.data.SEED_PLAYLISTS
import com.rfaizm.harmoniamusic.data.SEED_SONGS
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.formatDuration
import com.rfaizm.harmoniamusic.ui.theme.HarmoniaTheme
import com.rfaizm.harmoniamusic.ui.theme.LikedRed
import com.rfaizm.harmoniamusic.ui.theme.border
import com.rfaizm.harmoniamusic.ui.theme.card
import com.rfaizm.harmoniamusic.ui.theme.destructive
import com.rfaizm.harmoniamusic.ui.theme.muted
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground
import kotlin.math.min
import kotlinx.coroutines.delay

val colors @Composable get() = MaterialTheme.colorScheme

fun gradientBrush(g: Pair<Color, Color>) = Brush.linearGradient(listOf(g.first, g.second))

/** Small circular icon button (lucide-style `p-1.5 rounded-full`). */
@Composable
fun CircleIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    iconSize: Dp = 15.dp,
    tint: Color = colors.mutedForeground,
    background: Color = Color.Transparent,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun AlbumArt(
    gradient: Pair<Color, Color>,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    icon: ImageVector = Icons.Rounded.MusicNote,
) {
    Box(
        modifier.size(size).clip(shape).background(gradientBrush(gradient)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(size * 0.36f))
    }
}

@Composable
fun HeartButton(liked: Boolean, onClick: () -> Unit, iconSize: Dp = 15.dp, unlikedTint: Color = colors.mutedForeground) {
    // Micro "pop" when liking (PRD phase 8).
    val scale by animateFloatAsState(if (liked) 1f else 0.9f, label = "heart")
    CircleIcon(
        icon = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        label = if (liked) "Unlike" else "Like",
        onClick = onClick,
        iconSize = iconSize,
        tint = if (liked) LikedRed else unlikedTint,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        size = iconSize * 2,
    )
}

// Rows that first appear in the same frame (a list opening, a sort change) share a burst and
// get staggered 30ms apart; a row scrolled in on its own is slot 0, so it never waits.
private object EnterStagger {
    private var frame = -1L
    private var slot = 0
    fun next(frameMillis: Long): Int {
        if (frameMillis != frame) { frame = frameMillis; slot = 0 } else slot++
        return slot
    }
}

/** Row entry: fade + rise 8dp, staggered (GUIDELINE §10). Runs once per item. */
@Composable
fun Modifier.enterAnimation(): Modifier {
    var shown by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (shown) return@LaunchedEffect
        val slot = EnterStagger.next(withFrameMillis { it })
        delay(min(slot * 30L, 300L))
        progress.animateTo(1f, tween(260))
        shown = true
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1 - progress.value) * 8.dp.toPx()
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.Search, null, tint = colors.mutedForeground, modifier = Modifier.size(16.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Search songs, artists…", fontSize = 14.sp, color = colors.mutedForeground)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = colors.onBackground),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        AnimatedVisibility(query.isNotEmpty(), enter = scaleIn(initialScale = 0.7f) + fadeIn(), exit = scaleOut(targetScale = 0.7f) + fadeOut()) {
            CircleIcon(Icons.Rounded.Close, "Clear search", { onQueryChange("") }, size = 18.dp, iconSize = 13.dp, background = colors.muted)
        }
    }
}

@Composable
fun Pill(text: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (active) colors.onPrimary else colors.mutedForeground,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) colors.primary else colors.muted)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
fun LetterDivider(letter: String) {
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(letter.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = colors.primary, letterSpacing = 1.5.sp)
        Box(Modifier.weight(1f).height(1.dp).background(colors.border))
    }
}

@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        color = colors.primary,
        letterSpacing = 1.4.sp,
        modifier = modifier
    )
}

/** Three bouncing bars shown on the active track (GUIDELINE §10). */
@Composable
fun Soundbar(color: Color = colors.primary, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "soundbar")
    Row(modifier.height(14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { i ->
            val s by t.animateFloat(
                0.3f, 1f,
                infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(i * 150)),
                label = "bar$i"
            )
            Box(
                Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .graphicsLayer { scaleY = s; transformOrigin = TransformOrigin(0.5f, 1f) }
                    .background(color, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    index: Int,
    active: Boolean,
    isPlaying: Boolean,
    playlists: List<Playlist>,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onAddToPlaylist: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
    showIndex: Boolean = false,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        when {
            selected -> colors.primary.copy(alpha = 0.18f)
            active -> colors.primary.copy(alpha = 0.10f)
            else -> Color.Transparent
        },
        label = "rowBg"
    )

    Row(
        modifier
            .fillMaxWidth()
            .enterAnimation()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectionMode) SelectDot(selected)
        if (showIndex) {
            Box(Modifier.width(24.dp).height(48.dp), contentAlignment = Alignment.Center) {
                if (active && isPlaying) Soundbar()
                else Text("${index + 1}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (active) colors.primary else colors.mutedForeground)
            }
        } else {
            AlbumArt(song.gradient, 48.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(
                song.displayTitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = if (active) colors.primary else colors.onBackground,
                maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp
            )
            Text(
                song.displayArtist, fontSize = 12.sp, color = colors.mutedForeground,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (!selectionMode) {
            Text(formatDuration(song.duration), fontSize = 12.sp, color = colors.mutedForeground.copy(alpha = 0.7f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeartButton(song.liked, onLike)
                Box {
                    CircleIcon(Icons.Rounded.MoreVert, "More options", { menuOpen = true })
                    SongMenu(
                        expanded = menuOpen,
                        song = song,
                        playlists = playlists,
                        onDismiss = { menuOpen = false },
                        onAddToPlaylist = onAddToPlaylist,
                        onRemoveFromPlaylist = onRemoveFromPlaylist,
                        onDeleteFile = { menuOpen = false; confirmDelete = true },
                    )
                }
            }
        }
    }
    if (confirmDelete) DeleteFileDialog(song, onDismiss = { confirmDelete = false })
}

@Composable
private fun SelectDot(selected: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primary else Color.Transparent)
            .border(1.5.dp, if (selected) colors.primary else colors.mutedForeground.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Rounded.Check, null, tint = colors.onPrimary, modifier = Modifier.size(14.dp))
    }
}

/**
 * 3-dot menu (GUIDELINE §5.7). "Remove from playlist" and "Delete file" sit at opposite
 * ends with different icons (PRD phase 7, safe deletion).
 */
@Composable
private fun SongMenu(
    expanded: Boolean,
    song: Song,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Playlist) -> Unit,
    onRemoveFromPlaylist: (() -> Unit)?,
    onDeleteFile: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = colors.card,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier.width(208.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(song.displayTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.displayArtist, fontSize = 12.sp, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        HorizontalDivider(color = colors.border)
        Text(
            "ADD TO PLAYLIST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.primary, letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
        )
        playlists.forEach { p ->
            MenuRow(Icons.AutoMirrored.Rounded.PlaylistAdd, p.name, colors.onBackground) { onAddToPlaylist(p); onDismiss() }
        }
        if (onRemoveFromPlaylist != null) {
            HorizontalDivider(color = colors.border, modifier = Modifier.padding(top = 4.dp))
            MenuRow(Icons.Rounded.RemoveCircleOutline, "Remove from playlist", colors.onBackground) { onRemoveFromPlaylist(); onDismiss() }
        }
        // Deliberate gap before the destructive action.
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = colors.border)
        MenuRow(Icons.Rounded.DeleteForever, "Delete file", colors.destructive, onDeleteFile)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
        Text(text, fontSize = 14.sp, color = color)
    }
}

@Composable
fun DeleteFileDialog(song: Song, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(Modifier.size(48.dp).clip(CircleShape).background(colors.destructive.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.DeleteForever, null, tint = colors.destructive)
            }
        },
        title = { Text("Delete file permanently?", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
        text = {
            Text(
                "“${song.displayTitle}” will be removed from your device. This can't be undone.",
                fontSize = 14.sp, color = colors.mutedForeground, textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = colors.destructive, contentColor = Color.White)) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.mutedForeground) } },
    )
}

/** Custom switch (GUIDELINE §5.9). */
@Composable
fun HarmoniaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val track by animateColorAsState(if (checked) colors.primary else colors.muted, label = "track")
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, label = "thumb")
    Box(
        Modifier
            .size(width = 44.dp, height = 24.dp)
            .clip(CircleShape)
            .background(track)
            .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
    ) {
        Box(Modifier.offset(x = x, y = 2.dp).size(20.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    sub: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = colors.primary, modifier = Modifier.size(15.dp)) }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (sub != null) Text(sub, fontSize = 12.sp, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        trailing?.invoke()
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 20.dp)) {
        SectionHeading(title, Modifier.padding(start = 4.dp, bottom = 8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
        ) { content() }
    }
}

/** PRD phase 1: permission-denied / empty-library states. */
@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, action: String? = null, onAction: () -> Unit = {}, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = colors.primary, modifier = Modifier.size(36.dp))
        }
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp), textAlign = TextAlign.Center)
        Text(body, fontSize = 13.sp, color = colors.mutedForeground, textAlign = TextAlign.Center, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp))
        if (action != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
            ) { Text(action, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun PermissionEmptyState(onGrant: () -> Unit = {}) = EmptyState(
    Icons.Rounded.FolderOpen,
    "Let Harmonia find your music",
    "We only read audio files on this device to build your library. Nothing leaves your phone.",
    "Grant access",
    onGrant,
)

@Composable
fun NoMusicEmptyState(onRefresh: () -> Unit = {}) = EmptyState(
    Icons.Rounded.LibraryMusic,
    "No songs yet",
    "Copy some audio files to your phone's Music or Download folder, then pull down to refresh.",
    "Scan again",
    onRefresh,
)

@Preview(showBackground = true, backgroundColor = 0xFF1E2022)
@Composable
private fun EmptyStatesPreview() = HarmoniaTheme(darkTheme = true) {
    Column(Modifier.background(colors.background)) {
        PermissionEmptyState()
        NoMusicEmptyState()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F4F0)
@Composable
private fun SongRowPreview() = HarmoniaTheme(darkTheme = false) {
    Column(Modifier.background(colors.background).padding(8.dp)) {
        SEED_SONGS.take(3).forEachIndexed { i, s ->
            SongRow(s, i, active = i == 1, isPlaying = true, playlists = SEED_PLAYLISTS, onClick = {}, onLike = {}, onAddToPlaylist = {})
        }
        SongRow(SEED_SONGS[3], 3, false, false, SEED_PLAYLISTS, {}, {}, {}, selectionMode = true, selected = true, modifier = Modifier.alpha(1f))
    }
}
