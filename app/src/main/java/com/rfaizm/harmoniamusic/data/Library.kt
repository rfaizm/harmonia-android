package com.rfaizm.harmoniamusic.data

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Process-level library, so rotation doesn't rescan and the playback service (T6) can reach it.
 * Seed data in SampleData.kt is for @Previews only.
 */
object Library {
    val songs = mutableStateListOf<Song>()
    val playlists = mutableStateListOf<Playlist>()

    /** False until the first scan finishes, so the UI can tell "still loading" from "no music". */
    var loaded by mutableStateOf(false)
        private set

    /** Needs the audio permission; call on the main thread. */
    suspend fun scan(context: Context) {
        val fresh = withContext(Dispatchers.IO) { query(context.contentResolver) }
        val merged = keepUserState(fresh, songs.toList())
        songs.clear()
        songs.addAll(merged)
        loaded = true
    }
}

// PRD phase 1: MediaStore already has the tags, so no file is opened here.
private fun query(resolver: ContentResolver): List<Song> {
    val columns = arrayOf(Media._ID, Media.TITLE, Media.ARTIST, Media.ALBUM, Media.YEAR, Media.DURATION, Media.DISPLAY_NAME)
    // Settings › Filters: "Hide clips shorter than 30 seconds".
    val selection = "${Media.IS_MUSIC} != 0 AND ${Media.DURATION} >= 30000"
    return resolver.query(Media.EXTERNAL_CONTENT_URI, columns, selection, null, null)?.use { c ->
        buildList(c.count) {
            while (c.moveToNext()) {
                add(toSong(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4), c.getLong(5), c.getString(6)))
            }
        }
    }.orEmpty()
}

/** PRD phase 2: an empty title falls back to the file name; missing tags get readable placeholders. */
internal fun toSong(id: Long, title: String?, artist: String?, album: String?, year: Int, durationMs: Long, fileName: String?) = Song(
    id = id.toInt(), // ponytail: MediaStore _ID fits Int on real devices; make Song.id a Long if one ever overflows
    title = title.known() ?: fileName?.substringBeforeLast('.').known() ?: "Unknown title",
    artist = artist.known() ?: "Unknown artist",
    album = album.known() ?: "Unknown album",
    year = year,
    duration = (durationMs / 1000).toInt(),
)

private fun String?.known() = takeUnless { it.isNullOrBlank() || it == MediaStore.UNKNOWN_STRING }

/** A rescan builds fresh Songs; carry likes and play counts over by id. */
internal fun keepUserState(fresh: List<Song>, old: List<Song>): List<Song> {
    val byId = old.associateBy { it.id }
    return fresh.map { s -> byId[s.id]?.let { s.copy(liked = it.liked, playCount = it.playCount) } ?: s }
}
