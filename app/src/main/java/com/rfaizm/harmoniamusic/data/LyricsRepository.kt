package com.rfaizm.harmoniamusic.data

import com.rfaizm.harmoniamusic.data.local.LrcFolderSource
import com.rfaizm.harmoniamusic.data.local.LyricsStore
import com.rfaizm.harmoniamusic.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lyrics beyond the song's own tags (M6, T32), offline first: a `.lrc` beside the song, then anything found online
 * before; the network only when the user asks, and what it finds is kept for next time. The tags themselves come
 * from the player, which reads the file.
 */
@Singleton
class LyricsRepository @Inject constructor(
    private val lrcFolder: LrcFolderSource,
    private val store: LyricsStore,
    private val lrclib: ApiService,
) {
    /** What the phone already has for this song. Never touches the network. */
    suspend fun saved(song: Song): String? = lrcFolder.read(song) ?: store.get(song.id)

    /** The only way this app uses the network, and only because the user tapped "Find lyrics online". */
    suspend fun findOnline(song: Song, playerArtist: String?): LyricsResult =
        fetchLyrics(lrclib, song, playerArtist).also { if (it is LyricsResult.Found) store.put(song.id, it.text) }
}
