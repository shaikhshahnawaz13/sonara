package com.sonara.app.core.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPermissionPolicyTest {

    @Test
    fun android12UsesLegacyStoragePermission() {
        assertEquals("android.permission.READ_EXTERNAL_STORAGE", AudioPermissionPolicy.requiredPermission(31))
        assertEquals("android.permission.READ_EXTERNAL_STORAGE", AudioPermissionPolicy.requiredPermission(32))
    }

    @Test
    fun android13AndNewerUseReadMediaAudio() {
        assertEquals("android.permission.READ_MEDIA_AUDIO", AudioPermissionPolicy.requiredPermission(33))
        assertEquals("android.permission.READ_MEDIA_AUDIO", AudioPermissionPolicy.requiredPermission(34))
        assertEquals("android.permission.READ_MEDIA_AUDIO", AudioPermissionPolicy.requiredPermission(36))
    }

    @Test
    fun grantedWinsOverEverythingElse() {
        assertEquals(
            AudioAccessState.Granted,
            AudioPermissionPolicy.resolve(granted = true, requestedBefore = false, shouldShowRationale = false),
        )
        assertEquals(
            AudioAccessState.Granted,
            AudioPermissionPolicy.resolve(granted = true, requestedBefore = true, shouldShowRationale = true),
        )
    }

    @Test
    fun neverRequestedIsNotRequested() {
        assertEquals(
            AudioAccessState.NotRequested,
            AudioPermissionPolicy.resolve(granted = false, requestedBefore = false, shouldShowRationale = false),
        )
    }

    @Test
    fun deniedOnceWithRationaleIsDenied() {
        assertEquals(
            AudioAccessState.Denied,
            AudioPermissionPolicy.resolve(granted = false, requestedBefore = true, shouldShowRationale = true),
        )
    }

    @Test
    fun deniedWithoutRationaleAfterRequestIsPermanentlyDenied() {
        assertEquals(
            AudioAccessState.PermanentlyDenied,
            AudioPermissionPolicy.resolve(granted = false, requestedBefore = true, shouldShowRationale = false),
        )
    }
}
