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
import kotlin.math.abs

/** What one lookup came back with, so the player can say which of these happened rather than just "nothing". */
sealed interface LyricsResult {
    data class Found(val text: String) : LyricsResult

    /** The service answered, and has nothing for this song. */
    data object NotFound : LyricsResult

    /** The service couldn't be reached at all. */
    data object Offline : LyricsResult
}

/**
 * One way of asking for a song's lyrics. A null [artist] is the title alone, which only a matching length confirms.
 * [fromFileName] marks the two halves of a file name, where either may be the artist, so the artist must match too.
 */
data class LyricsQuery(val artist: String?, val title: String, val fromFileName: Boolean = false)

/**
 * Looks lyrics up through [ApiConfig]'s service. Only ever called when the user taps the button with the setting
 * switched on, and sends nothing but this one song's tags (PRD phase 9 asks for local lyrics; this is the extra).
 * [playerArtist] is what the player read from the file itself, which MediaStore sometimes misses (SPEC.md S2).
 */
suspend fun fetchLyrics(song: Song, playerArtist: String?): LyricsResult = withContext(Dispatchers.IO) {
    val service = ApiConfig.lyricsService
    try {
        for (query in lyricsQueries(song, playerArtist)) {
            // The exact endpoint needs an artist; when there is one it is the most precise answer.
            // The "Unknown album" placeholder is never sent: LRCLIB took it literally and matched a stray upload.
            val album = song.album.takeIf { it != UNKNOWN_ALBUM }
            val exact = query.artist?.let { service.getLyrics(it, query.title, album, song.duration).body() }
                ?.takeIf { matches(it, query, song.duration) }
            syncedIn(exact)?.let { return@withContext LyricsResult.Found(it) }
            // Plain only, or no exact match: the search may still hold a synced copy of the same song.
            val results = service.searchLyrics(query.title, query.artist).body().orEmpty().filter { matches(it, query, song.duration) }
            lyricsIn(listOfNotNull(exact) + results, song.duration)?.let { return@withContext LyricsResult.Found(it) }
        }
        LyricsResult.NotFound
    } catch (e: IOException) {
        LyricsResult.Offline // no connection, timed out, dns failed
    } catch (e: Exception) {
        LyricsResult.NotFound // an answer we couldn't read is no better than no answer
    }
}

/**
 * Who and what to ask for, best first (SPEC.md S1, S2): the song's own tags, then the artist the player read, then
 * what the file name says, then the title alone. Each is tried only if the one before found nothing it could
 * trust, and the "Unknown artist" placeholder is never sent.
 */
internal fun lyricsQueries(song: Song, playerArtist: String?): List<LyricsQuery> {
    val artists = listOfNotNull(
        song.displayArtist.takeIf { it != UNKNOWN_ARTIST },
        playerArtist?.let(::cleanTag)?.takeIf { it.isNotBlank() && it != UNKNOWN_ARTIST },
    )
    val guess = guessFromFileName(song.fileName)
    // With no title tag the title is the file name, and half of a split file name may well be the artist.
    val titleIsFileName = song.fileName.isNotEmpty() && song.title == song.fileName.substringBeforeLast('.')
    return buildList {
        artists.forEach { artist ->
            add(LyricsQuery(artist, titleWithout(artist, song.displayTitle)))
            add(LyricsQuery(artist, song.displayTitle)) // in case the title really starts with the artist's name
        }
        // "Artist - Title" is the usual order, but "Title - Artist" is common too, so both are tried. Some downloaders
        // add the channel after the title ("Harry Styles - Sign of the Times - Harry Styles"), so the artist comes off.
        guess?.let { (first, second) ->
            add(LyricsQuery(first, titleWithout(first, second), fromFileName = true))
            add(LyricsQuery(second, titleWithout(second, first), fromFileName = true))
        }
        if (guess == null || !titleIsFileName) {
            add(LyricsQuery(null, artists.firstOrNull()?.let { titleWithout(it, song.displayTitle) } ?: song.displayTitle))
        }
    }.distinctBy { key(it.artist.orEmpty()) + "|" + key(it.title) }
}

/**
 * Downloaded songs are often titled "Artist - Title" or "Title - Artist" ("Sign of the Times Harry Styles" once
 * cleaned), and the service only knows the bare title. Takes [artist] off either end, but only as whole words.
 */
