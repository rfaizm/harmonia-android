package com.rfaizm.harmoniamusic.data

import org.json.JSONArray
import org.json.JSONObject


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

/** Saved ids minus whatever a rescan removed, with the index still pointing at the song that was playing. */
internal fun resumeQueue(ids: List<Int>, index: Int, songs: List<Song>): Pair<List<Song>, Int> {
    val byId = songs.associateBy { it.id }
    val queue = ids.mapNotNull { byId[it] }
    val playing = ids.getOrNull(index)
    return queue to queue.indexOfFirst { it.id == playing }.coerceAtLeast(0)
}
