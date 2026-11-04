package com.rfaizm.harmoniamusic.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import com.rfaizm.harmoniamusic.R
import com.rfaizm.harmoniamusic.data.SettingsRepository
import com.rfaizm.harmoniamusic.data.SettingsRepository.Key
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** The Settings tab (M6, T33): its switches and the `.lrc` folder, as [SettingsRepository] keeps them. */
@HiltViewModel
class SettingsViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val flags: StateFlow<Map<Key, Boolean>> = settings.flags
    val lyricsFolder: StateFlow<String?> = settings.lyricsFolder

    operator fun set(key: Key, value: Boolean) {
        settings[key] = value
    }

    fun grantLyricsFolder(folder: Uri) = settings.grantLyricsFolder(folder)

    /** The app's own language choice; AppCompat stores it (L1). */
    val language get() = languageOf(AppCompatDelegate.getApplicationLocales().toLanguageTags())

    /** Redraws the app in [language]. AppCompat recreates the activity, and remembers the choice. */
    fun setLanguage(language: AppLanguage) =
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
}

/** The languages the app speaks. Each is named in itself, so anyone can find their own (L1). */
enum class AppLanguage(val tag: String, @StringRes val label: Int) {
    System("", R.string.language_system),
    English("en", R.string.language_english),
    Indonesian("id", R.string.language_indonesian),
}

/** The choice behind AppCompat's tags, e.g. "id-ID"; Android still reports Indonesian as "in" in places. */
internal fun languageOf(tags: String): AppLanguage = when (tags.substringBefore(',').substringBefore('-').lowercase()) {
    "en" -> AppLanguage.English
    "id", "in" -> AppLanguage.Indonesian
    else -> AppLanguage.System
}
