package com.sonara.app.core.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSizes
import kotlin.math.roundToInt

/**
 * Where the mini-player and the navigation bar sit, as functions of one number: [progress], 1 with the bar
 * fully shown and 0 with it fully retracted below the screen edge. Pure integer arithmetic so it can be tested.
 *
 * Coordinates are in the chrome's own space: y = 0 is its top, y = [height] is the bottom of the screen.
 *
 *  - [navHeight] is the bar's full height including the system navigation inset below its labels;
 *    [navContentHeight] is the same without that inset, so the inset is the difference.
 *  - The bar's top edge travels from `height - navHeight` (shown) to `height` (retracted).
 *  - The mini-player's bottom edge travels with the bar's content, from `height - navHeight` to
 *    `height - inset`, which is where it has always sat on screens without the bar.
 *
 * Neither position depends on [reserved], so the layout space changing hands once at either end of the
 * motion never moves anything on screen.
 */
internal object BottomChromeGeometry {

    /** The layout space the chrome asks for. Only the shown, settled bar takes space for itself. */
    fun height(reserved: Boolean, miniHeight: Int, navHeight: Int, navContentHeight: Int): Int = when {
        reserved -> miniHeight + navHeight
        miniHeight > 0 -> miniHeight + (navHeight - navContentHeight).coerceAtLeast(0)
        else -> 0
    }

    fun navTop(height: Int, navHeight: Int, progress: Float): Int =
        height - (navHeight * progress.coerceIn(0f, 1f)).roundToInt()

    fun miniTop(height: Int, miniHeight: Int, navHeight: Int, navContentHeight: Int, progress: Float): Int {
        val inset = (navHeight - navContentHeight).coerceAtLeast(0)
        return height - inset - (navContentHeight * progress.coerceIn(0f, 1f)).roundToInt() - miniHeight
    }
}

/**
 * The state behind the bar's retract and emerge. It never recreates the bar; it only moves it.
 *
 * [emerged] is false from the moment the bar starts to retract until it has finished coming back. The bar
 * reserves layout space only while a tab screen is showing and [emerged] is true, so:
 *  - on opening a screen the space is released at once, and the new screen is laid out at its final size while
 *    the bar still covers its bottom edge on the way out;
 *  - on returning, the space is taken back only after the bar is fully in place, so nothing is laid out twice
 *    while it moves.
 */
@Stable
class BottomChromeState(shown: Boolean) {
    val progress = Animatable(if (shown) 1f else 0f)

    var emerged: Boolean by mutableStateOf(shown)
        private set

    suspend fun retract(spec: FiniteAnimationSpec<Float>) {
        emerged = false
        progress.animateTo(0f, spec)
    }

    suspend fun emerge(spec: FiniteAnimationSpec<Float>) {
        progress.animateTo(1f, spec)
        emerged = true
    }
}

private const val MINI = "mini"
private const val NAV = "nav"

/**
 * Lays out the mini-player above the navigation bar and moves both with [BottomChromeState.progress]. The
 * progress is read only while placing, so a running motion re-places two children and recomposes nothing;
 * each child sits in its own layer, so its content is not redrawn as it moves.
 *
 * While the bar is not reserved (a deeper screen is open, or it is still moving) it is hidden from
 * accessibility, since it is not something a person can use.
 */
@Composable
internal fun SonaraBottomChrome(
    state: BottomChromeState,
    reserved: Boolean,
    mini: @Composable () -> Unit,
    nav: @Composable () -> Unit,
) {
    Layout(
        content = {
            Box(Modifier.layoutId(MINI).graphicsLayer { }) { mini() }
            Box(
                Modifier
                    .layoutId(NAV)
                    .graphicsLayer { }
                    .then(if (reserved) Modifier else Modifier.clearAndSetSemantics { }),
            ) { nav() }
        },
    ) { measurables, constraints ->
        val loose = Constraints(minWidth = 0, maxWidth = constraints.maxWidth, minHeight = 0, maxHeight = constraints.maxHeight)
        val miniP = measurables.first { it.layoutId == MINI }.measure(loose)
        val navP = measurables.first { it.layoutId == NAV }.measure(loose)
        val navContent = (SonaraBorders.hairline.roundToPx() + SonaraSizes.bottomNavHeight.roundToPx()).coerceAtMost(navP.height)
        val height = BottomChromeGeometry.height(reserved, miniP.height, navP.height, navContent)
        layout(constraints.maxWidth, height) {
            val p = state.progress.value
            miniP.place(0, BottomChromeGeometry.miniTop(height, miniP.height, navP.height, navContent, p))
            navP.place(0, BottomChromeGeometry.navTop(height, navP.height, p))
        }
    }
}
