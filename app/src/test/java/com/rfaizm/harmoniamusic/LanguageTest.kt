package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.ui.AppLanguage
import com.rfaizm.harmoniamusic.ui.languageOf
import org.junit.Assert.assertEquals
import org.junit.Test

/** L1: the Settings language row shows the app's own choice, read back from AppCompat's language tags. */
class LanguageTest {
    @Test fun noChoiceMeansTheSystemLanguage() {
        assertEquals(AppLanguage.System, languageOf(""))
    }

    @Test fun eachChoiceReadsBack() {
        AppLanguage.entries.forEach { assertEquals(it, languageOf(it.tag)) }
    }

    @Test fun oldAndNewIndonesianCodesBothCount() {
        // Android still reports Indonesian as the old code "in" in some places, and BCP 47 says "id".
        assertEquals(AppLanguage.Indonesian, languageOf("in"))
        assertEquals(AppLanguage.Indonesian, languageOf("id-ID"))
        assertEquals(AppLanguage.English, languageOf("en-GB,id"))
    }

    @Test fun aLanguageTheAppDoesNotSpeakFallsBackToTheSystem() {
        assertEquals(AppLanguage.System, languageOf("fr"))
    }
}
