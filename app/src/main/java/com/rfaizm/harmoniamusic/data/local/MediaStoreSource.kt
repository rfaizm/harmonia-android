package com.rfaizm.harmoniamusic.data.local

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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

    /** True once the song is gone; a row that was already deleted elsewhere counts as gone. */
    fun delete(songId: Int): Boolean = runCatching { context.contentResolver.delete(songUri(songId), null, null) }.isSuccess

    /** Android 11+: the intent that makes the system show its own delete confirmation, or null if it can't be built. */
    fun deleteRequest(songId: Int): IntentSender? {
        if (Build.VERSION.SDK_INT < 30) return null
        return runCatching { MediaStore.createDeleteRequest(context.contentResolver, listOf(songUri(songId))).intentSender }.getOrNull()
    }

    /** Android 10: null when the song is already gone, otherwise the consent the user has to grant first. */
    fun deleteOrConsent(songId: Int): IntentSender? {
        if (Build.VERSION.SDK_INT < 29) return null
        return try {
            context.contentResolver.delete(songUri(songId), null, null)
            null
        } catch (e: RecoverableSecurityException) {
            e.userAction.actionIntent.intentSender
        } // any other SecurityException is a real failure and is reported by the caller
    }

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

/** Where the player and the delete calls find a song's audio. */
fun songUri(songId: Int): Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId.toLong())
