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

/** Each artist with their songs, alphabetically. Names are compared as tagged, so differently cased tags stay apart. */
internal fun artistsOf(songs: List<Song>): List<Pair<String, List<Song>>> =
    songs.groupBy { it.displayArtist }.toList().sortedBy { it.first.lowercase() }

/** Each album with its songs, alphabetically. */
internal fun albumsOf(songs: List<Song>): List<Pair<String, List<Song>>> =
    songs.groupBy { it.album }.toList().sortedBy { it.first.lowercase() }

/** The Artists/Albums tab (M6, T33): the library grouped both ways, kept up to date as it changes. */
@HiltViewModel
class ExploreViewModel @Inject constructor(private val music: MusicRepository) : ViewModel() {
    val songs: StateFlow<List<Song>> = music.songs
    val playlists: StateFlow<List<Playlist>> = music.playlists
    internal val artists = music.songs.map(::artistsOf).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), artistsOf(music.songs.value))
    internal val albums = music.songs.map(::albumsOf).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), albumsOf(music.songs.value))

    fun toggleLike(song: Song) = music.toggleLike(song.id)

    fun addToPlaylist(name: String, ids: Collection<Int>) = music.addToPlaylist(name, ids)
}
