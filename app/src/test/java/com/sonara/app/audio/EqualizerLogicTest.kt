package com.sonara.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerLogicTest {
    private val profile = EqualizerProfile(
        bandCentresMilliHz = listOf(60_000, 230_000, 910_000, 3_600_000, 14_000_000),
        minLevelMb = -1500,
        maxLevelMb = 1500,
        presets = listOf(
            EqualizerPreset("Normal", listOf(300, 0, 0, 0, 300)),
            EqualizerPreset("Flat", listOf(0, 0, 0, 0, 0)),
            EqualizerPreset("Rock", listOf(500, 300, -200, 200, 500)),
        ),
    )

    // ---- labels ----

    @Test
    fun frequencyLabels() {
        assertEquals("60 Hz", EqualizerLogic.formatFrequency(60_000))
        assertEquals("910 Hz", EqualizerLogic.formatFrequency(910_000))
        assertEquals("1 kHz", EqualizerLogic.formatFrequency(1_000_000))
        assertEquals("3.6 kHz", EqualizerLogic.formatFrequency(3_600_000))
        assertEquals("14 kHz", EqualizerLogic.formatFrequency(14_000_000))
        assertEquals("4 kHz", EqualizerLogic.formatFrequency(3_960_000))
        assertEquals("10 kHz", EqualizerLogic.formatFrequency(9_970_000))
    }

    @Test
    fun gainLabels() {
        assertEquals("0 dB", EqualizerLogic.formatGain(0))
        assertEquals("+3 dB", EqualizerLogic.formatGain(300))
        assertEquals("+3.5 dB", EqualizerLogic.formatGain(350))
        assertEquals("-2 dB", EqualizerLogic.formatGain(-200))
        assertEquals("-12.5 dB", EqualizerLogic.formatGain(-1250))
        assertEquals("+3.25 dB", EqualizerLogic.formatGain(325))
        assertEquals("+15 dB", EqualizerLogic.formatGain(1500))
    }

    // ---- slider maths ----

    @Test
    fun snapKeepsHalfDecibelStepsInsideTheRange() {
        assertEquals(0, EqualizerLogic.snap(10, -1500, 1500))
        assertEquals(50, EqualizerLogic.snap(30, -1500, 1500))
        assertEquals(1500, EqualizerLogic.snap(9_999, -1500, 1500))
        assertEquals(-1500, EqualizerLogic.snap(-9_999, -1500, 1500))
    }

    @Test
    fun fractionAndLevelRoundTrip() {
        assertEquals(0f, EqualizerLogic.fractionOf(-1500, -1500, 1500), 0f)
        assertEquals(0.5f, EqualizerLogic.fractionOf(0, -1500, 1500), 0f)
        assertEquals(1f, EqualizerLogic.fractionOf(1500, -1500, 1500), 0f)
        assertEquals(0, EqualizerLogic.levelFromFraction(0.5f, -1500, 1500))
        assertEquals(1500, EqualizerLogic.levelFromFraction(1.4f, -1500, 1500))
        assertEquals(-1500, EqualizerLogic.levelFromFraction(-0.4f, -1500, 1500))
    }

    @Test
    fun degenerateRangeDoesNotDivideByZero() {
        assertEquals(0f, EqualizerLogic.fractionOf(0, 0, 0), 0f)
    }

    // ---- levels, presets, custom ----

    @Test
    fun storedLevelsAreClampedAndUsedWhenTheBandCountMatches() {
        val levels = EqualizerLogic.resolveLevels(listOf(5000, -5000, 0, 100, 200), 5, -1500, 1500)
        assertEquals(listOf(1500, -1500, 0, 100, 200), levels)
    }

    @Test
    fun levelsFromOtherHardwareFallBackToFlat() {
        assertEquals(List(5) { 0 }, EqualizerLogic.resolveLevels(listOf(1, 2, 3), 5, -1500, 1500))
        assertEquals(List(5) { 0 }, EqualizerLogic.resolveLevels(emptyList(), 5, -1500, 1500))
    }

    @Test
    fun flatIsClampedIntoARangeThatExcludesZero() {
        assertEquals(listOf(100, 100), EqualizerLogic.resolveLevels(emptyList(), 2, 100, 500))
    }

    @Test
    fun movingABandMakesTheCurveCustomAndKeepsTheOthers() {
        val start = EqualizerLogic.withPreset(EqualizerSettings(enabled = true), profile, "Rock")!!
        val moved = EqualizerLogic.withBandLevel(start, profile, band = 2, levelMb = 700)
        assertNull(moved.presetName)
        assertEquals(listOf(500, 300, 700, 200, 500), moved.bandLevelsMb)
        assertTrue(moved.enabled)
    }

    @Test
    fun bandLevelIsSnappedAndClamped() {
        val s = EqualizerLogic.withBandLevel(EqualizerSettings(), profile, 0, 9_999)
        assertEquals(1500, s.bandLevelsMb[0])
        val t = EqualizerLogic.withBandLevel(EqualizerSettings(), profile, 1, 120)
        assertEquals(100, t.bandLevelsMb[1])
    }

    @Test
    fun aBandThatDoesNotExistChangesNothing() {
        val s = EqualizerSettings(enabled = true, bandLevelsMb = listOf(1, 2, 3, 4, 5))
        assertEquals(s, EqualizerLogic.withBandLevel(s, profile, 5, 100))
        assertEquals(s, EqualizerLogic.withBandLevel(s, profile, -1, 100))
    }

    @Test
    fun selectingAPresetAppliesItsCurveAndName() {
        val s = EqualizerLogic.withPreset(EqualizerSettings(enabled = true), profile, "Rock")!!
        assertEquals("Rock", s.presetName)
        assertEquals(listOf(500, 300, -200, 200, 500), s.bandLevelsMb)
    }

    @Test
    fun aPresetTheDeviceDoesNotHaveIsRefused() {
        assertNull(EqualizerLogic.withPreset(EqualizerSettings(), profile, "Classical"))
    }

    @Test
    fun resetNamesTheDevicePresetWithAFlatCurveOtherwiseIsCustom() {
        val s = EqualizerLogic.flat(EqualizerSettings(bandLevelsMb = listOf(1, 2, 3, 4, 5)), profile)
        assertEquals(List(5) { 0 }, s.bandLevelsMb)
        assertEquals("Flat", s.presetName)

        val noFlat = profile.copy(presets = listOf(EqualizerPreset("Rock", listOf(500, 300, -200, 200, 500))))
        assertNull(EqualizerLogic.flat(EqualizerSettings(), noFlat).presetName)
    }

    @Test
    fun aSavedPresetThisDeviceLacksShowsAsCustom() {
        val saved = EqualizerSettings(presetName = "Classical", bandLevelsMb = List(5) { 0 })
        assertNull(EqualizerLogic.effectivePreset(saved, profile))
        assertEquals("Rock", EqualizerLogic.effectivePreset(saved.copy(presetName = "Rock"), profile))
        assertNull(EqualizerLogic.effectivePreset(EqualizerSettings(), profile))
    }

    // ---- persistence encoding ----

    @Test
    fun levelsEncodeAndDecodeLosslessly() {
        val levels = listOf(-1500, 0, 50, 1500, -350)
        assertEquals(levels, EqualizerLogic.decodeLevels(EqualizerLogic.encodeLevels(levels)))
    }

    @Test
    fun damagedOrMissingLevelsDecodeToEmpty() {
        assertEquals(emptyList<Int>(), EqualizerLogic.decodeLevels(null))
        assertEquals(emptyList<Int>(), EqualizerLogic.decodeLevels(""))
        assertEquals(emptyList<Int>(), EqualizerLogic.decodeLevels("100,abc,200"))
    }

    // ---- status line ----

    @Test
    fun statusFollowsSupportEnabledAndLink() {
        val supported = EqualizerSupport.Supported(profile)
        fun state(enabled: Boolean, link: EffectLink, support: EqualizerSupport = supported, loaded: Boolean = true) =
            EqualizerUiState(loaded = loaded, support = support, enabled = enabled, link = link)

        assertEquals("CHECKING", EqualizerLogic.statusText(state(true, EffectLink.Attached, loaded = false)))
        assertEquals("CHECKING", EqualizerLogic.statusText(state(true, EffectLink.Attached, EqualizerSupport.Checking)))
        assertEquals("NOT SUPPORTED", EqualizerLogic.statusText(state(true, EffectLink.Attached, EqualizerSupport.Unsupported("x"))))
        assertEquals("OFF", EqualizerLogic.statusText(state(false, EffectLink.Idle)))
        assertEquals("ACTIVE", EqualizerLogic.statusText(state(true, EffectLink.Attached)))
        assertEquals("ANOTHER APP HAS CONTROL", EqualizerLogic.statusText(state(true, EffectLink.ControlLost)))
        assertEquals("NOT AVAILABLE ON THIS OUTPUT", EqualizerLogic.statusText(state(true, EffectLink.Failed)))
        assertEquals("WAITING FOR PLAYBACK", EqualizerLogic.statusText(state(true, EffectLink.Idle)))
        assertEquals("WAITING FOR PLAYBACK", EqualizerLogic.statusText(state(true, EffectLink.NoPlayer)))
    }

    @Test
    fun aDisabledEqualizerNeverClaimsToBeActive() {
        assertEquals(
            "OFF",
            EqualizerLogic.statusText(
                EqualizerUiState(loaded = true, support = EqualizerSupport.Supported(profile), enabled = false, link = EffectLink.Attached),
            ),
        )
    }
}
