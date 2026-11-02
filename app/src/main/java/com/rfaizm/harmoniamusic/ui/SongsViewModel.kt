package com.rfaizm.harmoniamusic.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rfaizm.harmoniamusic.R
import com.rfaizm.harmoniamusic.data.MusicRepository
import com.rfaizm.harmoniamusic.data.Playlist
import com.rfaizm.harmoniamusic.data.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal enum class SortBy(@StringRes val label: Int) { AZ(R.string.sort_az), Recent(R.string.sort_recent), MostPlayed(R.string.sort_most_played) }

/** A row of the Songs list: a letter heading (A–Z only) or a song with its place in the sorted list. */
internal sealed interface Entry {
    data class Header(val letter: String) : Entry
    data class Item(val song: Song, val index: Int) : Entry
}

/** What the Songs list shows for a search and a sort. A–Z headings come from the same rule as the order (T41). */
internal fun songEntries(songs: List<Song>, query: String, sortBy: SortBy): List<Entry> {
    val q = query.trim().lowercase()
    val filtered = songs.filter { q.isEmpty() || it.displayTitle.lowercase().contains(q) || it.displayArtist.lowercase().contains(q) }
    val sorted = when (sortBy) {
        SortBy.AZ -> azOrder(filtered)
        SortBy.Recent -> filtered.sortedByDescending { it.id }.map { null to it }
        SortBy.MostPlayed -> filtered.sortedByDescending { it.playCount }.map { null to it }
    }
    return buildList {
        var last: String? = null
        sorted.forEachIndexed { i, (letter, s) ->
            if (letter != null && letter != last) add(Entry.Header(letter)).also { last = letter }
            add(Entry.Item(s, i))
        }
    }
}

/** A playlist's songs in its own order, skipping any deleted since: one lookup per song, not a search per song. */
internal fun resolve(ids: List<Int>, byId: Map<Int, Song>): List<Song> = ids.mapNotNull(byId::get)

/**
 * The Songs tab (M6, T33). The search and the sort survive rotation and the app being killed in the background
 * ([SavedStateHandle]). The search is Compose state rather than a flow: a text field fed asynchronously can jump its
 * cursor while typing.
 */
@HiltViewModel
class SongsViewModel @Inject constructor(private val music: MusicRepository, private val saved: SavedStateHandle) : ViewModel() {
    var query by mutableStateOf(saved.get<String>(QUERY).orEmpty())
        private set
    internal var sortBy by mutableStateOf(saved.get<SortBy>(SORT) ?: SortBy.AZ)
        private set

    val playlists: StateFlow<List<Playlist>> = music.playlists

    internal val entries: StateFlow<List<Entry>> =
        combine(music.songs, snapshotFlow { query to sortBy }) { songs, (q, by) -> songEntries(songs, q, by) }
            .flowOn(Dispatchers.Default) // sorting a large library stays off the main thread
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), songEntries(music.songs.value, query, sortBy))

    fun search(text: String) {
        query = text
        saved[QUERY] = text
    }

    internal fun sort(by: SortBy) {
        sortBy = by
        saved[SORT] = by
    }

    fun toggleLike(song: Song) = music.toggleLike(song.id)

    fun addToPlaylist(name: String, ids: Collection<Int>) = music.addToPlaylist(name, ids)

    private companion object {
        const val QUERY = "query"
        const val SORT = "sortBy"
    }
}
