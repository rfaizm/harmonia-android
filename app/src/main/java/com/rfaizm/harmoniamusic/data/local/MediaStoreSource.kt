package com.rfaizm.harmoniamusic.data.local

import android.content.Context
import android.provider.MediaStore.Audio.Media
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.toSong
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** PRD phase 1: the phone's music as MediaStore lists it. MediaStore already has the tags, so no file is opened. */
@Singleton
class MediaStoreSource @Inject constructor(@ApplicationContext private val context: Context) {
    /** Blocking: call from IO. Null without the audio permission, where the query throws. */
    fun songs(): List<Song>? = runCatching { query() }.getOrNull()

    private fun query(): List<Song> {
        val columns = arrayOf(Media._ID, Media.TITLE, Media.ARTIST, Media.ALBUM, Media.YEAR, Media.DURATION, Media.DISPLAY_NAME)
        // Settings › Filters: "Hide clips shorter than 30 seconds".
        val selection = "${Media.IS_MUSIC} != 0 AND ${Media.DURATION} >= 30000"
        return context.contentResolver.query(Media.EXTERNAL_CONTENT_URI, columns, selection, null, null)?.use { c ->
            buildList(c.count) {
                while (c.moveToNext()) {
                    add(toSong(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4), c.getLong(5), c.getString(6)))
                }
            }
        }.orEmpty()
    }
}
