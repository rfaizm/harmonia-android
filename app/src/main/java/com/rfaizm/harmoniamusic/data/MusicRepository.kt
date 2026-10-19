package com.rfaizm.harmoniamusic.data

import android.content.IntentSender
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rfaizm.harmoniamusic.data.local.ArtSource
import com.rfaizm.harmoniamusic.data.local.LibraryStore
import com.rfaizm.harmoniamusic.data.local.MediaStoreSource
import com.rfaizm.harmoniamusic.data.local.Session
import com.rfaizm.harmoniamusic.data.local.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The library (M6, T32, formerly `object Library`): songs, playlists, likes and play counts, the scan, album art,
 * deleting a file, and the session a media button resumes. A singleton for the whole process, so rotation doesn't
 * rescan and the playback service sees the same songs as the screens.
 */
@Singleton
class MusicRepository @Inject constructor(
    private val mediaStore: MediaStoreSource,
    private val store: LibraryStore,
    private val art: ArtSource,
    private val sessions: SessionStore,
) {
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
    suspend fun scan() = loadLock.withLock { scanNow() }

    /** Restores and scans once, whoever asks first; a second caller waits for that scan instead of repeating it. */
    suspend fun ensureLoaded() = loadLock.withLock {
        restore()
        if (!loaded) scanNow()
    }

    private suspend fun scanNow() {
        // Without the audio permission the query fails; leave [loaded] false so a later grant still scans.
        val fresh = withContext(Dispatchers.IO) { mediaStore.songs() } ?: return
        // Nothing scanned yet means this is the first scan of the process, so the saved state is the newest.
        val merged = keepUserState(fresh, songs.toList().ifEmpty { saved })
        songs.clear()
        songs.addAll(merged)
        loaded = true
    }

    /** Reads what MediaStore can't tell us, once, before the first scan. */
    private suspend fun restore() {
        if (restored) return
        restored = true
        val text = store.read() ?: return
        val (savedSongs, savedPlaylists) = parseState(text)
        saved = savedSongs
        playlists.clear()
        playlists.addAll(savedPlaylists)
    }

    /** Call after every like or playlist change. Snapshots on the caller's thread, writes on IO. */
    fun save() {
        val json = stateJson(songs.toList(), playlists.toList())
        writes.launch { store.write(json) }
    }

    /** Counted by the service, so tracks that advance in the background count too. */
    fun countPlay(songId: Int) {
        songs.countPlay(songId)
        save()
    }

    // --- Album art (PRD phase 2): decoded once per song and kept in memory.

    fun cachedArt(songId: Int): Bitmap? = art[songId]

    /** Blocking: call from IO. Null when the song has no art worth showing. */
    fun loadArt(songId: Int): Bitmap? = art.load(songId)

    // --- Deleting a file (T16). The UI picks the path for the Android version and asks the user; these do the work.

    /** True once the file is gone; a row already deleted elsewhere counts as gone. Then drop it with [remove]. */
    fun delete(songId: Int): Boolean = mediaStore.delete(songId)

    /** Android 11+: the system's own confirmation, which deletes the file itself; null if it can't be built. */
    fun deleteRequest(songId: Int): IntentSender? = mediaStore.deleteRequest(songId)

    /** Android 10: null once deleted, otherwise the consent to ask for first. Throws on any other refusal. */
    fun deleteOrConsent(songId: Int): IntentSender? = mediaStore.deleteOrConsent(songId)

    /** The song's file is gone, so drop it from the library and every playlist. */
    fun remove(songId: Int) {
        songs.removeAll { it.id == songId }
        playlists.removeSongEverywhere(songId)
        save()
    }

    // --- The session a media button resumes after the app was closed (T12).

    fun saveSession(ids: List<Int>, index: Int, positionMs: Long) = sessions.save(Session(ids, index, positionMs))

    /** Waits for the library, since a media button can start the service before anything has scanned. */
    suspend fun lastSession(): SavedQueue? {
        val session = sessions.load() ?: return null
        ensureLoaded()
        val (queue, index) = resumeQueue(session.ids, session.index, songs.toList())
        return if (queue.isEmpty()) null else SavedQueue(queue, index, session.positionMs)
    }
}

/** What to resume: the saved queue with songs deleted since dropped, and where to start. */
data class SavedQueue(val songs: List<Song>, val index: Int, val positionMs: Long)
