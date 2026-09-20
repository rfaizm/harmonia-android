package com.rfaizm.harmoniamusic.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Roughly the size art is shown at in the full player; rows scale the same bitmap down. */
private const val ART_SIZE = 512

/**
 * Embedded album art, decoded once per song and kept in memory (PRD phase 2). Songs without art, or whose art
 * can't be decoded, are remembered as such so scrolling doesn't reopen the same file over and over; the UI
 * falls back to the song's gradient.
 */
object AlbumArt {
    // An eighth of the heap, in KB, so a low-end device caches proportionally less (PRD phase 5).
    private val cache = object : LruCache<Int, Bitmap>((Runtime.getRuntime().maxMemory() / 8 / 1024).toInt()) {
        override fun sizeOf(key: Int, value: Bitmap) = value.byteCount / 1024
    }
    private val withoutArt = mutableSetOf<Int>()

    operator fun get(songId: Int): Bitmap? = cache.get(songId)

    /** Blocking: call from [Dispatchers.IO]. Returns null when the song has no art worth showing. */
    fun load(context: Context, songId: Int): Bitmap? {
        cache.get(songId)?.let { return it }
        if (songId in withoutArt) return null
        // OutOfMemory on a huge cover shouldn't take the app down; the gradient is a fine fallback.
        val art = runCatching { decode(context, songId) }.getOrNull()
        if (art == null) withoutArt += songId else cache.put(songId, art)
        return art
    }

    private fun decode(context: Context, songId: Int): Bitmap? {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId.toLong())
        if (Build.VERSION.SDK_INT >= 29) {
            return context.contentResolver.loadThumbnail(uri, Size(ART_SIZE, ART_SIZE), null)
        }
        val bytes = MediaMetadataRetriever().use { it.setDataSource(context, uri); it.embeddedPicture } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, ART_SIZE) }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    /** MediaMetadataRetriever only became Closeable in API 29. */
    private inline fun <T> MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> T): T =
        try { block(this) } finally { release() }
}

/** Largest power-of-two shrink that still covers [target] on the shorter side, so art never decodes blurry. */
internal fun sampleSizeFor(width: Int, height: Int, target: Int): Int {
    var sample = 1
    while (minOf(width, height) / (sample * 2) >= target) sample *= 2
    return sample
}

/** The song's art once it has been read from the file, or null while loading and when there is none. */
@Composable
fun albumArtOf(songId: Int?): ImageBitmap? {
    if (songId == null) return null
    val context = LocalContext.current
    var art by remember(songId) { mutableStateOf(AlbumArt[songId]?.asImageBitmap()) }
    LaunchedEffect(songId) {
        if (art == null) art = withContext(Dispatchers.IO) { AlbumArt.load(context, songId) }?.asImageBitmap()
    }
    return art
}
