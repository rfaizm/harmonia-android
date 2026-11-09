package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.ui.rateLinks
import org.junit.Assert.assertEquals
import org.junit.Test

/** L5: Settings › Rate Harmonia tries the store app first (F-Droid and Play both take market://), then GitHub. */
class RateLinksTest {
    @Test fun storeFirstThenGitHub() {
        assertEquals(
            listOf("market://details?id=com.rfaizm.harmoniamusic", "https://github.com/rfaizm/harmonia-android"),
            rateLinks,
        )
    }
}
