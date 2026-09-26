package com.rfaizm.harmoniamusic.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * One lyrics entry as the service returns it, from either endpoint.
 *
 * [plainLyrics] and [syncedLyrics] are nullable on purpose: most search results carry a JSON null there, and an
 * instrumental track always does.
 */
data class LyricsDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("trackName") val trackName: String? = null,
    @SerializedName("artistName") val artistName: String? = null,
    @SerializedName("albumName") val albumName: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("instrumental") val instrumental: Boolean = false,
    @SerializedName("plainLyrics") val plainLyrics: String? = null,
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null,
)
