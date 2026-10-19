package com.rfaizm.harmoniamusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * M6 (T32): outside `data/`, the app talks to repositories only. Nothing there opens files, reads preferences,
 * queries MediaStore or the content resolver, or calls HTTP; the repositories in `data/` do that through their
 * sources. Tests read the sources from the module folder, where Gradle runs them.
 */
class ArchitectureTest {
    private val app = File("src/main/java/com/rfaizm/harmoniamusic")

    @Test
    fun onlyTheDataLayerDoesIo() {
        assertTrue("sources not found from ${File("").absolutePath}", app.isDirectory)
        val io = listOf(
            "import android.provider.MediaStore", "import android.provider.DocumentsContract",
            "import android.content.ContentResolver", "import android.content.SharedPreferences", "import java.io.File",
            "import retrofit2.", "import okhttp3.", "getSharedPreferences(", "contentResolver", "File(",
            // T31's bridge to the sources; the repositories replace it.
            "dataSources()",
        )

        val outsideData = app.walk()
            .filter { it.extension == "kt" && !it.path.replace('\\', '/').contains("/harmoniamusic/data/") }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { i, line ->
                    io.firstOrNull { it in line && !line.trimStart().startsWith("//") && !line.trimStart().startsWith("*") }
                        ?.let { "${file.name}:${i + 1} $it" }
                }
            }.toList()

        assertEquals(emptyList<String>(), outsideData)
    }

    @Test
    fun screensLeaveThePlayerToItsViewModel() {
        // M6 (T33): the MediaController connection, the playback commands and the volume belong to PlayerViewModel,
        // so they outlive rotation and the composables only draw state and report taps.
        val playerApis = listOf("import androidx.media3.session.", "import android.media.AudioManager")

        val screens = File(app, "ui").walk()
            .filter { it.extension == "kt" && !it.name.endsWith("ViewModel.kt") }
            .flatMap { file -> file.readLines().filter { line -> playerApis.any { line.startsWith(it) } }.map { "${file.name}: $it" } }
            .toList()

        assertEquals(emptyList<String>(), screens)
    }
}
