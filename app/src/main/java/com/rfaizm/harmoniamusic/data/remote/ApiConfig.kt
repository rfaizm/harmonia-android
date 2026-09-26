package com.rfaizm.harmoniamusic.data.remote

import com.rfaizm.harmoniamusic.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Builds the client and holds the base url; the calls themselves are in [ApiService]. */
object ApiConfig {
    private const val TIMEOUT_SECONDS = 10L
    private const val USER_AGENT = "Harmonia/1.0 (offline music player; com.rfaizm.harmoniamusic)"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // The lyrics service asks callers to name themselves.
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
            }
            .build()
    }

    /** Created once, on first use, so nothing is built for someone who never turns the lookup on. */
    val lyricsService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.LYRICS_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
