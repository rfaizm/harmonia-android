package com.rfaizm.harmoniamusic.data.local

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.lrcNameFor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lyrics from a `.lrc` file sitting in the folder the user granted (PRD phase 9's "Lirik Lokal"). Scoped storage
 * hides non-media files, so nothing here works until that one-time grant exists, and nothing leaves the phone.
 */
@Singleton
class LrcFolderSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsStore,
) {
    /** The song's `.lrc`, timestamps and all, so the player can follow the song (T39); null when there is none. */
    suspend fun read(song: Song): String? {
        val folder = settings.lyricsFolder ?: return null
        val wanted = lrcNameFor(song.fileName) ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { findAndRead(folder.toUri(), wanted) }.getOrNull()
        }
    }

    private fun findAndRead(folder: Uri, wanted: String): String? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val id = context.contentResolver.query(children, columns, null, null, null)?.use { row ->
            generateSequence { if (row.moveToNext()) row.getString(0) to row.getString(1) else null }
                .firstOrNull { (_, name) -> name.equals(wanted, ignoreCase = true) }
                ?.first
        } ?: return null
        val file = DocumentsContract.buildDocumentUriUsingTree(folder, id)
        val text = context.contentResolver.openInputStream(file)?.use { it.bufferedReader().readText() } ?: return null
        return text.ifBlank { null }
    }
}
