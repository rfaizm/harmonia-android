package com.rfaizm.harmoniamusic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rfaizm.harmoniamusic.data.MusicRepository
import com.rfaizm.harmoniamusic.data.Playlist
import com.rfaizm.harmoniamusic.data.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The Playlists tab (M6, T33). [songsById] turns a playlist's ids into songs with one lookup each; the screen used to
 * search the whole library for every song of every card.
 */
@HiltViewModel
class PlaylistsViewModel @Inject constructor(private val music: MusicRepository) : ViewModel() {
    val songs: StateFlow<List<Song>> = music.songs
    val playlists: StateFlow<List<Playlist>> = music.playlists
    val songsById: StateFlow<Map<Int, Song>> = music.songs
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), music.songs.value.associateBy { it.id })

    fun create(name: String) = music.addToPlaylist(name, emptyList())

    fun addToPlaylist(name: String, ids: Collection<Int>) = music.addToPlaylist(name, ids)

    fun removeFromPlaylist(playlistId: Int, songId: Int) = music.removeFromPlaylist(playlistId, songId)

    fun deletePlaylist(playlist: Playlist) = music.deletePlaylist(playlist.id)

    fun toggleLike(song: Song) = music.toggleLike(song.id)
}
