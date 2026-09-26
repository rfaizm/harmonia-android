package com.rfaizm.harmoniamusic.data

import android.content.Context
import com.rfaizm.harmoniamusic.data.remote.ApiConfig
import com.rfaizm.harmoniamusic.data.remote.dto.LyricsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** What one lookup came back with, so the player can say which of these happened rather than just "nothing". */
sealed interface LyricsResult {
    data class Found(val text: String) : LyricsResult

    /** The service answered, and has nothing for this song. */
    data object NotFound : LyricsResult

    /** The service couldn't be reached at all. */
    data object Offline : LyricsResult
}

/**
 * Looks lyrics up through [ApiConfig]'s service. Only ever called when the user taps the button with the setting
 * switched on, and sends nothing but this one song's tags (PRD phase 9 asks for local lyrics; this is the extra).
 */
suspend fun fetchLyrics(song: Song): LyricsResult = withContext(Dispatchers.IO) {
    val service = ApiConfig.lyricsService
    val artist = artistQuery(song)
    try {
        // The exact endpoint needs a real artist, so a tagless file goes straight to the search.
        if (artist != null) {
            val exact = service.getLyrics(artist, song.displayTitle, song.album, song.duration).body()
            wordsIn(exact)?.let { return@withContext LyricsResult.Found(it) }
        }
        val results = service.searchLyrics(song.displayTitle, artist).body().orEmpty()
        lyricsIn(results)?.let { LyricsResult.Found(it) } ?: LyricsResult.NotFound
    } catch (e: IOException) {
        LyricsResult.Offline // no connection, timed out, dns failed
    } catch (e: Exception) {
        LyricsResult.NotFound // an answer we couldn't read is no better than no answer
    }
}

/** Null when the file has no artist tag: searching for an artist called "Unknown artist" finds the wrong songs. */
internal fun artistQuery(song: Song) = song.displayArtist.takeIf { it != UNKNOWN_ARTIST }

/** A search answers with many entries and most carry no lyrics, so this takes the first that actually does. */
internal fun lyricsIn(results: List<LyricsDto>): String? = results.firstNotNullOfOrNull { wordsIn(it) }

/** Plain lyrics if the entry has them, otherwise the synced ones with their timestamps taken off. */
internal fun wordsIn(entry: LyricsDto?): String? {
    if (entry == null || entry.instrumental) return null
    val plain = entry.plainLyrics?.trim().orEmpty()
    return plain.ifEmpty { stripLrcTimestamps(entry.syncedLyrics.orEmpty()) }.ifEmpty { null }
}

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
