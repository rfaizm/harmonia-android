package com.rfaizm.harmoniamusic.data

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rfaizm.harmoniamusic.data.local.LrcFolderSource
import com.rfaizm.harmoniamusic.data.local.SettingsStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Settings tab's switches, the `.lrc` folder grant and each song's lyrics timing (M6, T32). Values are mirrored
 * into Compose state so screens recompose; [onChange] tells the playback service when to apply them while playing.
 */
@Singleton
class SettingsRepository @Inject constructor(private val store: SettingsStore, private val lrcFolder: LrcFolderSource) {
    /** The name is the storage key, so don't rename one without migrating it. */
    enum class Key(val default: Boolean) {
        DarkMode(true),
        Ducking(true),
        PauseOnUnplug(true),
        SmartShuffle(true),   // used by T14
        LiteMode(false),      // used by T19
        LockPrivacy(false),   // used by T15
        OnlineLyrics(false),  // the only setting that lets the app touch the network
    }

    private val values = mutableStateMapOf<Key, Boolean>().apply { Key.entries.forEach { put(it, store.flag(it.name, it.default)) } }
    private var folder by mutableStateOf(store.lyricsFolder)

    operator fun get(key: Key) = values[key] ?: key.default

    operator fun set(key: Key, value: Boolean) {
        values[key] = value
        store.setFlag(key.name, value)
    }

    /** The folder the user granted for `.lrc` files (T29), or null while none is granted. */
    val lyricsFolder: String? get() = folder

    /** Keeps read access to the picked folder across restarts, then remembers it. */
    fun grantLyricsFolder(uri: Uri) {
        lrcFolder.keepAccess(uri)
        folder = uri.toString()
        store.lyricsFolder = folder
    }

    /**
     * How far the user moved one song's synced lyrics, in ms; positive shows them later. LRCLIB's copies of a song
     * disagree by seconds and a video rip can have a longer intro, so no automatic pick fits every file.
     */
    fun lyricsShift(songId: Int): Int = store.lyricsShift(songId)

    fun setLyricsShift(songId: Int, ms: Int) = store.setLyricsShift(songId, ms)

    /** Calls [listener] after any switch changes; the returned function stops it. Lyrics timing doesn't count. */
    fun onChange(listener: () -> Unit): () -> Unit = store.onChange(listener)
}
