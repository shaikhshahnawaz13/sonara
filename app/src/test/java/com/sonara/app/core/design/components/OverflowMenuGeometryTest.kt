package com.sonara.app.core.design.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverflowMenuGeometryTest {

    private val window = IntSize(1080, 2400)
    private val menu = IntSize(560, 480)
    private val margin = 24
    private val top = 96
    private val bottom = 144

    private fun place(anchor: IntRect, rtl: Boolean = false, content: IntSize = menu, win: IntSize = window) =
        resolveMenuPlacement(anchor, win, content, margin, top, bottom, rtl)

    private fun button(right: Int, topY: Int) = IntRect(right - 144, topY, right, topY + 144)

    @Test
    fun menuCoversItsButtonAndAlignsToTheButtonsEnd() {
        val anchor = button(right = 1056, topY = 800)
        val p = place(anchor)
        assertEquals(anchor.top, p.y)
        assertEquals(anchor.right - menu.width, p.x)
        assertFalse(p.opensUpward)
    }

    @Test
    fun followsTheButtonDownTheScreen() {
        val a = place(button(1056, 400))
        val b = place(button(1056, 1200))
        assertEquals(400, a.y)
        assertEquals(1200, b.y)
    }

    @Test
    fun opensAboveWhenThereIsNoRoomBelow() {
        val anchor = button(1056, 2000)
        val p = place(anchor)
        assertTrue(p.opensUpward)
        assertEquals(anchor.bottom - menu.height, p.y)
    }

    @Test
    fun exactlyFittingBelowStaysBelow() {
        val maxY = window.height - bottom - margin - menu.height
        val p = place(IntRect(912, maxY, 1056, maxY + 144))
        assertFalse(p.opensUpward)
        assertEquals(maxY, p.y)
    }

    @Test
    fun neverLeavesTheUsableWindow() {
        for (y in listOf(0, 50, 300, 1500, 1900, 2100, 2250, 2400)) {
            for (right in listOf(144, 500, 1080)) {
                val p = place(button(right, y))
                assertTrue(p.x >= margin)
                assertTrue(p.x + menu.width <= window.width - margin)
                assertTrue("top y=$y was ${p.y}", p.y >= top + margin)
                assertTrue("bottom y=$y was ${p.y}", p.y + menu.height <= window.height - bottom - margin)
            }
        }
    }

    @Test
    fun aButtonNearTheLeftEdgeShiftsTheMenuRightInsteadOfClipping() {
        val p = place(button(right = 200, topY = 800))
        assertEquals(margin, p.x)
    }

    @Test
    fun rtlAlignsTheMenuToTheButtonsLeftEdge() {
        val anchor = IntRect(24, 800, 168, 944)
        assertEquals(24, place(anchor, rtl = true).x)
    }

    @Test
    fun aMenuTallerThanTheUsableWindowIsPinnedToTheTop() {
        val tiny = IntSize(1080, 600)
        val p = place(button(1056, 300), content = IntSize(560, 900), win = tiny)
        assertEquals(top + margin, p.y)
    }

    @Test
    fun sameInputsGiveTheSamePlacement() {
        val anchor = button(1056, 1234)
        assertEquals(place(anchor), place(anchor))
    }

    // ---- reveal ----

    private val anchorInMenu = Rect(416f, 0f, 560f, 144f)

    @Test
    fun revealStartsAsTheButtonAndEndsAsTheWholeMenu() {
        val start = revealRect(0f, anchorInMenu, 560f, 480f)
        assertEquals(anchorInMenu, start)
        val end = revealRect(1f, anchorInMenu, 560f, 480f)
        assertEquals(Rect(0f, 0f, 560f, 480f), end)
    }

    @Test
    fun revealGrowsMonotonicallyAndStaysInsideTheMenu() {
        var prev = revealRect(0f, anchorInMenu, 560f, 480f)
        for (i in 1..50) {
            val r = revealRect(i / 50f, anchorInMenu, 560f, 480f)
            assertTrue(r.left <= prev.left && r.top <= prev.top && r.right >= prev.right && r.bottom >= prev.bottom)
            assertTrue(r.left >= 0f && r.top >= 0f && r.right <= 560f && r.bottom <= 480f)
            prev = r
        }
    }

    @Test
    fun revealOfAFlippedMenuGrowsUpwardFromTheButtonAtItsBottom() {
        val bottomButton = Rect(416f, 336f, 560f, 480f)
        val half = revealRect(0.5f, bottomButton, 560f, 480f)
        assertEquals(480f, half.bottom, 0.001f)
        assertTrue(half.top < 336f && half.top > 0f)
    }

    @Test
    fun revealClampsAButtonThatSitsOutsideTheMenu() {
        val outside = Rect(-100f, -50f, 40f, 90f)
        val r = revealRect(0f, outside, 560f, 480f)
        assertTrue(r.left >= 0f && r.top >= 0f && r.right >= r.left && r.bottom >= r.top)
    }

    @Test
    fun revealClampsProgressOutsideZeroToOne() {
        assertEquals(revealRect(1f, anchorInMenu, 560f, 480f), revealRect(1.4f, anchorInMenu, 560f, 480f))
        assertEquals(revealRect(0f, anchorInMenu, 560f, 480f), revealRect(-0.3f, anchorInMenu, 560f, 480f))
    }

    // ---- items ----

    @Test
    fun itemsAreHiddenAtTheStartAndFullyShownAtTheEnd() {
        for (count in listOf(1, 2, 5, 7)) {
            for (i in 0 until count) {
                assertEquals(0f, menuItemProgress(0f, i, count), 0f)
                assertEquals("count=$count i=$i", 1f, menuItemProgress(1f, i, count), 0f)
            }
        }
    }

    @Test
    fun itemsAppearInOrderWithoutExcessiveStagger() {
        val count = 7
        val p = 0.6f
        val values = (0 until count).map { menuItemProgress(p, it, count) }
        assertEquals(values, values.sortedDescending())
        // The last item starts before half-way and the whole sequence is done before the surface settles.
        assertTrue(menuItemProgress(0.5f, count - 1, count) >= 0f)
        assertEquals(1f, menuItemProgress(0.97f, count - 1, count), 0f)
    }

    @Test
    fun noItemsMeansNothingToWaitFor() {
        assertEquals(1f, menuItemProgress(0f, 0, 0), 0f)
    }

    @Test
    fun surfaceFillAppearsEarlyAndSettlesFullyOpaque() {
        assertEquals(0f, menuFillAlpha(0f), 0f)
        assertEquals(1f, menuFillAlpha(1f), 0f)
        assertTrue(menuFillAlpha(0.2f) in 0.01f..0.99f)
    }
}
