package com.rfaizm.harmoniamusic

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** F1: F-Droid only takes open-source apps, and its reviewers check the license says the same everywhere. */
class LicenseTest {
    private val root = File("..") // Gradle runs the tests from app/

    @Test fun theRepoIsGpl3() {
        val license = File(root, "LICENSE")
        assertTrue("no LICENSE at the repo root", license.isFile)
        val head = license.readLines().take(2).map { it.trim() }
        assertTrue(head.toString(), head == listOf("GNU GENERAL PUBLIC LICENSE", "Version 3, 29 June 2007"))
    }

    @Test fun theReadmeNamesTheSameLicense() {
        assertTrue(File(root, "README.md").readText().contains("GPL-3.0"))
    }
}
