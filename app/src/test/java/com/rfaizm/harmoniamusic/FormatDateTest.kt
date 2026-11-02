package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.ui.formatDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** T21: a playlist's date follows the phone's language instead of always reading "Nov 2, 2026". */
class FormatDateTest {
    @Test fun englishReadsMonthDayYear() {
        assertEquals("Nov 2, 2026", formatDate("2026-11-02", Locale.US))
    }

    @Test fun indonesianPutsTheDayFirst() {
        val date = formatDate("2026-11-02", Locale("in", "ID"))
        assertTrue(date, date.startsWith("2 ") && date.endsWith(" 2026"))
    }

    @Test fun aBrokenDateIsShownAsItIs() {
        assertEquals("someday", formatDate("someday", Locale.US))
    }
}
