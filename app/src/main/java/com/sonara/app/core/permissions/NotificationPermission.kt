package com.sonara.app.core.permissions

import android.os.Build

/** Pure rules for the Android 13+ notification permission, which the playback notification needs. */
object NotificationPermissionPolicy {

    const val PERMISSION = "android.permission.POST_NOTIFICATIONS"

    /** Before Android 13 notifications need no runtime permission. */
    fun isRuntimePermission(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU

    /**
     * Ask once, and only after audio access is granted: asking for both at the first launch would show two
     * dialogs before the person has seen anything. A refused request is not repeated automatically.
     */
    fun shouldRequest(
        sdkInt: Int,
        audioGranted: Boolean,
        notificationGranted: Boolean,
        requestedBefore: Boolean,
    ): Boolean = isRuntimePermission(sdkInt) && audioGranted && !notificationGranted && !requestedBefore
}
