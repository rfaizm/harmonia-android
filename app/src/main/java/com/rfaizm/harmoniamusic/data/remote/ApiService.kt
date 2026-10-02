package com.rfaizm.harmoniamusic.data.remote

import com.rfaizm.harmoniamusic.data.remote.dto.LyricsDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** Every endpoint the app can hit. Building the client lives in [ApiConfig]; this only describes the calls. */
interface ApiService {
    /**
     * The exact match: everything has to line up, so it needs a real artist. A null [album] is left out, and the
     * service then matches on artist, title and length alone.
     * Wrapped in [Response] because a song the service doesn't know answers 404 or 503, which is not an error here.
     */
    @GET("get")
    suspend fun getLyrics(
        @Query("artist_name") artist: String,
        @Query("track_name") track: String,
        @Query("album_name") album: String?,
        @Query("duration") duration: Int,
    ): Response<LyricsDto>

    /** The looser search. A null [artist] is left out of the query entirely, which is what a tagless file needs. */
    @GET("search")
    suspend fun searchLyrics(
        @Query("track_name") track: String,
        @Query("artist_name") artist: String?,
    ): Response<List<LyricsDto>>
}
