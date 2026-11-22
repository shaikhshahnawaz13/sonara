package com.sonara.app.core.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.sonara.app.core.animation.MotionTokens
import com.sonara.app.core.design.SonaraTheme

/**
 * Gives an icon that changes meaning (play to pause, repeat to repeat-one) a short compression and release at
 * the moment it changes: the new icon appears slightly small and settles to full size. [state] is whatever
 * decides which icon is shown. Nothing animates on first composition, and with motion Off the change is
 * instant. The scale is read only in the layer, so it costs no recomposition.
 */
@Composable
fun Modifier.iconStateChange(state: Any?): Modifier {
    val motion = SonaraTheme.motion
    val scale = remember { Animatable(1f) }
    val first = remember { BooleanArray(1) { true } }
    LaunchedEffect(state) {
        if (first[0]) {
            first[0] = false
            return@LaunchedEffect
        }
        if (motion.isOff) {
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        scale.snapTo(MotionTokens.ICON_SWAP_FROM_SCALE)
        scale.animateTo(1f, motion.iconStateSpec())
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
