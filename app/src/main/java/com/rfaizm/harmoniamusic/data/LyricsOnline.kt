package com.rfaizm.harmoniamusic.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val API = "https://lrclib.net/api"
private const val USER_AGENT = "Harmonia/1.0 (offline music player; com.rfaizm.harmoniamusic)"
private const val TIMEOUT_MS = 10_000

/** What one lookup came back with, so the player can say which of these happened rather than just "nothing". */
sealed interface LyricsResult {
    data class Found(val text: String) : LyricsResult

    /** The service answered, and has nothing for this song. */
    data object NotFound : LyricsResult

    /** The service couldn't be reached at all. */
    data object Offline : LyricsResult
}

/**
 * Looks lyrics up at lrclib.net. Only ever called when the user taps the button with the setting switched on, and
 * sends nothing but this one song's tags (PRD phase 9 asks for local lyrics; this is the opt-in extra).
 */
suspend fun fetchLyrics(song: Song): LyricsResult = withContext(Dispatchers.IO) {
    // The exact endpoint matches on album and length; the search is the looser second try.
    for (url in listOf(lyricsUrl(song), searchUrl(song))) {
        val body = read(url).getOrElse { return@withContext LyricsResult.Offline }
        lyricsFrom(body.orEmpty())?.let { return@withContext LyricsResult.Found(it) }
    }
    LyricsResult.NotFound
}

/** The body of a 200, null for any other status, and a failure only when the service couldn't be reached. */
private fun read(url: String): Result<String?> = runCatching {
    (URL(url).openConnection() as HttpURLConnection).run {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        setRequestProperty("User-Agent", USER_AGENT)
        try {
            // A song it doesn't know answers 503 as readily as 404, so anything but 200 counts as "no lyrics".
            if (responseCode == HttpURLConnection.HTTP_OK) inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            disconnect()
        }
    }
}

internal fun lyricsUrl(song: Song) = "$API/get?artist_name=${esc(song.displayArtist)}&track_name=${esc(song.displayTitle)}" +
    "&album_name=${esc(song.album)}&duration=${song.duration}"

internal fun searchUrl(song: Song) = "$API/search?artist_name=${esc(song.displayArtist)}&track_name=${esc(song.displayTitle)}"

private fun esc(value: String): String = URLEncoder.encode(value, "UTF-8")

/** Plain lyrics if the answer has them, otherwise the synced ones with their timestamps taken off. */
internal fun lyricsFrom(json: String): String? = runCatching {
    val answer = when (val parsed = JSONTokener(json).nextValue()) {
        is JSONArray -> if (parsed.length() > 0) parsed.getJSONObject(0) else return null
        is JSONObject -> parsed
        else -> return null
    }
    if (answer.optBoolean("instrumental")) return null
    answer.optString("plainLyrics").trim().ifEmpty { stripLrcTimestamps(answer.optString("syncedLyrics")) }.ifEmpty { null }
}.getOrNull()

/** Turns `[00:12.00]Line` into `Line`, and drops the `[ar:...]` style header lines of an LRC file. */
internal fun stripLrcTimestamps(text: String): String = text.lineSequence()
    .map { it.replace(Regex("""^(\[\d+:\d+(?:[.:]\d+)?])+"""), "").trim() }
    .filterNot { it.isEmpty() || it.matches(Regex("""^\[[a-zA-Z]+:.*]$""")) }
    .joinToString("\n")

/**
 * Lyrics found online, kept on the phone so a song needs the network at most once and still shows them offline.
 * ponytail: one flat JSON file like library.json; move it into that file if a third store ever appears.
 */
object LyricsCache {
    private val byId = mutableMapOf<Int, String>()
    private val lock = Mutex()
    private var loaded = false

    suspend fun get(context: Context, songId: Int): String? = lock.withLock {
        load(context)
        byId[songId]
    }

    suspend fun put(context: Context, songId: Int, lyrics: String) = lock.withLock {
        load(context)
        byId[songId] = lyrics
        val json = JSONObject(byId.mapKeys { it.key.toString() }).toString()
        withContext(Dispatchers.IO) { runCatching { file(context).writeText(json) } }
        Unit
    }

    private suspend fun load(context: Context) {
        if (loaded) return
        loaded = true
        val text = withContext(Dispatchers.IO) { runCatching { file(context).readText() }.getOrNull() } ?: return
        runCatching {
            val json = JSONObject(text)
            json.keys().forEach { key -> key.toIntOrNull()?.let { byId[it] = json.getString(key) } }
        }
    }

    private fun file(context: Context) = File(context.filesDir, "lyrics.json")
}
