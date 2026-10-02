package com.rfaizm.harmoniamusic.data

import android.content.Context
import androidx.core.net.toUri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** `Song.mp3` keeps its name and swaps the extension, which is how `.lrc` files are normally saved. */
internal fun lrcNameFor(fileName: String): String? {
    if (fileName.isBlank()) return null
    return fileName.substringBeforeLast('.') + ".lrc"
}

/**
 * Lyrics from a `.lrc` file sitting in the folder the user granted (PRD phase 9's "Lirik Lokal"). Scoped storage
 * hides non-media files, so nothing here works until that one-time grant exists, and nothing leaves the phone.
 */
suspend fun readLrc(context: Context, song: Song): String? {
    val folder = Settings.lyricsFolder ?: return null
    val wanted = lrcNameFor(song.fileName) ?: return null
    return withContext(Dispatchers.IO) {
        runCatching { findAndRead(context, folder.toUri(), wanted) }.getOrNull()
    }
}

private fun findAndRead(context: Context, folder: android.net.Uri, wanted: String): String? {
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
    val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    val id = context.contentResolver.query(children, columns, null, null, null)?.use { row ->
        generateSequence { if (row.moveToNext()) row.getString(0) to row.getString(1) else null }
            .firstOrNull { (_, name) -> name.equals(wanted, ignoreCase = true) }
            ?.first
    } ?: return null
    val file = DocumentsContract.buildDocumentUriUsingTree(folder, id)
    val text = context.contentResolver.openInputStream(file)?.use { it.bufferedReader().readText() } ?: return null
    // Kept with its timestamps, so the player can follow the song (T39).
    return text.ifBlank { null }
}
