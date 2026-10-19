package com.rfaizm.harmoniamusic.data

/** `Song.mp3` keeps its name and swaps the extension, which is how `.lrc` files are normally saved. */
internal fun lrcNameFor(fileName: String): String? {
    if (fileName.isBlank()) return null
    return fileName.substringBeforeLast('.') + ".lrc"
}
