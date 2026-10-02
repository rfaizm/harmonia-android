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

/** One line of synced lyrics and the moment it is sung. A blank [text] is a pause in the singing. */
data class LyricLine(val timeMs: Long, val text: String)

private val STAMP = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")
private val LEADING_STAMPS = Regex("""^(?:\[\d+:\d{1,2}(?:[.:]\d{1,3})?]\s*)+""")
private val WORD_STAMP = Regex("""<\d+:\d{1,2}(?:[.:]\d{1,3})?>""")
private val OFFSET = Regex("""^\[offset:\s*([+-]?\d+)\s*]$""", RegexOption.IGNORE_CASE)

/**
 * The timed lines of LRC text (`[01:02.50]line`), in the order they are sung, or nothing when the text has no times,
 * which is how plain lyrics are told apart. Header lines like `[ar:...]` are skipped, `[offset:...]` is applied, and
 * enhanced LRC's per-word marks are removed, since the player highlights whole lines.
 */
internal fun parseLrc(text: String): List<LyricLine> {
    val lines = text.lines().map { it.trim() }
    // A positive offset shows every line that many milliseconds earlier.
    val offset = lines.firstNotNullOfOrNull { OFFSET.matchEntire(it)?.groupValues?.get(1)?.toLongOrNull() } ?: 0
    return lines.flatMap { line ->
        val stamps = LEADING_STAMPS.find(line)?.value ?: return@flatMap emptyList()
        val words = line.substring(stamps.length).replace(WORD_STAMP, "").trim()
        STAMP.findAll(stamps).map { LyricLine((millisOf(it) - offset).coerceAtLeast(0), words) }.toList()
    }.sortedBy { it.timeMs }
}

private fun millisOf(stamp: MatchResult): Long {
    val (minutes, seconds, fraction) = stamp.destructured
    // ".5" is half a second and ".05" five hundredths, so the fraction is read as thousandths after padding.
    return minutes.toLong() * 60_000 + seconds.toLong() * 1_000 + fraction.padEnd(3, '0').toLong()
}

/** The line being sung at [positionMs]: the last one already reached, or -1 during the intro. */
internal fun currentLine(lines: List<LyricLine>, positionMs: Long): Int = lines.indexOfLast { it.timeMs <= positionMs }

/** Where this line is sung once the song's Earlier/Later shift is applied, which is where a tap on it plays from. */
internal fun LyricLine.startsAt(shiftMs: Int): Long = (timeMs + shiftMs).coerceAtLeast(0)
