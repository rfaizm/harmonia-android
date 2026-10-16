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
}
