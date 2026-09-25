package com.rfaizm.harmoniamusic.data

import androidx.compose.ui.graphics.Color

// GUIDELINE §2 album-art gradients, assigned by index % 12.
val GRADIENTS = listOf(
    Color(0xFF4E7C65) to Color(0xFF2E5A48),
    Color(0xFF3A6878) to Color(0xFF1E4A5C),
    Color(0xFF6A5C7E) to Color(0xFF4A3E5E),
    Color(0xFF5E7050) to Color(0xFF3E5030),
    Color(0xFF7A5E52) to Color(0xFF5A3E32),
    Color(0xFF4A607A) to Color(0xFF2A4060),
    Color(0xFF6E4A62) to Color(0xFF4E2A42),
    Color(0xFF587A68) to Color(0xFF385A48),
    Color(0xFF7A6A4E) to Color(0xFF5A4A2E),
    Color(0xFF486878) to Color(0xFF285060),
    Color(0xFF5A4E7A) to Color(0xFF3A2E5A),
    Color(0xFF688060) to Color(0xFF486040),
)
val LIKED_GRADIENT = Color(0xFFC0392B) to Color(0xFF7A1A2E)

fun gradientFor(index: Int) = GRADIENTS[index.mod(GRADIENTS.size)]

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
    val gradient get() = gradientFor(id)
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
    val gradient get() = gradientFor(gradientIndex)
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

fun formatDuration(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun formatDate(iso: String): String {
    val (y, m, d) = iso.split("-")
    return "${MONTHS[m.toInt() - 1]} ${d.toInt()}, $y"
}

val SEED_SONGS = listOf(
    Song(0, "Morning_Light (Official Audio) [y2mate.com]", "Aurelia Vance", "Quiet Hours", 2023, 214, true, 12),
    Song(1, "Paper Boats", "Linen & Oak", "Tidewater", 2022, 187, playCount = 4),
    Song(2, "Slow River feat. Mira", "Aurelia Vance", "Quiet Hours", 2023, 243, true, 8),
    Song(3, "Cedar Smoke", "The Fern Collective", "Understory", 2021, 199, playCount = 2),
    Song(4, "y2mate.com - Harbour Lights", "Linen & Oak", "Tidewater", 2022, 226, playCount = 6),
    Song(5, "Amber Fields", "Hollow Pines", "Amber", 2020, 265, true, 21),
    Song(6, "Lanterns", "The Fern Collective", "Understory", 2021, 178, playCount = 1),
    Song(7, "Salt_and_Stone", "Marlowe Kei", "Coastline", 2024, 204, playCount = 9),
    Song(8, "Blue Hour (Live)", "Hollow Pines", "Amber", 2020, 311, playCount = 3),
    Song(9, "Driftwood", "Marlowe Kei", "Coastline", 2024, 192, playCount = 5),
    Song(10, "Northern Wind", "Aurelia Vance", "Quiet Hours", 2023, 233),
    Song(11, "Evergreen [Remastered]", "Sable & Sage", "Moss Room", 2019, 248, true, 14),
    Song(12, "Clay Pots", "Sable & Sage", "Moss Room", 2019, 171, playCount = 2),
    Song(13, "Ivory Keys", "Nadia Orr", "Nocturnes", 2022, 282, playCount = 7),
    Song(14, "Tidal", "Linen & Oak", "Tidewater", 2022, 219, playCount = 3),
    Song(15, "Wild Thyme", "The Fern Collective", "Understory", 2021, 207),
    Song(16, "Stillwater", "Nadia Orr", "Nocturnes", 2022, 254, true, 17),
    Song(17, "Ochre", "Hollow Pines", "Amber", 2020, 196, playCount = 1),
    Song(18, "Moonlit_Garden - prod. Kaito", "Marlowe Kei", "Coastline", 2024, 229, playCount = 10),
    Song(19, "Kinfolk", "Sable & Sage", "Moss Room", 2019, 188, playCount = 4),
    Song(20, "Rainfall Sonata", "Nadia Orr", "Nocturnes", 2022, 301, playCount = 6),
    Song(21, "Quiet Harbour", "Aurelia Vance", "Quiet Hours", 2023, 215, playCount = 2),
    Song(22, "Juniper", "Wren Halloway", "Fieldnotes", 2025, 183, true, 11),
    Song(23, "Umber Sky", "Wren Halloway", "Fieldnotes", 2025, 240, playCount = 3),
    Song(24, "夜の散歩", "森 遥", "Tokyo Dusk", 2021, 236, playCount = 2),
    Song(25, "Vesper (Acoustic Version)", "Linen & Oak", "Tidewater", 2022, 209, playCount = 5),
)

val SEED_PLAYLISTS = listOf(
    Playlist(1, "Evening Wind-down", listOf(2, 5, 11, 13, 16, 20), 2, "2026-08-12"),
    Playlist(2, "Focus Flow", listOf(3, 6, 9, 15, 19), 5, "2026-07-28"),
    Playlist(3, "Road Trip", listOf(0, 4, 7, 8, 14, 17, 22), 8, "2026-06-03"),
)
