package com.sonara.app.core.permissions

import androidx.compose.runtime.staticCompositionLocalOf

/** What the UI may ask the host Activity to do. The Activity owns the launchers; screens only call these. */
class PermissionActions(
    val requestAudio: () -> Unit,
    val openAppSettings: () -> Unit,
)

val LocalPermissionActions = staticCompositionLocalOf {
    PermissionActions(requestAudio = {}, openAppSettings = {})
}
