package com.rfaizm.harmoniamusic.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The queue, the song and the position playback stopped at, so a media button can resume it later (T12). */
data class Session(val ids: List<Int>, val index: Int, val positionMs: Long)

@Singleton
class SessionStore @Inject constructor(@ApplicationContext context: Context) {
    // Its own file, so writing the position doesn't wake the playback service's settings listener.
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun save(session: Session) = prefs.edit {
        putString("ids", session.ids.joinToString(","))
        putInt("index", session.index)
        putLong("position", session.positionMs)
    }

    /** Null when nothing was ever played. */
    fun load(): Session? {
        val ids = prefs.getString("ids", "").orEmpty().split(",").mapNotNull(String::toIntOrNull)
        if (ids.isEmpty()) return null
        return Session(ids, prefs.getInt("index", 0), prefs.getLong("position", 0))
    }
}
