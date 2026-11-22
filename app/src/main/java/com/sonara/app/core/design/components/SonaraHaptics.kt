package com.sonara.app.core.design.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * The app's haptics behind one switch (Settings > Appearance > Haptics). Every vibration goes through here, so
 * turning the setting off silences all of them and none is left behind.
 */
@Stable
class SonaraHaptics(private val enabled: Boolean, private val feedback: HapticFeedback?) {

    /** A light tick for stepping a control. */
    fun tick() {
        if (enabled) feedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** The firmer pulse for picking something up (starting a reorder). */
    fun press() {
        if (enabled) feedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

val LocalSonaraHaptics = staticCompositionLocalOf { SonaraHaptics(enabled = false, feedback = null) }

@Composable
fun ProvideSonaraHaptics(enabled: Boolean, content: @Composable () -> Unit) {
    val feedback = LocalHapticFeedback.current
    val haptics = remember(enabled, feedback) { SonaraHaptics(enabled, feedback) }
    CompositionLocalProvider(LocalSonaraHaptics provides haptics, content = content)
}
