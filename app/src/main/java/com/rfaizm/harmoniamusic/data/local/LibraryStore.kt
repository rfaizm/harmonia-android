package com.rfaizm.harmoniamusic.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** `library.json`: likes, play counts and playlists, the only things a rescan can't rebuild (T10). */
@Singleton
class LibraryStore @Inject constructor(@ApplicationContext context: Context) {
    private val file = File(context.filesDir, "library.json")
    private val writeLock = Mutex()

    /** Null when nothing has been saved yet, or the file can't be read. */
    suspend fun read(): String? = withContext(Dispatchers.IO) { runCatching { file.readText() }.getOrNull() }

    /** One write at a time, so a quick run of saves can't interleave and leave a torn file. */
    suspend fun write(json: String) {
        writeLock.withLock { withContext(Dispatchers.IO) { runCatching { file.writeText(json) } } }
    }
}
