package com.rfaizm.harmoniamusic.ui

import com.rfaizm.harmoniamusic.data.Playlist
import com.rfaizm.harmoniamusic.data.Song

/** Made-up songs for `@Preview`s, covering the tag mess `cleanTag` exists for. The app itself shows MediaStore. */
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
