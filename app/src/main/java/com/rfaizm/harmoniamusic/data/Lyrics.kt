package com.rfaizm.harmoniamusic.data

/**
 * Lyrics out of an ID3 USLT ("unsynchronised lyrics") frame, which Media3 hands over as raw bytes because it has
 * no frame type of its own for it (PRD phase 9).
 *
 * The payload is an encoding byte, a 3-letter language code, a description terminated by a null, and then the
 * lyrics themselves. Anything unreadable returns null and the player simply says the file has none.
 */
internal fun parseUslt(payload: ByteArray): String? {
    if (payload.size < 6) return null
    val charset = when (payload[0].toInt()) {
        0 -> Charsets.ISO_8859_1
        1 -> Charsets.UTF_16
        2 -> Charsets.UTF_16BE
        3 -> Charsets.UTF_8
        else -> return null
    }
    // UTF-16 counts in pairs, so its terminator is two nulls and the description is walked two bytes at a time.
    val wide = charset == Charsets.UTF_16 || charset == Charsets.UTF_16BE
    val step = if (wide) 2 else 1
    var end = 4 // past the encoding byte and the language
    while (end < payload.size) {
        val terminator = payload[end] == 0.toByte() && (!wide || payload.getOrNull(end + 1) == 0.toByte())
        if (terminator) break
        end += step
    }
    val start = end + step
    if (start >= payload.size) return null
    return String(payload, start, payload.size - start, charset).trim().ifEmpty { null }
}

/** Turns `[00:12.00]Line` into `Line`, and drops the `[ar:...]` style header lines of an LRC file. */
internal fun stripLrcTimestamps(text: String): String = text.lineSequence()
    .map { it.replace(Regex("""^(\[\d+:\d+(?:[.:]\d+)?])+"""), "").trim() }
    .filterNot { it.isEmpty() || it.matches(Regex("""^\[[a-zA-Z]+:.*]$""")) }
    .joinToString("\n")
