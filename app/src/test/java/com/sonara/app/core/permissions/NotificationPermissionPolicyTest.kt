package com.sonara.app.core.permissions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPolicyTest {

    @Test
    fun noRuntimePermissionBeforeAndroid13() {
        assertFalse(NotificationPermissionPolicy.isRuntimePermission(31))
        assertFalse(NotificationPermissionPolicy.isRuntimePermission(32))
        assertTrue(NotificationPermissionPolicy.isRuntimePermission(33))
        assertTrue(NotificationPermissionPolicy.isRuntimePermission(36))
    }

    @Test
    fun asksOnlyOnAndroid13PlusAfterAudioIsGrantedAndNeverTwice() {
        fun ask(sdk: Int, audio: Boolean, granted: Boolean, before: Boolean) =
            NotificationPermissionPolicy.shouldRequest(sdk, audio, granted, before)
        assertTrue(ask(33, audio = true, granted = false, before = false))
        assertFalse(ask(32, audio = true, granted = false, before = false))
        assertFalse(ask(34, audio = false, granted = false, before = false))
        assertFalse(ask(34, audio = true, granted = true, before = false))
        assertFalse(ask(34, audio = true, granted = false, before = true))
    }
}