internal fun titleWithout(artist: String, title: String): String {
    val name = artist.trim()
    val text = title.trim()
    if (name.isEmpty() || text.length <= name.length) return text
    val rest = when {
        text.startsWith(name, ignoreCase = true) && text[name.length].isSeparator() ->
            text.drop(name.length).trimStart { it.isSeparator() }
        text.endsWith(name, ignoreCase = true) && text[text.length - name.length - 1].isSeparator() ->
            text.dropLast(name.length).trimEnd { it.isSeparator() }
        else -> text
    }
    return rest.ifEmpty { text }
}

private fun Char.isSeparator() = isWhitespace() || this in "-–—:|,·"

/**
 * SPEC.md S1: "Coldplay - Yellow.mp3" names its artist and title, as most downloaded files do. The parts are cleaned
 * the way tags are, so a "y2mate.com - " prefix or an "(Official Video)" suffix drops away. A bare track number in
 * front ("01 - Yellow") is not an artist.
 */
internal fun guessFromFileName(fileName: String): Pair<String, String>? {
    val parts = fileName.substringBeforeLast('.')
        .split(" - ", " \u2013 ", " \u2014 ") // hyphen, en dash, em dash
        .map(::cleanTag)
        .filter { it.isNotBlank() }
    if (parts.size < 2) return null
    val artist = parts.first()
    if (artist.all { it.isDigit() || it.isWhitespace() }) return null
    return artist to parts.drop(1).joinToString(" ")
}

/**
 * SPEC.md S3: an entry counts only if it is this very song. Its title must match once cleaned, and then either the
 * artist matches too, or the length is within a few seconds. A live version or a video rip by the right artist
 * passes; a different song that happens to share a title does not. A guess from the file name needs the artist:
 * LRCLIB has uploads with the fields swapped (title "Coldplay", artist "Clocks"), and the length alone let one through.
 */
internal fun matches(entry: LyricsDto, query: LyricsQuery, durationSec: Int): Boolean {
    if (!sameText(entry.trackName, query.title)) return false
    val sameLength = entry.duration?.let { abs(it - durationSec) <= LENGTH_TOLERANCE_SEC } == true
    val sameArtist = query.artist != null && sameText(entry.artistName, query.artist)
    return sameArtist || (sameLength && !query.fromFileName)
}

private const val LENGTH_TOLERANCE_SEC = 3

private fun sameText(a: String?, b: String?) = a != null && b != null && key(a).let { it.isNotEmpty() && it == key(b) }

/** Case, spacing, punctuation and bracketed extras don't make two titles different songs. */
private fun key(text: String) = cleanTag(text).lowercase().filter(Char::isLetterOrDigit)

/**
 * A search answers with many entries and most carry no lyrics. Synced ones win, since they let the player follow the
 * song (T39). LRCLIB keeps many copies of one song whose timings disagree by seconds, so the synced copy closest in
 * length to the file is taken: it's the likeliest to be timed for the same recording. Otherwise the first with any.
 */
internal fun lyricsIn(results: List<LyricsDto>, durationSec: Int): String? =
    results.filter { syncedIn(it) != null }
        .minByOrNull { entry -> entry.duration?.let { abs(it - durationSec) } ?: Double.MAX_VALUE }
        ?.let(::syncedIn)
        ?: results.firstNotNullOfOrNull { wordsIn(it) }

/** Synced lyrics if the entry has them, timestamps and all, otherwise the plain ones. */
internal fun wordsIn(entry: LyricsDto?): String? =
    syncedIn(entry) ?: entry?.takeUnless { it.instrumental }?.plainLyrics?.trim()?.ifEmpty { null }

private fun syncedIn(entry: LyricsDto?): String? =
    entry?.takeUnless { it.instrumental }?.syncedLyrics?.trim()?.ifEmpty { null }

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
        val text = withContext(Dispatchers.IO) {
            // Older files kept plain text, or whichever synced copy came first, so those songs are looked up again.
            listOf("lyrics.json", "lyrics-synced.json").forEach { File(context.filesDir, it).delete() }
            runCatching { file(context).readText() }.getOrNull()
        } ?: return
        runCatching {
            val json = JSONObject(text)
            json.keys().forEach { key -> key.toIntOrNull()?.let { byId[it] = json.getString(key) } }
        }
    }

    private fun file(context: Context) = File(context.filesDir, "lyrics-v3.json")
}
