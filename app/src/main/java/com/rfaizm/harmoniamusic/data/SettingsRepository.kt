package com.rfaizm.harmoniamusic.data

import android.net.Uri
import com.rfaizm.harmoniamusic.data.local.LrcFolderSource
import com.rfaizm.harmoniamusic.data.local.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Settings tab's switches, the `.lrc` folder grant and each song's lyrics timing (M6, T32). Screens collect
 * [flags] and [lyricsFolder] (T33); [onChange] tells the playback service when to apply a switch while playing.
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

    private val _flags = MutableStateFlow(Key.entries.associateWith { store.flag(it.name, it.default) })
    private val _lyricsFolder = MutableStateFlow(store.lyricsFolder)

    /** Every switch, always holding all of [Key]. */
    val flags: StateFlow<Map<Key, Boolean>> = _flags.asStateFlow()

    /** The folder the user granted for `.lrc` files (T29), or null while none is granted. */
    val lyricsFolder: StateFlow<String?> = _lyricsFolder.asStateFlow()

    /** The switch as it is now, for code that reads it once rather than following it. */
    operator fun get(key: Key) = _flags.value[key] ?: key.default

    operator fun set(key: Key, value: Boolean) {
        _flags.update { it + (key to value) }
        store.setFlag(key.name, value)
    }

    /** Keeps read access to the picked folder across restarts, then remembers it. */
    fun grantLyricsFolder(uri: Uri) {
        lrcFolder.keepAccess(uri)
        _lyricsFolder.value = uri.toString()
        store.lyricsFolder = uri.toString()
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
