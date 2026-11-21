package com.sonara.app.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerGeometryTest {

    private val d = 3f
    private val w = 1080f
    private val h = 2400f
    private val insetTop = 72f
    private val insetBottom = 48f
    private val miniBounds = floatArrayOf(0f, 2100f, 1080f, 2271f)

    private fun portrait(width: Float = w, height: Float = h, bounds: FloatArray? = miniBounds) =
        PlayerGeometry.compute(width, height, insetTop, insetBottom, d, bounds)

    private fun FloatArray.rect(slot: PlayerSlot): FloatArray = copyOfRange(slot.ordinal * 4, slot.ordinal * 4 + 4)
    private fun FloatArray.l(s: PlayerSlot) = this[s.ordinal * 4]
    private fun FloatArray.t(s: PlayerSlot) = this[s.ordinal * 4 + 1]
    private fun FloatArray.r(s: PlayerSlot) = this[s.ordinal * 4 + 2]
    private fun FloatArray.b(s: PlayerSlot) = this[s.ordinal * 4 + 3]

    @Test
    fun fullPlayerArtworkIsASquareInsideTheWindowAndTheMargins() {
        val f = portrait().full
        val art = PlayerSlot.Art
        assertEquals(f.r(art) - f.l(art), f.b(art) - f.t(art), 0.01f)
        assertTrue(f.l(art) >= PlayerGeometry.PAD * d - 0.01f)
        assertTrue(f.r(art) <= w - PlayerGeometry.PAD * d + 0.01f)
        assertTrue(f.t(art) >= insetTop)
    }

    @Test
    fun theFullPlayerStacksWithoutOverlap() {
        val f = portrait().full
        val order = listOf(
            PlayerSlot.Collapse, PlayerSlot.Art, PlayerSlot.Title, PlayerSlot.Artist, PlayerSlot.Album,
            PlayerSlot.Seek, PlayerSlot.TimeStart, PlayerSlot.Play,
        )
        for (i in 0 until order.lastIndex) {
            val above = order[i]
            val below = order[i + 1]
            assertTrue("$above should end before $below", f.b(above) <= f.t(below) + 0.01f)
        }
        assertTrue(f.b(PlayerSlot.Play) <= h - insetBottom)
    }

    @Test
    fun theTransportRowNeverOverlapsAnywhereFromNarrowToWide() {
        for (widthDp in listOf(320f, 360f, 412f, 600f)) {
            val f = portrait(width = widthDp * d).full
            assertTrue("shuffle/prev $widthDp", f.r(PlayerSlot.Shuffle) <= f.l(PlayerSlot.Previous) + 0.01f)
            assertTrue("prev/play $widthDp", f.r(PlayerSlot.Previous) <= f.l(PlayerSlot.Play) + 0.01f)
            assertTrue("play/next $widthDp", f.r(PlayerSlot.Play) <= f.l(PlayerSlot.Next) + 0.01f)
            assertTrue("next/repeat $widthDp", f.r(PlayerSlot.Next) <= f.l(PlayerSlot.Repeat) + 0.01f)
        }
    }

    @Test
    fun theTopBarKeepsItsThreePartsApart() {
        val f = portrait().full
        assertTrue(f.r(PlayerSlot.Collapse) <= f.l(PlayerSlot.Label))
        assertTrue(f.r(PlayerSlot.Label) <= f.l(PlayerSlot.Queue))
    }

    @Test
    fun theTitleLeavesRoomForTheFavouriteButton() {
        val f = portrait().full
        assertTrue(f.r(PlayerSlot.Title) <= f.l(PlayerSlot.Favourite))
        assertTrue(f.r(PlayerSlot.Favourite) <= w)
    }

    @Test
    fun everyFullPlayerElementIsInsideTheWindow() {
        val f = portrait().full
        for (slot in PlayerSlot.entries) {
            assertTrue("$slot left", f.l(slot) >= 0f)
            assertTrue("$slot top", f.t(slot) >= 0f)
            assertTrue("$slot right", f.r(slot) <= w)
            assertTrue("$slot bottom", f.b(slot) <= h)
        }
    }

    @Test
    fun miniSlotsFollowTheMiniPlayersRealBounds() {
        val s = portrait()
        val m = s.mini
        val cy = (miniBounds[1] + PlayerGeometry.MINI_BORDER * d + miniBounds[3]) / 2f
        assertEquals(PlayerGeometry.MINI_ART * d, m.r(PlayerSlot.Art) - m.l(PlayerSlot.Art), 0.01f)
        assertEquals(PlayerGeometry.MINI_ART * d, m.b(PlayerSlot.Art) - m.t(PlayerSlot.Art), 0.01f)
        assertEquals(cy, (m.t(PlayerSlot.Art) + m.b(PlayerSlot.Art)) / 2f, 0.01f)
        assertEquals(miniBounds[0] + 12f * d, m.l(PlayerSlot.Art), 0.01f)
        assertEquals(miniBounds[2] - 4f * d, m.r(PlayerSlot.Next), 0.01f)
        assertEquals(m.l(PlayerSlot.Next), m.r(PlayerSlot.Play), 0.01f)
        assertEquals(miniBounds[0] + 64f * d, m.l(PlayerSlot.Title), 0.01f)
        assertEquals(m.b(PlayerSlot.Title), m.t(PlayerSlot.Artist), 0.01f)
        assertTrue(m.r(PlayerSlot.Title) <= m.l(PlayerSlot.Play))
    }

    @Test
    fun theSurfaceGrowsFromTheMiniPlayerToTheWholeWindow() {
        val s = portrait()
        assertEquals(miniBounds.toList(), s.surfaceMini.toList())
        assertEquals(listOf(0f, 0f, w, h), s.surfaceFull.toList())
    }

    @Test
    fun elementsThatOnlyExistInTheFullPlayerStayPut() {
        val s = portrait()
        for (slot in PlayerSlot.entries.filter { it.ordinal >= PlayerSlot.sharedCount }) {
            assertEquals(s.full.rect(slot).toList(), s.mini.rect(slot).toList())
        }
    }

    @Test
    fun withoutKnownBoundsTheMiniPlayerIsAssumedAboveTheBottomEdge() {
        val s = portrait(bounds = null)
        assertEquals(0f, s.surfaceMini[0])
        assertEquals(w, s.surfaceMini[2])
        assertTrue(s.surfaceMini[3] <= h - insetBottom)
        assertTrue(s.surfaceMini[1] < s.surfaceMini[3])
    }

    @Test
    fun landscapePutsTheArtworkBesideTheControls() {
        val s = PlayerGeometry.compute(2400f, 1080f, 0f, 0f, d, null)
        assertTrue(s.landscape)
        val f = s.full
        assertFalse(portrait().landscape)
        assertTrue(f.r(PlayerSlot.Art) <= f.l(PlayerSlot.Title))
        assertTrue(f.b(PlayerSlot.Art) <= 1080f)
        assertEquals(f.r(PlayerSlot.Art) - f.l(PlayerSlot.Art), f.b(PlayerSlot.Art) - f.t(PlayerSlot.Art), 0.01f)
    }

    @Test
    fun theSameInputsGiveTheSameLayout() {
        assertEquals(portrait().full.toList(), portrait().full.toList())
    }

    // ---- choreography ----

    @Test
    fun interpolationHitsItsEndpoints() {
        assertEquals(10f, PlayerGeometry.lerp(10f, 50f, 0f), 0f)
        assertEquals(50f, PlayerGeometry.lerp(10f, 50f, 1f), 0f)
        assertEquals(30f, PlayerGeometry.lerp(10f, 50f, 0.5f), 0f)
    }

    @Test
    fun revealWindowsStartInOrderAndFinishBeforeTheSurfaceLands() {
        val order = listOf(PlayerSlot.Collapse, PlayerSlot.Album, PlayerSlot.Seek, PlayerSlot.Previous, PlayerSlot.Shuffle)
        val starts = order.map { PlayerGeometry.revealStart(it) }
        assertEquals(starts.sorted(), starts)
        assertTrue(starts.zipWithNext().all { (a, b) -> b > a })
        for (slot in PlayerSlot.entries.filter { it.ordinal >= PlayerSlot.sharedCount }) {
            assertTrue("$slot", PlayerGeometry.revealStart(slot) + PlayerGeometry.REVEAL_LENGTH <= 1f)
            assertTrue("$slot starts after the surface has begun to move", PlayerGeometry.revealStart(slot) > 0.3f)
        }
    }

    @Test
    fun phaseIsClampedAndMonotonic() {
        assertEquals(0f, PlayerGeometry.phase(0.1f, 0.4f, 0.3f), 0f)
        assertEquals(0.5f, PlayerGeometry.phase(0.55f, 0.4f, 0.3f), 1e-5f)
        assertEquals(1f, PlayerGeometry.phase(0.9f, 0.4f, 0.3f), 0f)
        assertEquals(0f, PlayerGeometry.phase(0.3f, 0.4f, 0f), 0f)
        assertEquals(1f, PlayerGeometry.phase(0.5f, 0.4f, 0f), 0f)
    }

    @Test
    fun theTextScalesMatchTheTypeSizes() {
        assertEquals(16f / 28f, PlayerGeometry.MINI_TITLE_SCALE, 1e-6f)
        assertEquals(12f / 16f, PlayerGeometry.MINI_ARTIST_SCALE, 1e-6f)
    }

    // ---- seeking ----

    @Test
    fun aTouchOnTheSeekBarMapsToAFraction() {
        assertEquals(0.5f, PlayerGeometry.seekFraction(500f, 1000f), 0f)
        assertEquals(0f, PlayerGeometry.seekFraction(-40f, 1000f), 0f)
        assertEquals(1f, PlayerGeometry.seekFraction(1400f, 1000f), 0f)
        assertEquals(0f, PlayerGeometry.seekFraction(10f, 0f), 0f)
    }

    @Test
    fun aFractionMapsToAPlaybackPosition() {
        assertEquals(90_000L, PlayerGeometry.positionForFraction(0.5f, 180_000L))
        assertEquals(0L, PlayerGeometry.positionForFraction(0f, 180_000L))
        assertEquals(180_000L, PlayerGeometry.positionForFraction(1f, 180_000L))
        assertEquals(180_000L, PlayerGeometry.positionForFraction(3f, 180_000L))
        assertEquals(0L, PlayerGeometry.positionForFraction(0.5f, 0L))
        assertEquals(0L, PlayerGeometry.positionForFraction(0.5f, -5L))
    }

    @Test
    fun progressIsThePlayedFractionAndStaysInRange() {
        assertEquals(0.25f, PlayerGeometry.fractionOf(45_000L, 180_000L), 1e-6f)
        assertEquals(0f, PlayerGeometry.fractionOf(1000L, 0L), 0f)
        assertEquals(1f, PlayerGeometry.fractionOf(200_000L, 180_000L), 0f)
        assertEquals(0f, PlayerGeometry.fractionOf(-1L, 180_000L), 0f)
    }

    @Test
    fun seekingThereAndBackIsConsistent() {
        val duration = 241_000L
        for (px in listOf(0f, 133f, 500f, 999f, 1000f)) {
            val ms = PlayerGeometry.positionForFraction(PlayerGeometry.seekFraction(px, 1000f), duration)
            val back = PlayerGeometry.fractionOf(ms, duration)
            assertEquals(px / 1000f, back, 0.001f)
        }
    }
}
