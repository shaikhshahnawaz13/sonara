package com.sonara.app.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.animate
import androidx.compose.ui.Modifier
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.first
import androidx.compose.ui.layout.onSizeChanged
import com.sonara.app.core.animation.SonaraMotion
import com.sonara.app.core.design.components.SonaraScreenSurface
import com.sonara.app.feature.folders.FoldersScreen
import com.sonara.app.feature.home.HomeScreen
import com.sonara.app.feature.library.LibraryScreen
import com.sonara.app.feature.playlists.PlaylistsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** How many whole pages a trip from [from] (a fractional page position) to [target] crosses; at least one. */
internal fun tabTravelPages(from: Float, target: Int): Int =
    abs(target - from).roundToInt().coerceAtLeast(1)

/**
 * A tap on the tab the strip is already on, or already travelling to, must not restart the animation.
 * [activeTarget] is the destination of a trip in progress, or null when the strip is at rest.
 */
internal fun shouldIgnoreTabRequest(requested: Int, activeTarget: Int?, currentPage: Int): Boolean =
    requested == (activeTarget ?: currentPage)

/**
 * How many pages beside the visible one stay composed. Before the tabs have settled, one on each side, so
 * start-up and returning from a deeper screen stay cheap. Once settled, every page, so a trip between tabs
 * never has to compose or dispose a page while it is animating.
 */
internal fun pagesKeptComposed(warm: Boolean, pageCount: Int): Int =
    if (warm) (pageCount - 1).coerceAtLeast(1) else 1

/** Frames to wait after the tabs settle before composing the remaining pages, so that work never shares a frame with a transition. */
private const val WARM_UP_FRAMES = 3

/** Below this speed (px/s) a new trip starts from rest, with the normal curve, instead of carrying velocity. */
private const val CARRY_VELOCITY_THRESHOLD = 50f

/**
 * The four tabs as one continuous horizontal strip of pages.
 *
 * Moving between tabs scrolls the strip, so every page between the current one and the destination
 * visibly passes through the viewport. Only the pages near the viewport are composed, and each page
 * keeps its saved state (such as scroll position) while off screen.
 *
 * The bottom-navigation indicator reads [position] from the same scroll offset, so it can never drift
 * out of step with the pages.
 */
@Stable
class SonaraTabsState internal constructor(
    internal val pagerState: PagerState,
    private val scope: CoroutineScope,
) {
    /** Width of one page in pixels, reported by the pager once it has been laid out. */
    internal var pageExtentPx: Int = 0

    private var activeTarget: Int? = null
    private var velocityPxPerSec: Float = 0f

    /** The page the strip is closest to. Changes as the strip passes the half-way point of a page. */
    val selectedPage: Int get() = pagerState.currentPage

    /** Fractional page index of the strip, for example 1.5 half-way between Library and Playlists. */
    val position: Float get() = pagerState.currentPage + pagerState.currentPageOffsetFraction

    /**
     * Travel to [page]. Repeating the current destination does nothing; choosing another one while
     * travelling redirects the strip from where it is, carrying its speed.
     */
    fun select(page: Int, motion: SonaraMotion) {
        val target = page.coerceIn(0, pagerState.pageCount - 1)
        if (shouldIgnoreTabRequest(target, activeTarget, pagerState.currentPage)) return
        activeTarget = target
        scope.launch { travelTo(target, motion) }
    }

    private suspend fun travelTo(target: Int, motion: SonaraMotion) {
        val extent = pageExtentPx
        if (motion.isOff || extent <= 0) {
            velocityPxPerSec = 0f
            pagerState.scrollToPage(target)
            finish(target)
            return
        }

        // A later scroll call of the same priority cancels this one, which is how redirects work.
        pagerState.scroll {
            val distance = (target - position) * extent
            if (abs(distance) >= 0.5f) {
                val carrying = abs(velocityPxPerSec) > CARRY_VELOCITY_THRESHOLD
                val spec = if (carrying) {
                    motion.tabRedirectSpec<Float>()
                } else {
                    motion.tabTravelSpec<Float>(tabTravelPages(position, target))
                }
                var previous = 0f
                animate(
                    initialValue = 0f,
                    targetValue = distance,
                    initialVelocity = if (carrying) velocityPxPerSec else 0f,
                    animationSpec = spec,
                ) { value, velocity ->
                    scrollBy(value - previous)
                    previous = value
                    velocityPxPerSec = velocity
                }
            }
        }

        // Only reached when the trip was not interrupted. Remove any sub-pixel remainder.
        if (pagerState.currentPage != target || abs(pagerState.currentPageOffsetFraction) > 0.0001f) {
            pagerState.scrollToPage(target)
        }
        velocityPxPerSec = 0f
        finish(target)
    }

    private fun finish(target: Int) {
        if (activeTarget == target) activeTarget = null
    }
}

@Composable
fun rememberSonaraTabsState(): SonaraTabsState {
    val pagerState = rememberPagerState(initialPage = 0, initialPageOffsetFraction = 0f) {
        SonaraDestination.primary.size
    }
    val scope = rememberCoroutineScope()
    return remember(pagerState, scope) { SonaraTabsState(pagerState, scope) }
}

/**
 * The pager itself. Swiping is off for now: tabs change by tapping the bottom navigation only.
 * Pages are composed ahead of time (see [pagesKeptComposed]) so a page never pops in mid-travel.
 */
@Composable
internal fun SonaraTabsHost(
    state: SonaraTabsState,
    modifier: Modifier = Modifier,
    /** True while no navigation transition is moving the tabs. Read in a coroutine, never in composition. */
    settled: () -> Boolean = { true },
) {
    var warm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow(settled).first { it }
        repeat(WARM_UP_FRAMES) { withFrameNanos { } }
        warm = true
    }
    HorizontalPager(
        state = state.pagerState,
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { state.pageExtentPx = it.width },
        beyondViewportPageCount = pagesKeptComposed(warm, state.pagerState.pageCount),
        userScrollEnabled = false,
        key = { page -> SonaraDestination.primary[page].route },
    ) { page ->
        SonaraScreenSurface {
            when (SonaraDestination.primary[page]) {
                SonaraDestination.Home -> HomeScreen()
                SonaraDestination.Library -> LibraryScreen()
                SonaraDestination.Playlists -> PlaylistsScreen()
                SonaraDestination.Folders -> FoldersScreen()
                else -> Unit
            }
        }
    }
}
