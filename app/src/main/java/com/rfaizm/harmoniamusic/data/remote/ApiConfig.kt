package com.rfaizm.harmoniamusic.data.remote

import com.rfaizm.harmoniamusic.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Builds the client and holds the base url; the calls themselves are in [ApiService]. */
@Module
@InstallIn(SingletonComponent::class)
object ApiConfig {
    private const val TIMEOUT_SECONDS = 10L
    private const val USER_AGENT = "Harmonia/1.0 (offline music player; com.rfaizm.harmoniamusic)"

    /** Built once, when first asked for, so nothing is created for someone who never turns the lookup on. */
    @Provides
    @Singleton
    fun lyricsService(): ApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // The lyrics service asks callers to name themselves.
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.LYRICS_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
