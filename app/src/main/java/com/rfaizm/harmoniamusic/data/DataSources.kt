package com.rfaizm.harmoniamusic.data

import android.content.Context
import com.rfaizm.harmoniamusic.data.local.LrcFolderSource
import com.rfaizm.harmoniamusic.data.local.LyricsStore
import com.rfaizm.harmoniamusic.data.remote.ApiService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * M6 (T31): how the lyrics view, which Hilt doesn't build, reaches
 * the data sources, which Hilt does build, once each. The repositories take them by injection in T32, and this goes.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataSources {
    fun lrcFolder(): LrcFolderSource
    fun lyricsStore(): LyricsStore
    fun lyricsService(): ApiService
}

fun Context.dataSources(): DataSources = EntryPointAccessors.fromApplication(this, DataSources::class.java)
