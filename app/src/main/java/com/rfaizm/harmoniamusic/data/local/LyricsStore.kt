package com.rfaizm.harmoniamusic.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lyrics found online, kept on the phone so a song needs the network at most once and still shows them offline.
 * ponytail: one flat JSON file like library.json; move it into that file if a third store ever appears.
 */
@Singleton
class LyricsStore @Inject constructor(@ApplicationContext context: Context) {
    private val dir = context.filesDir
    private val file = File(dir, "lyrics-v3.json")
    private val byId = mutableMapOf<Int, String>()
    private val lock = Mutex()
    private var loaded = false

    suspend fun get(songId: Int): String? = lock.withLock {
        load()
        byId[songId]
    }

    suspend fun put(songId: Int, lyrics: String) = lock.withLock {
        load()
        byId[songId] = lyrics
        val json = JSONObject(byId.mapKeys { it.key.toString() }).toString()
        withContext(Dispatchers.IO) { runCatching { file.writeText(json) } }
        Unit
    }

    private suspend fun load() {
        if (loaded) return
        loaded = true
        val text = withContext(Dispatchers.IO) {
            // Older files kept plain text, or whichever synced copy came first, so those songs are looked up again.
            listOf("lyrics.json", "lyrics-synced.json").forEach { File(dir, it).delete() }
            runCatching { file.readText() }.getOrNull()
        } ?: return
        runCatching {
            val json = JSONObject(text)
            json.keys().forEach { key -> key.toIntOrNull()?.let { byId[it] = json.getString(key) } }
        }
    }
}
