package com.rfaizm.harmoniamusic.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.rfaizm.harmoniamusic.data.SettingsRepository
import com.rfaizm.harmoniamusic.data.SettingsRepository.Key
import com.rfaizm.harmoniamusic.data.SettingsRepository.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** The Settings tab (M6, T33): its switches and the `.lrc` folder, as [SettingsRepository] keeps them. */
@HiltViewModel
class SettingsViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val flags: StateFlow<Map<Key, Boolean>> = settings.flags
    val lyricsFolder: StateFlow<String?> = settings.lyricsFolder
    val theme: StateFlow<Theme> = settings.theme

    operator fun set(key: Key, value: Boolean) {
        settings[key] = value
    }

    fun grantLyricsFolder(folder: Uri) = settings.grantLyricsFolder(folder)

    fun setTheme(theme: Theme) = settings.setTheme(theme)
}
