package com.rfaizm.harmoniamusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfaizm.harmoniamusic.data.Playlist
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.gradientFor
import com.rfaizm.harmoniamusic.ui.theme.border
import com.rfaizm.harmoniamusic.ui.theme.card
import com.rfaizm.harmoniamusic.ui.theme.muted
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground

@Composable
fun ExploreScreen(
    songs: List<Song>,
    playlists: List<Playlist>,
    activeId: Int?,
    isPlaying: Boolean,
    onPlay: (song: Song, queue: List<Song>) -> Unit,
    onLike: (Song) -> Unit,
    onAddToPlaylist: (name: String, ids: Collection<Int>) -> Unit,
) {
    var albumsView by rememberSaveable { mutableStateOf(false) }
    // Open artist or album name (which one follows albumsView); local state, like PlaylistsScreen.
    var openName by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(openName != null) { openName = null }
    // derivedStateOf, not remember(songs) alone: the list instance never changes, so a rescan would never regroup.
    val artists by remember(songs) { derivedStateOf { songs.groupBy { it.displayArtist }.toList().sortedBy { it.first.lowercase() } } }
    val albums by remember(songs) { derivedStateOf { songs.groupBy { it.album }.toList().sortedBy { it.first.lowercase() } } }

    AnimatedContent(openName, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "exploreDetail") { open ->
        if (open != null) {
            // Filter the live list (not the remembered groups) so likes and play counts stay current.
            val list = songs.filter { if (albumsView) it.album == open else it.displayArtist == open }
            PlaylistDetail(
                name = open, createdAt = null, gradient = gradientFor(list.firstOrNull()?.id ?: 0),
                icon = if (albumsView) Icons.Rounded.Album else Icons.Rounded.Mic,
                songs = list, playlists = playlists, fadeUnliked = false, activeId = activeId, isPlaying = isPlaying,
                onBack = { openName = null }, onPlay = onPlay, onLike = onLike, onAddToPlaylist = onAddToPlaylist, onRemove = null,
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.muted).padding(4.dp)) {
                    listOf("Artists", "Albums").forEachIndexed { i, label ->
                        val active = albumsView == (i == 1)
                        Box(
                            Modifier.weight(1f)
                                .then(if (active) Modifier.shadow(1.dp, RoundedCornerShape(12.dp)) else Modifier)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) colors.card else Color.Transparent)
                                .clickable { albumsView = i == 1 }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (active) colors.onBackground else colors.mutedForeground)
                        }
                    }
                }

                AnimatedContent(
                    albumsView,
                    transitionSpec = {
                        val dir = if (targetState) 1 else -1
                        (slideInHorizontally(tween(180)) { dir * 36 } + fadeIn(tween(180))) togetherWith
                            (slideOutHorizontally(tween(180)) { -dir * 36 } + fadeOut(tween(180)))
                    },
                    label = "explore"
                ) { showAlbums ->
                    if (!showAlbums) {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                            itemsIndexed(artists, key = { _, a -> a.first }) { i, (name, list) ->
                                if (i > 0) HorizontalDivider(color = colors.border)
                                Row(
                                    Modifier.enterAnimation().fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { openName = name }.padding(horizontal = 8.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    AlbumArt(gradientFor(list.first().id), 48.dp, shape = CircleShape, icon = Icons.Rounded.Mic)
                                    Column(Modifier.weight(1f)) {
                                        Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        val albumCount = list.distinctBy { it.album }.size
                                        Text("${list.size} songs · $albumCount ${if (albumCount == 1) "album" else "albums"}", fontSize = 12.sp, color = colors.mutedForeground)
                                    }
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.mutedForeground, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            GridCells.Fixed(2),
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(albums, key = { _, a -> a.first }) { i, (album, list) ->
                                val shape = RoundedCornerShape(16.dp)
                                Column(Modifier.enterAnimation().clip(shape).background(colors.card).border(1.dp, colors.border, shape).clickable { openName = album }) {
                                    Box(Modifier.fillMaxWidth().height(112.dp).background(gradientBrush(gradientFor(list.first().id))), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Album, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(44.dp))
                                    }
                                    Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 12.dp)) {
                                        Text(album, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(list.first().displayArtist, fontSize = 11.sp, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(listOfNotNull(list.first().year.takeIf { it > 0 }, "${list.size} tracks").joinToString(" · "), fontSize = 10.sp, color = colors.mutedForeground.copy(alpha = 0.55f), modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
