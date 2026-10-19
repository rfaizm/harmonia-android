package com.rfaizm.harmoniamusic.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rfaizm.harmoniamusic.data.local.SettingsStore

/**
 * The Settings tab's switches, kept by [SettingsStore]. Values are mirrored into Compose state so
 * screens recompose, and the playback service watches the same file to pick up changes while playing.
 */
object Settings {
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

    internal const val FILE = "settings"

    private var store: SettingsStore? = null
    private val values = mutableStateMapOf<Key, Boolean>()
    private var folder by mutableStateOf<String?>(null)

    /** Called by both the activity and the service, whichever starts first; the second call does nothing. */
    fun init(context: Context) {
        if (store != null) return
        val stored = context.dataSources().settingsStore()
        store = stored
        Key.entries.forEach { values[it] = stored.flag(it.name, it.default) }
        folder = stored.lyricsFolder
    }

    /**
     * How far the user moved one song's synced lyrics, in ms; positive shows them later. LRCLIB's copies of a song
     * disagree by seconds and a video rip can have a longer intro, so no automatic pick fits every file. Kept in its
     * own file, because the playback service re-applies its settings on every change to the main one.
     */
    fun lyricsShift(songId: Int): Int = store?.lyricsShift(songId) ?: 0

    fun setLyricsShift(songId: Int, ms: Int) {
        store?.setLyricsShift(songId, ms)
    }

    /** The folder the user granted for `.lrc` files (T29), or null while none is granted. */
    var lyricsFolder: String?
        get() = folder
        set(value) {
            folder = value
            store?.lyricsFolder = value
        }

    operator fun get(key: Key) = values[key] ?: key.default

    operator fun set(key: Key, value: Boolean) {
        values[key] = value
        store?.setFlag(key.name, value)
    }
}
