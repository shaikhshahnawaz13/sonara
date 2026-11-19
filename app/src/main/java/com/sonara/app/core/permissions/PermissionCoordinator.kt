package com.sonara.app.core.permissions

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.sonara.app.data.local.preferences.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Activity-scoped: owns the runtime-permission launchers (they must be registered in onCreate) and
 * reports results into the process-wide [AudioAccessModel]. Create it in Activity.onCreate.
 */
class PermissionCoordinator(
    private val activity: ComponentActivity,
    private val audioAccess: AudioAccessModel,
    private val settings: SettingsRepository,
    private val checker: AudioPermissionChecker,
    private val scope: CoroutineScope,
) {
    /** True while the notification dialog is up or its result is being recorded; prevents a second launch. */
    private var notificationRequestInFlight = false

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        scope.launch {
            if (checker.permission in results) settings.markAudioPermissionRequested()
            if (NotificationPermissionPolicy.PERMISSION in results) {
                settings.markNotificationPermissionRequested()
                notificationRequestInFlight = false
            }
            refresh()
            maybeAskForNotifications()
        }
    }

    val actions = PermissionActions(
        requestAudio = { launcher.launch(arrayOf(checker.permission)) },
        openAppSettings = {
            activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
    )

    /** Call from onResume: the person may have changed the permission in system Settings. */
    fun refresh() {
        val rationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, checker.permission)
        audioAccess.refresh(rationale)
        scope.launch { maybeAskForNotifications() }
    }

    private suspend fun maybeAskForNotifications() {
        val sdk = Build.VERSION.SDK_INT
        if (!NotificationPermissionPolicy.isRuntimePermission(sdk) || notificationRequestInFlight) return
        val notificationGranted = ContextCompat.checkSelfPermission(
            activity, NotificationPermissionPolicy.PERMISSION,
        ) == PackageManager.PERMISSION_GRANTED
        val shouldAsk = NotificationPermissionPolicy.shouldRequest(
            sdkInt = sdk,
            audioGranted = checker.isGranted(),
            notificationGranted = notificationGranted,
            requestedBefore = settings.settings.first().notificationPermissionRequested,
        )
        if (shouldAsk && !notificationRequestInFlight) {
            notificationRequestInFlight = true
            launcher.launch(arrayOf(NotificationPermissionPolicy.PERMISSION))
        }
    }
}
