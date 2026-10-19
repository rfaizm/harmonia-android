package com.rfaizm.harmoniamusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.rfaizm.harmoniamusic.data.LyricsRepository
import com.rfaizm.harmoniamusic.data.MusicRepository
import com.rfaizm.harmoniamusic.data.SettingsRepository
import com.rfaizm.harmoniamusic.ui.HarmoniaApp
import com.rfaizm.harmoniamusic.ui.LocalMusic
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var music: MusicRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var lyrics: LyricsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Album art deep in the tree reads the library from LocalMusic.
        setContent { CompositionLocalProvider(LocalMusic provides music) { HarmoniaApp(music, settings, lyrics) } }
    }
}
