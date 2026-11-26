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

/**
 * Line weights. [hairline] (1dp) is every divider and quiet outline; [rule] (2dp) is the one deliberate heavy line,
 * such as the underline of the chosen category or the rule above a section. [standard] and [emphasis] are the V1
 * names for the same two values and stay so existing screens keep compiling.
 */
object SonaraBorders {
    val hairline = 1.dp
    val rule = 2.dp
    val standard = hairline
    val emphasis = rule
}

/**
 * Page layout. One grid for every screen: 20dp side margins, 16dp between columns, and three vertical rhythms
 * (item, block, section) so whitespace is chosen, not improvised. All are multiples of 4dp.
 */
object SonaraLayout {
    /** Left and right margin of every screen. */
    val screenMargin = 20.dp
    /** Space between columns (a grid of albums) and between a thumbnail and its text. */
    val gutter = 16.dp
    /** Between neighbouring rows or controls. */
    val itemGap = 12.dp
    /** Between a heading and what it introduces, and between related blocks. */
    val blockGap = 24.dp
    /** Between sections. Generous on purpose: whitespace does the separating, not boxes. */
    val sectionGap = 40.dp
    /** Minimum height of a list row (also a 48dp touch target plus padding). */
    val rowMinHeight = 56.dp
}

/** Artwork edge lengths. Artwork is always square and is not decorated with borders or shadows. */
object SonaraArtworkSizes {
    /** Leading thumbnail of a list row. */
    val row = 48.dp
    /** A tile in a grid or carousel. */
    val tile = 144.dp
    /** A feature image at the top of a detail screen. */
    val feature = 240.dp
}

/** Shared control sizes (spec sections 3.5 and 12). */
object SonaraSizes {
    val minTouchTarget = 48.dp
    val iconSmall = 16.dp
    val iconStandard = 24.dp
    val iconSecondary = 20.dp
    val iconPrimary = 28.dp
    val topBarMinHeight = 56.dp
    val bottomNavHeight = 64.dp
}

/** Near-square geometry: radii stay between 0 and 6dp, so nothing reads as a rounded card. */
@Immutable
data class SonaraShapes(
    val none: RoundedCornerShape = RoundedCornerShape(0.dp),
    val small: RoundedCornerShape = RoundedCornerShape(2.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(4.dp),
    val large: RoundedCornerShape = RoundedCornerShape(6.dp),
)
