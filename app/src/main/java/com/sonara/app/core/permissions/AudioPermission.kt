package com.sonara.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** What the app can currently say about access to shared audio. */
enum class AudioAccessState {
    /** Permission is granted. */
    Granted,

    /** The permission has never been requested; show the contextual explanation first. */
    NotRequested,

    /** Denied, but the system will still show the request dialog again. */
    Denied,

    /** Denied and the system will no longer show the dialog; the user must use system Settings. */
    PermanentlyDenied,
}

/** Pure, unit-tested permission rules (spec sections 2 and 10). */
object AudioPermissionPolicy {

    /** Android 13+ uses READ_MEDIA_AUDIO; Android 12 (API 31-32) uses the legacy storage permission. */
    fun requiredPermission(sdkInt: Int): String =
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /**
     * Derives the access state from facts the platform exposes.
     * Android has no direct "permanently denied" query: a permission that was requested before, is not
     * granted, and no longer warrants a rationale is treated as permanently denied.
     */
    fun resolve(
        granted: Boolean,
        requestedBefore: Boolean,
        shouldShowRationale: Boolean,
    ): AudioAccessState = when {
        granted -> AudioAccessState.Granted
        !requestedBefore -> AudioAccessState.NotRequested
        shouldShowRationale -> AudioAccessState.Denied
        else -> AudioAccessState.PermanentlyDenied
    }
}

/** Reads the current grant state. Requesting the permission is wired up with discovery in M2. */
class AudioPermissionChecker(private val context: Context) {

    val permission: String
        get() = AudioPermissionPolicy.requiredPermission(Build.VERSION.SDK_INT)

    fun isGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
