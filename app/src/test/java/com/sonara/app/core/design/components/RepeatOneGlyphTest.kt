package com.sonara.app.core.design.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatOneGlyphTest {
    @Test
    fun glyphBoundsAreCentredOnTheLoop() {
        val cx = (RepeatOneGlyph.minX + RepeatOneGlyph.maxX) / 2f
        val cy = (RepeatOneGlyph.minY + RepeatOneGlyph.maxY) / 2f
        assertEquals(RepeatOneGlyph.CENTER_X, cx, 0.001f)
        assertEquals(RepeatOneGlyph.CENTER_Y, cy, 0.001f)
    }

    @Test
    fun loopCentreIsTheViewportCentre() {
        assertEquals(12f, RepeatOneGlyph.CENTER_X, 0f)
        assertEquals(12f, RepeatOneGlyph.CENTER_Y, 0f)
    }

    @Test
    fun glyphStaysInsideTheLoopsInnerSpace() {
        // The loop's horizontal lines are at y=8 and y=16; the glyph must clear them by more than the stroke.
        assertTrue(RepeatOneGlyph.minY > 8f + 1f)
        assertTrue(RepeatOneGlyph.maxY < 16f - 1f)
    }
}
