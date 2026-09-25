package com.rfaizm.harmoniamusic.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.edit

/**
 * The Settings tab's switches, kept in SharedPreferences. Values are mirrored into Compose state so
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

    private var prefs: SharedPreferences? = null
    private val values = mutableStateMapOf<Key, Boolean>()

    /** Called by both the activity and the service, whichever starts first; the second call does nothing. */
    fun init(context: Context) {
        if (prefs != null) return
        val stored = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs = stored
        Key.entries.forEach { values[it] = stored.getBoolean(it.name, it.default) }
    }

    operator fun get(key: Key) = values[key] ?: key.default

    operator fun set(key: Key, value: Boolean) {
        values[key] = value
        prefs?.edit { putBoolean(key.name, value) }
    }
}
