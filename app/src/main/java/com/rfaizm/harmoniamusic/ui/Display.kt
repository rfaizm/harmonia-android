package com.rfaizm.harmoniamusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.rfaizm.harmoniamusic.data.MusicRepository
import com.rfaizm.harmoniamusic.data.Playlist
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.UNKNOWN_ALBUM
import com.rfaizm.harmoniamusic.data.UNKNOWN_ARTIST
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// How the library looks on screen (T30): the models in data/ know nothing about colours or images.

// GUIDELINE §2 album-art gradients, assigned by index % 12.
val GRADIENTS = listOf(
    Color(0xFF4E7C65) to Color(0xFF2E5A48),
    Color(0xFF3A6878) to Color(0xFF1E4A5C),
    Color(0xFF6A5C7E) to Color(0xFF4A3E5E),
    Color(0xFF5E7050) to Color(0xFF3E5030),
    Color(0xFF7A5E52) to Color(0xFF5A3E32),
    Color(0xFF4A607A) to Color(0xFF2A4060),
    Color(0xFF6E4A62) to Color(0xFF4E2A42),
    Color(0xFF587A68) to Color(0xFF385A48),
    Color(0xFF7A6A4E) to Color(0xFF5A4A2E),
    Color(0xFF486878) to Color(0xFF285060),
    Color(0xFF5A4E7A) to Color(0xFF3A2E5A),
    Color(0xFF688060) to Color(0xFF486040),
)
val LIKED_GRADIENT = Color(0xFFC0392B) to Color(0xFF7A1A2E)

fun gradientFor(index: Int) = GRADIENTS[index.mod(GRADIENTS.size)]

val Song.gradient get() = gradientFor(id)
val Playlist.gradient get() = gradientFor(gradientIndex)

fun formatDuration(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun formatDate(iso: String): String {
    val (y, m, d) = iso.split("-")
    return "${MONTHS[m.toInt() - 1]} ${d.toInt()}, $y"
}

/**
 * The library, for composables deep in the tree that show album art. HarmoniaApp provides it; previews don't,
 * so they show the gradient.
 */
val LocalMusic = staticCompositionLocalOf<MusicRepository?> { null }

/** The song's art once it has been read from the file, or null while loading and when there is none. */
@Composable
fun albumArtOf(songId: Int?): ImageBitmap? {
    if (songId == null) return null
    val music = LocalMusic.current ?: return null
    var art by remember(songId) { mutableStateOf(music.cachedArt(songId)?.asImageBitmap()) }
    LaunchedEffect(songId) {
        if (art == null) art = withContext(Dispatchers.IO) { music.loadArt(songId) }?.asImageBitmap()
    }
    return art
}

/**
 * The artist under a song's title, or nothing when the file has no artist tag (M11, T46). A library of downloaded
 * files would otherwise read "Unknown artist" on almost every row. The file-name guess isn't shown: "Title - Artist"
 * names guess the wrong way round, and a wrong artist is worse than none.
 */
internal fun artistLine(song: Song): String? = song.displayArtist.takeUnless { it.isBlank() || it == UNKNOWN_ARTIST }

/** The full player's "artist · album", keeping only the halves that are known. */
internal fun playerSubtitle(song: Song): String? =
    listOfNotNull(artistLine(song), song.album.takeUnless { it.isBlank() || it == UNKNOWN_ALBUM }).joinToString(" · ").ifEmpty { null }

/**
 * The letter a tile shows when a song has no cover art (M11, T47), so a library of untagged files isn't a wall of
 * identical music notes. Accents fold as in the A–Z bar (`letterOf`); anything filed under "#" keeps the icon.
 */
internal fun tileLetter(label: String?): String? = label?.let(::letterOf)?.takeUnless { it == "#" }
