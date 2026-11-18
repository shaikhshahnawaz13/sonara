package com.sonara.app.app

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sonara.app.core.animation.MotionLevel
import com.sonara.app.data.local.preferences.AnimationIntensity

/**
 * Combines the in-app animation setting with the system "animator duration scale".
 * A system scale of 0 (Android's "remove animations") always wins. Other scale values are not applied
 * here: Compose's own animation clock already follows them.
 */
internal fun resolveMotionLevel(intensity: AnimationIntensity, systemAnimatorScale: Float): MotionLevel =
    if (systemAnimatorScale <= 0f) {
        MotionLevel.Off
    } else {
        when (intensity) {
            AnimationIntensity.Full -> MotionLevel.Full
            AnimationIntensity.Reduced -> MotionLevel.Reduced
            AnimationIntensity.Off -> MotionLevel.Off
        }
    }

private fun readAnimatorScale(context: Context): Float =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

/** The system animator duration scale, kept current while the user changes it in system settings. */
@Composable
internal fun rememberSystemAnimatorScale(): Float {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(readAnimatorScale(context)) }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scale = readAnimatorScale(context)
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        scale = readAnimatorScale(context)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return scale
}
