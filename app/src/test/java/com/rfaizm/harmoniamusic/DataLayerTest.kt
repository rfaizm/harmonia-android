package com.rfaizm.harmoniamusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * M6 (T30): `data/` holds data, and drawing it is the UI's job. Colours, images and `@Composable` functions belong to
 * `ui/`. Compose's runtime state holders may stay in `data/` until the repositories replace them (T32).
 */
class DataLayerTest {
    @Test
    fun theDataLayerDrawsNothing() {
        // Unit tests run from the module folder, so the sources are right here.
        val data = File("src/main/java/com/rfaizm/harmoniamusic/data")
        assertTrue("data/ not found from ${File("").absolutePath}", data.isDirectory)

        val uiImports = data.walk().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines()
                .filter { it.startsWith("import androidx.compose.ui") || it == "import androidx.compose.runtime.Composable" }
                .map { "${file.name}: $it" }
        }.toList()

        assertEquals(emptyList<String>(), uiImports)
    }

    @Test
    fun onlyTheDataSourcesTouchStorageMediaStoreOrTheNetwork() {
        // M6 (T31): files, preferences, MediaStore, the folder grant, decoders and HTTP live in data/local/ and
        // data/remote/, each class taking the app's context once. The rest of data/ is logic over what they return.
        val data = File("src/main/java/com/rfaizm/harmoniamusic/data")
        val io = listOf(
            "android.provider.", "android.content.ContentResolver", "android.content.SharedPreferences", "java.io.File",
            "android.media.MediaMetadataRetriever", "android.graphics.BitmapFactory", "retrofit2.", "okhttp3.",
        )

        val outsideSources = data.listFiles().orEmpty().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines().filter { line -> io.any { line.startsWith("import $it") } }.map { "${file.name}: $it" }
        }

        assertEquals(emptyList<String>(), outsideSources)
    }
}
