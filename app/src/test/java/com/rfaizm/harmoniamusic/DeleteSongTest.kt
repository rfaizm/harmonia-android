package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.ui.DeletePath
import com.rfaizm.harmoniamusic.ui.deletePathFor
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteSongTest {
    @Test
    fun androidElevenAndLaterLetsTheSystemAskBeforeDeleting() {
        assertEquals(DeletePath.SystemDialog, deletePathFor(sdkInt = 34))
        assertEquals(DeletePath.SystemDialog, deletePathFor(sdkInt = 30))
    }

    @Test
    fun androidTenAsksForConsentOnlyAfterTheFirstAttemptIsRefused() {
        assertEquals(DeletePath.RecoverableConsent, deletePathFor(sdkInt = 29))
    }

    @Test
    fun olderAndroidNeedsTheStoragePermissionInstead() {
        assertEquals(DeletePath.StoragePermission, deletePathFor(sdkInt = 28))
        assertEquals(DeletePath.StoragePermission, deletePathFor(sdkInt = 24)) // minSdk
    }
}
