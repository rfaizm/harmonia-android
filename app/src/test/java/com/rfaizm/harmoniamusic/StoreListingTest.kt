package com.rfaizm.harmoniamusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.File

/**
 * F4: the F-Droid store page comes from fastlane/metadata/android/<locale>/. These are F-Droid's limits, checked
 * for every language the app speaks, and the changelog has to exist for the version code being released.
 */
class StoreListingTest {
    private val root = File("../fastlane/metadata/android") // Gradle runs the tests from app/
    private val locales = listOf("en-US", "id")
    private val versionCode = Regex("""versionCode\s*=\s*(\d+)""").find(File("build.gradle.kts").readText())!!.groupValues[1]

    private fun text(locale: String, name: String): String {
        val file = File(root, "$locale/$name")
        assertTrue("missing ${file.path}", file.isFile)
        return file.readText().trim()
    }

    @Test fun titlesAndDescriptionsFitFdroidsLimits() = locales.forEach { locale ->
        val title = text(locale, "title.txt")
        val short = text(locale, "short_description.txt")
        val full = text(locale, "full_description.txt")
        assertTrue("$locale title: ${title.length} > 50", title.length in 1..50)
        assertTrue("$locale short description: ${short.length} > 80", short.length in 1..80)
        assertTrue("$locale short description ends with a full stop", !short.endsWith("."))
        assertTrue("$locale full description: ${full.length} > 4000", full.length in 1..4000)
    }

    @Test fun theReleasedVersionHasAChangelog() = locales.forEach { locale ->
        val changelog = text(locale, "changelogs/$versionCode.txt")
        assertTrue("$locale changelog: ${changelog.length} > 500", changelog.length in 1..500)
    }

    @Test fun theIconIsA512PixelPng() {
        val icon = File(root, "en-US/images/icon.png")
        assertTrue("missing ${icon.path}", icon.isFile)
        DataInputStream(icon.inputStream()).use { png ->
            val header = ByteArray(16).also(png::readFully)
            assertEquals("not a PNG", "PNG", String(header, 1, 3))
            assertEquals(512 to 512, png.readInt() to png.readInt()) // IHDR width and height
        }
    }
}
