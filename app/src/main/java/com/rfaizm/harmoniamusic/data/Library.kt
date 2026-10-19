package com.rfaizm.harmoniamusic.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Process-level library, so rotation doesn't rescan and the playback service (T6) can reach it.
 * Seed data in ui/PreviewData.kt is for @Previews only.
 */
object Library {
    val songs = mutableStateListOf<Song>()
    val playlists = mutableStateListOf<Playlist>()

    /** False until the first scan finishes, so the UI can tell "still loading" from "no music". */
    var loaded by mutableStateOf(false)
        private set

    // Likes and play counts read from disk, as id-only songs; applied to the first scan by keepUserState.
    private var saved = emptyList<Song>()
    private var restored = false
    private val writes = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadLock = Mutex() // the UI and the service both ask for the library at launch

    /** Needs the audio permission; call on the main thread. */
    suspend fun scan(context: Context) = loadLock.withLock { scanNow(context) }

    /** Restores and scans once, whoever asks first; a second caller waits for that scan instead of repeating it. */
    suspend fun ensureLoaded(context: Context) = loadLock.withLock {
        restore(context)
        if (!loaded) scanNow(context)
    }

    private suspend fun scanNow(context: Context) {
        // Without the audio permission the query throws; leave [loaded] false so a later grant still scans.
        val fresh = withContext(Dispatchers.IO) { context.dataSources().mediaStore().songs() } ?: return
        // Nothing scanned yet means this is the first scan of the process, so the saved state is the newest.
        val merged = keepUserState(fresh, songs.toList().ifEmpty { saved })
        songs.clear()
        songs.addAll(merged)
        loaded = true
    }

    /** Reads what MediaStore can't tell us. Call once before the first [scan]; later calls do nothing. */
    suspend fun restore(context: Context) {
        if (restored) return
        restored = true
        val text = context.dataSources().libraryStore().read() ?: return
        val (savedSongs, savedPlaylists) = parseState(text)
        saved = savedSongs
        playlists.clear()
        playlists.addAll(savedPlaylists)
    }

    /** The song's file is gone, so drop it from the library and every playlist. */
    fun remove(context: Context, songId: Int) {
        songs.removeAll { it.id == songId }
        playlists.removeSongEverywhere(songId)
        save(context)
    }

    /** Call after every like, play count or playlist change. Snapshots on the caller's thread, writes on IO. */
    fun save(context: Context) {
        val json = stateJson(songs.toList(), playlists.toList())
        val store = context.dataSources().libraryStore()
        writes.launch { store.write(json) }
    }
}

/** Only what a rescan can't rebuild: likes, play counts and playlists. */
internal fun stateJson(songs: List<Song>, playlists: List<Playlist>): String {
    val songArray = JSONArray()
    songs.filter { it.liked || it.playCount > 0 }.forEach {
        songArray.put(JSONObject().put("id", it.id).put("liked", it.liked).put("plays", it.playCount))
    }
    val playlistArray = JSONArray()
    playlists.forEach {
        playlistArray.put(
            JSONObject().put("id", it.id).put("name", it.name).put("songs", JSONArray(it.songIds))
                .put("gradient", it.gradientIndex).put("created", it.createdAt)
        )
    }
    return JSONObject().put("songs", songArray).put("playlists", playlistArray).toString()
}

/**
 * Songs come back carrying only id, liked and playCount, for [keepUserState] to merge onto a fresh scan.
 * A truncated or hand-edited file reads as empty rather than crashing the app on launch.
 */
internal fun parseState(json: String): Pair<List<Song>, List<Playlist>> = runCatching {
    val root = JSONObject(json)
    val songs = root.getJSONArray("songs").objects().map {
        Song(it.getInt("id"), "", "", "", 0, 0, it.optBoolean("liked"), it.optInt("plays"))
    }
    val playlists = root.getJSONArray("playlists").objects().map {
        Playlist(it.getInt("id"), it.getString("name"), it.getJSONArray("songs").ints(), it.optInt("gradient"), it.optString("created"))
    }
    songs to playlists
}.getOrElse { emptyList<Song>() to emptyList() }

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.ints() = (0 until length()).map { getInt(it) }

/** What a song with no artist tag is called, and what the online lookup must not search for. */
internal const val UNKNOWN_ARTIST = "Unknown artist"

/** The same for a song with no album tag. */
internal const val UNKNOWN_ALBUM = "Unknown album"

/** PRD phase 2: an empty title falls back to the file name; missing tags get readable placeholders. */
internal fun toSong(id: Long, title: String?, artist: String?, album: String?, year: Int, durationMs: Long, fileName: String?) = Song(
    id = id.toInt(), // ponytail: MediaStore _ID fits Int on real devices; make Song.id a Long if one ever overflows
    title = title.known() ?: fileName?.substringBeforeLast('.').known() ?: "Unknown title",
    artist = artist.known() ?: UNKNOWN_ARTIST,
    album = album.known() ?: UNKNOWN_ALBUM,
    year = year,
    duration = (durationMs / 1000).toInt(),
    fileName = fileName.orEmpty(),
)

// MediaStore.UNKNOWN_STRING, spelled out so this logic needs nothing from MediaStore itself.
private const val MEDIASTORE_UNKNOWN = "<unknown>"

private fun String?.known() = takeUnless { it.isNullOrBlank() || it == MEDIASTORE_UNKNOWN }

/** A rescan builds fresh Songs; carry likes and play counts over by id. */
internal fun keepUserState(fresh: List<Song>, old: List<Song>): List<Song> {
    val byId = old.associateBy { it.id }
    return fresh.map { s -> byId[s.id]?.let { s.copy(liked = it.liked, playCount = it.playCount) } ?: s }
}

/** Called by the player on every track change; the song may already be gone after a rescan. */
internal fun MutableList<Song>.countPlay(id: Int) {
    val i = indexOfFirst { it.id == id }
    if (i >= 0) this[i] = this[i].copy(playCount = this[i].playCount + 1)
}
