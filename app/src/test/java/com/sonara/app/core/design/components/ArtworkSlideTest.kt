package com.sonara.app.core.design.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkSlideTest {

    @Test
    fun forwardAndBackDirections() {
        assertEquals(1, ArtworkSlide.direction(previousIndex = 2, newIndex = 3))
        assertEquals(-1, ArtworkSlide.direction(previousIndex = 3, newIndex = 2))
    }

    @Test
    fun unknownOrUnchangedPositionSlidesForward() {
        assertEquals(1, ArtworkSlide.direction(-1, 4))
        assertEquals(1, ArtworkSlide.direction(4, -1))
        assertEquals(1, ArtworkSlide.direction(4, 4))
    }

    @Test
    fun pictureStartsOffToTheSideAndEndsCentred() {
        assertEquals(300f, ArtworkSlide.incomingOffset(0f, 1, 300f), 0f)
        assertEquals(0f, ArtworkSlide.incomingOffset(1f, 1, 300f), 0f)
        assertEquals(-300f, ArtworkSlide.incomingOffset(0f, -1, 300f), 0f)
        assertEquals(0f, ArtworkSlide.outgoingOffset(0f, 1, 300f), 0f)
        assertEquals(-300f, ArtworkSlide.outgoingOffset(1f, 1, 300f), 0f)
        assertEquals(300f, ArtworkSlide.outgoingOffset(1f, -1, 300f), 0f)
    }

    @Test
    fun theTwoPicturesStayExactlyOneWidthApartSoNothingGapsOrOverlaps() {
        for (direction in intArrayOf(1, -1)) {
            for (step in 0..20) {
                val p = step / 20f
                val gap = ArtworkSlide.incomingOffset(p, direction, 480f) - ArtworkSlide.outgoingOffset(p, direction, 480f)
                assertEquals(direction * 480f, gap, 1e-3f)
            }
        }
    }

    @Test
    fun overshootingProgressIsClamped() {
        assertEquals(0f, ArtworkSlide.incomingOffset(1.4f, 1, 100f), 0f)
        assertEquals(-100f, ArtworkSlide.outgoingOffset(1.4f, 1, 100f), 0f)
    }
}
