package com.rfaizm.harmoniamusic.data

data class Song(
    val id: Int,
    val title: String,   // raw tag, cleaned on render
    val artist: String,  // raw tag, cleaned on render
    val album: String,
    val year: Int,
    val duration: Int,   // seconds
    val liked: Boolean = false,
    val playCount: Int = 0,
    /** As MediaStore names it, so a `.lrc` beside the song can be found (T29). Empty for the preview seeds. */
    val fileName: String = "",
) {
    // Cleaned once per instance, not on every recomposition.
    val displayTitle = cleanTag(title)
    val displayArtist = cleanTag(artist)
}

data class Playlist(
    val id: Int,
    val name: String,
    val songIds: List<Int>,
    val gradientIndex: Int,
    val createdAt: String, // YYYY-MM-DD
) {
}

/** Adds [ids] to the playlist named [name] (case-insensitive), creating it if missing. An existing name merges and skips duplicates (PRD phase 3). */
fun MutableList<Playlist>.addSongs(name: String, ids: Collection<Int>, today: String) {
    val i = indexOfFirst { it.name.equals(name, ignoreCase = true) }
    if (i >= 0) this[i] = this[i].copy(songIds = (this[i].songIds + ids).distinct())
    else add(Playlist((maxOfOrNull { it.id } ?: 0) + 1, name, ids.distinct(), size + 3, today))
}

/** After the file is deleted the song shouldn't linger anywhere (T16). */
fun MutableList<Playlist>.removeSongEverywhere(songId: Int) {
    forEachIndexed { i, p -> if (songId in p.songIds) this[i] = p.copy(songIds = p.songIds - songId) }
}

fun MutableList<Playlist>.removeSong(playlistId: Int, songId: Int) {
    val i = indexOfFirst { it.id == playlistId }
    if (i >= 0) this[i] = this[i].copy(songIds = this[i].songIds - songId)
}

private val BRACKETS = Regex("""[\[(].*?[\])]""")
private val CREDITS = Regex("""(?i)\b(feat|ft|prod|vs)\b\.?\s+\S+""")
private val JUNK = Regex("""(?i)\by2mate(\.com)?\b""")
private val DASHES = Regex("[-_]+")
private val SPACES = Regex("""\s{2,}""")

fun cleanTag(s: String) = s
    .replace(BRACKETS, "")
    .replace(CREDITS, "")
    .replace(JUNK, "")
    .replace(DASHES, " ")
    .replace(SPACES, " ")
    .trim()
