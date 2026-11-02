package com.rfaizm.harmoniamusic

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * F3: AGP normally puts the dependency list in the APK's signing block, encrypted with a key only Google holds.
 * F-Droid's scanner rejects that block, so the build turns it off. The check on a real APK is in tasks/todo.md.
 */
class FdroidBuildTest {
    private val gradle = File("build.gradle.kts").readText() // Gradle runs the tests from app/

    @Test fun theApkCarriesNoGoogleOnlyDependencyBlock() {
        assertTrue(Regex("""dependenciesInfo\s*\{[^}]*includeInApk\s*=\s*false""").containsMatchIn(gradle))
        assertTrue(Regex("""dependenciesInfo\s*\{[^}]*includeInBundle\s*=\s*false""").containsMatchIn(gradle))
    }
}
