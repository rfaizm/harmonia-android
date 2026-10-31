package com.rfaizm.harmoniamusic.data.local

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Where the Settings tab's switches, the `.lrc` folder grant and each song's lyrics timing are kept. */
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext context: Context) {
    // The playback service watches this file to apply changes while playing.
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    // Its own file, because the service re-applies its settings on every change to the main one (M8).
    private val timing = context.getSharedPreferences("lyrics_timing", Context.MODE_PRIVATE)

    /** Android's own low-RAM flag (usually 1 GB or less), which turns light mode on by default (T19). */
    val lowRam = context.getSystemService(ActivityManager::class.java).isLowRamDevice

    fun flag(name: String, default: Boolean) = prefs.getBoolean(name, default)

    fun setFlag(name: String, value: Boolean) = prefs.edit { putBoolean(name, value) }

    /** The folder the user granted for `.lrc` files (T29), or null while none is granted. */
    var lyricsFolder: String?
        get() = prefs.getString(FOLDER_KEY, null)
        set(value) = prefs.edit { if (value == null) remove(FOLDER_KEY) else putString(FOLDER_KEY, value) }

    fun lyricsShift(songId: Int): Int = timing.getInt(songId.toString(), 0)

    fun setLyricsShift(songId: Int, ms: Int) = timing.edit { if (ms == 0) remove(songId.toString()) else putInt(songId.toString(), ms) }

    /** SharedPreferences only holds its listeners weakly, so the returned function keeps this one alive. */
    fun onChange(listener: () -> Unit): () -> Unit {
        val watcher = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> listener() }
        prefs.registerOnSharedPreferenceChangeListener(watcher)
        return { prefs.unregisterOnSharedPreferenceChangeListener(watcher) }
    }

    private companion object {
        const val FOLDER_KEY = "lyricsFolder"
    }
}
