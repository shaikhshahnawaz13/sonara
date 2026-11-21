package com.sonara.app.core.design.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import kotlin.math.min

/** Where an overflow menu sits in the window, and which way it opens. */
internal data class MenuPlacement(val x: Int, val y: Int, val opensUpward: Boolean)

/**
 * Places a menu of [content] size relative to its button's real window bounds [anchor].
 *
 * The menu covers the button: its top edge starts at the button's top and its end edge lines up with the
 * button's end edge, so the button is visibly the menu's origin. When there is no room below it flips, its
 * bottom edge on the button's bottom. The result is clamped so the menu never leaves the usable window,
 * i.e. the window minus the system bars plus [margin].
 */
internal fun resolveMenuPlacement(
    anchor: IntRect,
    window: IntSize,
    content: IntSize,
    margin: Int,
    insetTop: Int,
    insetBottom: Int,
    rtl: Boolean,
): MenuPlacement {
    val minX = margin
    val maxX = (window.width - margin - content.width).coerceAtLeast(minX)
    val preferredX = if (rtl) anchor.left else anchor.right - content.width
    val x = preferredX.coerceIn(minX, maxX)

    val minY = insetTop + margin
    val maxY = (window.height - insetBottom - margin - content.height).coerceAtLeast(minY)
    val flipTop = anchor.bottom - content.height
    val up = anchor.top > maxY && flipTop >= minY
    val y = (if (up) flipTop else anchor.top).coerceIn(minY, maxY)
    return MenuPlacement(x, y, up)
}

/**
 * The visible part of a menu of [width] x [height] at [progress] (0 = just the button, 1 = whole menu).
 * [anchor] is the button's bounds in the menu's own coordinates; it is clamped inside the menu so a menu
 * that was nudged by the screen edge still grows from where its button is.
 */
internal fun revealRect(progress: Float, anchor: Rect, width: Float, height: Float): Rect {
    val p = progress.coerceIn(0f, 1f)
    val startLeft = anchor.left.coerceIn(0f, width)
    val startTop = anchor.top.coerceIn(0f, height)
    val startRight = anchor.right.coerceIn(startLeft, width)
    val startBottom = anchor.bottom.coerceIn(startTop, height)
    return Rect(
        left = lerp(startLeft, 0f, p),
        top = lerp(startTop, 0f, p),
        right = lerp(startRight, width, p),
        bottom = lerp(startBottom, height, p),
    )
}

/**
 * Progress 0..1 of the item at [index] while the menu is at [progress]. Items start after the surface has
 * begun to open and follow each other by a small step, so the sequence is felt rather than seen.
 */
internal fun menuItemProgress(progress: Float, index: Int, count: Int): Float {
    if (count <= 0) return 1f
    val step = if (count > 1) min(MAX_ITEM_STEP, STAGGER_SPAN / (count - 1)) else 0f
    val start = ITEM_START + index * step
    return ((progress - start) / ITEM_WINDOW).coerceIn(0f, 1f)
}

/** Fill opacity of the surface: it appears as the surface leaves the button's outline. */
internal fun menuFillAlpha(progress: Float): Float = (progress / FILL_FULL_AT).coerceIn(0f, 1f)

private const val ITEM_START = 0.25f
private const val STAGGER_SPAN = 0.25f
private const val MAX_ITEM_STEP = 0.05f
private const val ITEM_WINDOW = 0.45f
private const val FILL_FULL_AT = 0.35f
