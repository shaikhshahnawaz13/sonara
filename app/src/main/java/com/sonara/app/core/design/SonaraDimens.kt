package com.sonara.app.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/** 4dp spacing scale (spec section 3.4). The name is the value in dp. */
object SonaraSpacing {
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s20 = 20.dp
    val s24 = 24.dp
    val s32 = 32.dp
    val s40 = 40.dp
}

/** Border widths (spec section 3.4): standard 1dp, emphasis 2dp. */
object SonaraBorders {
    val standard = 1.dp
    val emphasis = 2.dp
}

/** Shared control sizes (spec sections 3.5 and 12). */
object SonaraSizes {
    val minTouchTarget = 48.dp
    val iconStandard = 24.dp
    val iconSecondary = 20.dp
    val iconPrimary = 28.dp
    val topBarMinHeight = 56.dp
    val bottomNavHeight = 64.dp
}

/** Square geometry with small 0-6dp radii (spec section 3.4). */
@Immutable
data class SonaraShapes(
    val none: RoundedCornerShape = RoundedCornerShape(0.dp),
    val small: RoundedCornerShape = RoundedCornerShape(2.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(4.dp),
    val large: RoundedCornerShape = RoundedCornerShape(6.dp),
)
