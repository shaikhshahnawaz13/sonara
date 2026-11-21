package com.sonara.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatPresentationTest {
    @Test
    fun onlyOffIsInactive() {
        assertFalse(RepeatSetting.Off.isActive)
        assertTrue(RepeatSetting.All.isActive)
        assertTrue(RepeatSetting.One.isActive)
    }

    @Test
    fun theOneGlyphShowsOnlyForRepeatOne() {
        assertFalse(RepeatSetting.Off.showsOneGlyph)
        assertFalse(RepeatSetting.All.showsOneGlyph)
        assertTrue(RepeatSetting.One.showsOneGlyph)
    }

    @Test
    fun eachModeHasItsOwnDescription() {
        assertEquals("Repeat off", RepeatSetting.Off.contentDescription)
        assertEquals("Repeat all", RepeatSetting.All.contentDescription)
        assertEquals("Repeat one", RepeatSetting.One.contentDescription)
    }
}
