package com.rfaizm.harmoniamusic

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * T21: every English string has an Indonesian one with the same placeholders, and the screens already moved to
 * `strings.xml` don't grow new hardcoded text. Add a screen to [translated] once its strings are moved.
 */
class StringsTest {
    private val res = File("src/main/res")
    private val ui = File("src/main/java/com/rfaizm/harmoniamusic/ui")
    private val translated = listOf("SettingsScreen.kt", "Player.kt", "SleepTimer.kt", "SongsScreen.kt", "SongsViewModel.kt")

    /** name → text, skipping strings marked translatable="false". Plural items are keyed name/quantity. */
    private fun strings(folder: String): Map<String, String> {
        val xml = File(res, "$folder/strings.xml").readText()
        val singles = Regex("""<string name="(\w+)"(?![^>]*translatable="false")[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).associate { it.groupValues[1] to it.groupValues[2] }
        val plurals = Regex("""<plurals name="(\w+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL).findAll(xml).flatMap { p ->
            Regex("""<item quantity="(\w+)">(.*?)</item>""").findAll(p.groupValues[2])
                .map { "${p.groupValues[1]}/${it.groupValues[1]}" to it.groupValues[2] }
        }
        return singles + plurals
    }

    private fun placeholders(text: String) = Regex("""%\d\$[sd]""").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun everyStringHasAnIndonesianTranslation() {
        val en = strings("values")
        // Indonesian has no plural forms, so it only needs "other".
        val id = strings("values-in")
        val enKeys = en.keys.filterNot { it.contains('/') && !it.endsWith("/other") }.toSortedSet()
        assertEquals(enKeys, id.keys.toSortedSet())
        val mismatched = enKeys.filter { placeholders(en.getValue(it)) != placeholders(id.getValue(it)) }
        assertEquals(emptyList<String>(), mismatched)
    }

    @Test
    fun translatedScreensHaveNoHardcodedText() {
        val text = Regex(""""[A-Z][a-z][^"]*"""")
        val found = translated.flatMap { name ->
            File(ui, name).readLines().mapIndexedNotNull { i, line ->
                val code = line.substringBefore("//").trimStart()
                if (code.startsWith("*") || code.startsWith("/*")) null else text.find(code)?.let { "$name:${i + 1} ${it.value}" }
            }
        }
        assertEquals(emptyList<String>(), found)
    }
}
