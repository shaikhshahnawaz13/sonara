package com.sonara.app.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SonaraTabsLogicTest {

    @Test
    fun travelCountsEveryPageCrossed() {
        assertEquals(1, tabTravelPages(from = 0f, target = 1))
        assertEquals(2, tabTravelPages(from = 0f, target = 2))
        assertEquals(3, tabTravelPages(from = 0f, target = 3))
        assertEquals(3, tabTravelPages(from = 3f, target = 0))
        assertEquals(1, tabTravelPages(from = 2f, target = 1))
    }

    @Test
    fun travelFromBetweenPagesRoundsToTheNearestWholePage() {
        assertEquals(2, tabTravelPages(from = 1.4f, target = 3))
        assertEquals(1, tabTravelPages(from = 1.6f, target = 3))
    }

    @Test
    fun travelIsNeverLessThanOnePage() {
        assertEquals(1, tabTravelPages(from = 2f, target = 2))
        assertEquals(1, tabTravelPages(from = 2.2f, target = 2))
    }

    @Test
    fun tappingTheCurrentTabAtRestIsIgnored() {
        assertTrue(shouldIgnoreTabRequest(requested = 1, activeTarget = null, currentPage = 1))
    }

    @Test
    fun tappingAnotherTabAtRestIsAccepted() {
        assertFalse(shouldIgnoreTabRequest(requested = 3, activeTarget = null, currentPage = 0))
    }

    @Test
    fun tappingTheDestinationOfATripInProgressIsIgnored() {
        // Travelling Home to Folders, currently passing Library: a second Folders tap must not restart it.
        assertTrue(shouldIgnoreTabRequest(requested = 3, activeTarget = 3, currentPage = 1))
    }

    @Test
    fun tappingADifferentTabDuringATripRedirectsIt() {
        assertFalse(shouldIgnoreTabRequest(requested = 2, activeTarget = 3, currentPage = 1))
    }

    @Test
    fun tappingTheTabYouJustPassedButAreNotHeadingToRedirectsBackToIt() {
        // Heading to Folders (3), strip currently closest to Library (1): tapping Library must reverse.
        assertFalse(shouldIgnoreTabRequest(requested = 1, activeTarget = 3, currentPage = 1))
    }
}
