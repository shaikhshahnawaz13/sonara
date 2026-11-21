package com.sonara.app.core.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/** One entry of an overflow menu. */
class SonaraMenuAction(val label: String, val onClick: () -> Unit)

/** Identifies the menu that is open. A single holder means opening one menu closes any other. */
private object ActiveOverflowMenu {
    var token: Any? by mutableStateOf(null)
}

/**
 * The three-dot button and its menu. Renders nothing when there are no actions.
 *
 * The menu is a popup positioned from the button's real window bounds, so it opens exactly on the button
 * that was tapped wherever that button is on screen, flips above it when there is no room below, and
 * stays inside the usable window. It grows out of the button's outline into its full size and folds back
 * into it when closed; the animation is a single progress value read only while drawing, so opening a
 * menu does not recompose the list it belongs to.
 */
@Composable
fun SonaraOverflowMenu(
    actions: List<SonaraMenuAction>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return
    val token = remember { Any() }
    val isOpen = ActiveOverflowMenu.token === token
    var composed by remember { mutableStateOf(false) }
    // True once the popup window exists and has been positioned (see the note on the opening animation).
    var placed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val motion = SonaraTheme.motion

    LaunchedEffect(isOpen, placed) {
        if (isOpen) {
            composed = true
            if (placed) {
                // Creating the popup window takes several frames. Animations are timed by the clock, not by
                // frames, so starting before the window is up would skip the first part of the movement
                // (the visible stutter). The animation starts only when the window is on screen, on a frame.
                withFrameNanos { }
                progress.animateTo(1f, motion.menuOpenSpec())
            }
        } else if (composed) {
            progress.animateTo(0f, motion.menuCloseSpec())
            composed = false
            placed = false
        }
    }
    DisposableEffect(token) {
        onDispose { if (ActiveOverflowMenu.token === token) ActiveOverflowMenu.token = null }
    }

    // The box is exactly the button, so the popup's anchor is the button and nothing else.
    Box(modifier = modifier) {
        SonaraIconButton(
            icon = SonaraIcons.More,
            contentDescription = contentDescription,
            onClick = { ActiveOverflowMenu.token = token },
        )
        if (composed) {
            OverflowMenuPopup(
                actions = actions,
                progress = progress,
                onPlaced = { placed = true },
                onClose = { if (ActiveOverflowMenu.token === token) ActiveOverflowMenu.token = null },
            )
        }
    }
}

@Composable
private fun OverflowMenuPopup(
    actions: List<SonaraMenuAction>,
    progress: Animatable<Float, *>,
    onPlaced: () -> Unit,
    onClose: () -> Unit,
) {
    val placedCallback by rememberUpdatedState(onPlaced)
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    val density = LocalDensity.current
    val insetTop = WindowInsets.systemBars.getTop(density)
    val insetBottom = WindowInsets.systemBars.getBottom(density)
    val marginPx = with(density) { SonaraSpacing.s8.roundToPx() }
    val provider = remember(insetTop, insetBottom, marginPx) {
        AnchoredMenuPositionProvider(marginPx, insetTop, insetBottom) { placedCallback() }
    }
    val shiftPx = with(density) { ITEM_SHIFT.toPx() }
    val borderPx = with(density) { SonaraBorders.standard.toPx() }
    val count = actions.size

    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onClose,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .widthIn(min = MENU_MIN_WIDTH)
                .drawWithContent {
                    val anchor = provider.anchorInMenu ?: return@drawWithContent
                    val p = progress.value
                    val reveal = revealRect(p, anchor, size.width, size.height)
                    val inset = borderPx / 2f
                    drawRect(colors.surfaceRaised.copy(alpha = menuFillAlpha(p)), topLeft = Offset.Zero, size = size)
                    clipRect(reveal.left, reveal.top, reveal.right, reveal.bottom) {
                        this@drawWithContent.drawContent()
                    }
                    // The outline is the button's outline at progress 0 and the menu's at 1.
                    drawRect(
                        color = colors.border,
                        topLeft = Offset(reveal.left + inset, reveal.top + inset),
                        size = Size(
                            (reveal.width - borderPx).coerceAtLeast(0f),
                            (reveal.height - borderPx).coerceAtLeast(0f),
                        ),
                        style = Stroke(borderPx),
                    )
                },
        ) {
            actions.forEachIndexed { index, action ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val item = menuItemProgress(progress.value, index, count)
                            alpha = item
                            translationY = (1f - item) * shiftPx * (if (provider.opensUpward) 1f else -1f)
                        }
                        .defaultMinSize(minHeight = SonaraSizes.minTouchTarget)
                        .clickable(role = Role.Button) {
                            onClose()
                            action.onClick()
                        }
                        .padding(horizontal = SonaraSpacing.s16),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = action.label,
                        style = typography.microLabel,
                        color = colors.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Positions the menu from the anchor's window bounds (the popup machinery re-asks whenever the anchor moves, so a
 * menu follows its button). It also records where the button is in the menu's own space for the reveal.
 * The recorded values are only read while drawing; they are not Compose state.
 */
private class AnchoredMenuPositionProvider(
    private val margin: Int,
    private val insetTop: Int,
    private val insetBottom: Int,
    private val onFirstPlacement: () -> Unit,
) : PopupPositionProvider {
    private var announced = false

    var anchorInMenu: Rect? = null
        private set
    var opensUpward: Boolean = false
        private set

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val placement = resolveMenuPlacement(
            anchor = anchorBounds,
            window = windowSize,
            content = popupContentSize,
            margin = margin,
            insetTop = insetTop,
            insetBottom = insetBottom,
            rtl = layoutDirection == LayoutDirection.Rtl,
        )
        anchorInMenu = Rect(
            left = (anchorBounds.left - placement.x).toFloat(),
            top = (anchorBounds.top - placement.y).toFloat(),
            right = (anchorBounds.right - placement.x).toFloat(),
            bottom = (anchorBounds.bottom - placement.y).toFloat(),
        )
        opensUpward = placement.opensUpward
        if (!announced) {
            announced = true
            onFirstPlacement()
        }
        return IntOffset(placement.x, placement.y)
    }
}

private val MENU_MIN_WIDTH = 200.dp
private val ITEM_SHIFT = 6.dp
