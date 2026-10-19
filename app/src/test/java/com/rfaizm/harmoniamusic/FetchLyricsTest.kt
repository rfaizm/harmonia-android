package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.LyricsResult
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.fetchLyrics
import com.rfaizm.harmoniamusic.data.remote.ApiService
import com.rfaizm.harmoniamusic.data.remote.dto.LyricsDto
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * M6 (T32): the whole online lookup against a fake LRCLIB. Taking the service as a dependency is what makes this
 * possible; before T31 the lookup reached for the real one itself.
 */
class FetchLyricsTest {
    private val yellow = Song(1, "Yellow", "Coldplay", "Parachutes", 2000, 269)

    @Test
    fun aSyncedExactMatchIsTakenStraightAway() = runBlocking {
        val lrclib = FakeLrcLib(exact = entry(syncedLyrics = "[00:35.66]Look at the stars"))

        assertEquals(LyricsResult.Found("[00:35.66]Look at the stars"), fetchLyrics(lrclib, yellow, playerArtist = null))
        assertEquals(0, lrclib.searches) // no second request when the exact one already has timings
    }

    @Test
    fun aSyncedSearchResultBeatsAPlainExactMatch() = runBlocking {
        val lrclib = FakeLrcLib(
            exact = entry(plainLyrics = "Look at the stars"),
            search = listOf(entry(syncedLyrics = "[00:35.66]Look at the stars")),
        )

        assertEquals(LyricsResult.Found("[00:35.66]Look at the stars"), fetchLyrics(lrclib, yellow, playerArtist = null))
    }

    @Test
    fun anotherSongsLyricsAreNeverShown() = runBlocking {
        val lrclib = FakeLrcLib(search = listOf(entry(track = "Yellow Submarine", artist = "The Beatles", syncedLyrics = "[00:01.00]In the town")))

        assertEquals(LyricsResult.NotFound, fetchLyrics(lrclib, yellow, playerArtist = null))
    }

    @Test
    fun noConnectionReadsAsOffline() = runBlocking {
        assertEquals(LyricsResult.Offline, fetchLyrics(FakeLrcLib(offline = true), yellow, playerArtist = null))
    }

    private fun entry(
        track: String = "Yellow",
        artist: String = "Coldplay",
        plainLyrics: String? = null,
        syncedLyrics: String? = null,
    ) = LyricsDto(trackName = track, artistName = artist, duration = 269.0, plainLyrics = plainLyrics, syncedLyrics = syncedLyrics)

    /** Answers like LRCLIB: the exact lookup gives one entry or an error status, the search gives a list. */
    private class FakeLrcLib(
        private val exact: LyricsDto? = null,
        private val search: List<LyricsDto> = emptyList(),
        private val offline: Boolean = false,
    ) : ApiService {
        var searches = 0

        override suspend fun getLyrics(artist: String, track: String, album: String?, duration: Int): Response<LyricsDto> {
            if (offline) throw IOException("no network")
            return exact?.let { Response.success(it) } ?: Response.error(404, "".toResponseBody())
        }

        override suspend fun searchLyrics(track: String, artist: String?): Response<List<LyricsDto>> {
            if (offline) throw IOException("no network")
            searches++
            return Response.success(search)
        }
    }
}
