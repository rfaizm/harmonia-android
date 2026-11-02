package com.rfaizm.harmoniamusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

/**
 * F2: F-Droid finds the version by searching build.gradle.kts with regular expressions (it never runs Gradle), so
 * both numbers have to be written there plainly. The code keeps the scheme major * 10000 + minor * 100 + patch, so
 * every newer version has a bigger code, as Android and Play require.
 */
class VersionTest {
    private val gradle = File("build.gradle.kts").readText() // Gradle runs the tests from app/

    @Test fun bothNumbersAreWrittenPlainly() {
        assertNotNull("no plain versionCode", Regex("""versionCode\s*=\s*(\d+)""").find(gradle))
        assertNotNull("no plain versionName", Regex("""versionName\s*=\s*"([^"]+)"""").find(gradle))
    }

    @Test fun theCodeMatchesTheName() {
        val code = Regex("""versionCode\s*=\s*(\d+)""").find(gradle)!!.groupValues[1].toInt()
        val name = Regex("""versionName\s*=\s*"([^"]+)"""").find(gradle)!!.groupValues[1]
        val (major, minor, patch) = name.split(".").map { it.toInt() }
        assertEquals(major * 10_000 + minor * 100 + patch, code)
    }
}
