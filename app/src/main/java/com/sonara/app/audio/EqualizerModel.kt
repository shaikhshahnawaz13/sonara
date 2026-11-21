package com.sonara.app.audio

import java.util.Locale

/** One preset the device's equalizer offers, with the band levels (millibels) it sets. */
data class EqualizerPreset(val name: String, val levelsMb: List<Int>)

/**
 * What this device's equalizer offers, read from the device itself: the number of bands, their centre
 * frequencies (milliHertz, as Android reports them), the gain range in millibels (1 dB = 100 mB) and the presets.
 * Nothing here is assumed; a control exists only for what the device reports.
 */
data class EqualizerProfile(
    val bandCentresMilliHz: List<Int>,
    val minLevelMb: Int,
    val maxLevelMb: Int,
    val presets: List<EqualizerPreset>,
) {
    val bandCount: Int get() = bandCentresMilliHz.size
}

/**
 * What the user chose, as persisted. [presetName] null means a custom curve. [bandLevelsMb] empty means "not set
 * yet" (flat). Presets are stored by name, never by index, because indices can differ between devices.
 */
data class EqualizerSettings(
    val enabled: Boolean = false,
    val presetName: String? = null,
    val bandLevelsMb: List<Int> = emptyList(),
)

sealed interface EqualizerSupport {
    /** The device has not been asked yet. */
    data object Checking : EqualizerSupport

    /** The device offers no usable equalizer; [reason] is shown as it is. */
    data class Unsupported(val reason: String) : EqualizerSupport

    data class Supported(val profile: EqualizerProfile) : EqualizerSupport
}

/** How the effect is connected to the playing audio right now. */
enum class EffectLink {
    /** No player exists (the playback service is not running). */
    NoPlayer,

    /** Nothing is attached on purpose: the equalizer is off, or the player has no audio session yet. */
    Idle,

    /** The effect exists on the player's audio session and the app controls it. */
    Attached,

    /** Another app with a higher effect priority holds the equalizer. */
    ControlLost,

    /** The effect could not be created or driven on this device or output. */
    Failed,
}

/** Everything the equalizer screen shows. */
data class EqualizerUiState(
    val loaded: Boolean = false,
    val support: EqualizerSupport = EqualizerSupport.Checking,
    val enabled: Boolean = false,
    /** Null means a custom curve. */
    val presetName: String? = null,
    /** One level per band, in millibels; empty until the device profile is known. */
    val levelsMb: List<Int> = emptyList(),
    val link: EffectLink = EffectLink.NoPlayer,
)

/** The rules of the equalizer that need no Android, so they can be tested directly. */
object EqualizerLogic {
    /** Slider resolution: half a decibel. */
    const val STEP_MB = 50

    fun clamp(levelMb: Int, minMb: Int, maxMb: Int): Int = levelMb.coerceIn(minMb, maxMb)

    /** The level snapped to [STEP_MB] and kept inside the device range. */
    fun snap(levelMb: Int, minMb: Int, maxMb: Int): Int {
        val snapped = Math.round(levelMb.toDouble() / STEP_MB).toInt() * STEP_MB
        return clamp(snapped, minMb, maxMb)
    }

    /** Slider position 0..1 (0 = minimum) for a level. */
    fun fractionOf(levelMb: Int, minMb: Int, maxMb: Int): Float =
        if (maxMb <= minMb) 0f else ((levelMb - minMb).toFloat() / (maxMb - minMb)).coerceIn(0f, 1f)

    /** The snapped level for a slider position 0..1. */
    fun levelFromFraction(fraction: Float, minMb: Int, maxMb: Int): Int =
        snap(minMb + (fraction.coerceIn(0f, 1f) * (maxMb - minMb)).toInt(), minMb, maxMb)

    /**
     * The levels to use for a device with [bandCount] bands: the stored ones, clamped, when there are exactly that
     * many; otherwise flat. A mismatch means the settings came from different hardware, so they are not guessed at.
     */
    fun resolveLevels(stored: List<Int>, bandCount: Int, minMb: Int, maxMb: Int): List<Int> =
        if (stored.size == bandCount) {
            stored.map { clamp(it, minMb, maxMb) }
        } else {
            List(bandCount) { clamp(0, minMb, maxMb) }
        }

    /** Moves one band; the curve becomes custom. A band outside the device's bands changes nothing. */
    fun withBandLevel(settings: EqualizerSettings, profile: EqualizerProfile, band: Int, levelMb: Int): EqualizerSettings {
        if (band !in 0 until profile.bandCount) return settings
        val levels = resolveLevels(settings.bandLevelsMb, profile.bandCount, profile.minLevelMb, profile.maxLevelMb).toMutableList()
        levels[band] = snap(levelMb, profile.minLevelMb, profile.maxLevelMb)
        return settings.copy(presetName = null, bandLevelsMb = levels)
    }

    /** Applies the device preset called [name], or returns null when the device has no such preset. */
    fun withPreset(settings: EqualizerSettings, profile: EqualizerProfile, name: String): EqualizerSettings? {
        val preset = profile.presets.firstOrNull { it.name == name } ?: return null
        val levels = resolveLevels(preset.levelsMb, profile.bandCount, profile.minLevelMb, profile.maxLevelMb)
        return settings.copy(presetName = preset.name, bandLevelsMb = levels)
    }

    /** Every band at 0 dB. If the device has a preset with exactly that curve, it is named; otherwise it is custom. */
    fun flat(settings: EqualizerSettings, profile: EqualizerProfile): EqualizerSettings {
        val levels = List(profile.bandCount) { clamp(0, profile.minLevelMb, profile.maxLevelMb) }
        val match = profile.presets.firstOrNull { it.levelsMb == levels }
        return settings.copy(presetName = match?.name, bandLevelsMb = levels)
    }

    /** The stored preset name, if the device still has such a preset; otherwise null (custom). */
    fun effectivePreset(settings: EqualizerSettings, profile: EqualizerProfile): String? =
        settings.presetName?.takeIf { name -> profile.presets.any { it.name == name } }

    /** "60 Hz", "910 Hz", "1 kHz", "3.6 kHz", "14 kHz" from Android's milliHertz. */
    fun formatFrequency(milliHz: Int): String {
        val hz = milliHz / 1000.0
        if (hz < 1000.0) return "${Math.round(hz)} Hz"
        val khz = hz / 1000.0
        return if (khz >= 10.0 || Math.round(khz * 10) % 10L == 0L) {
            "${Math.round(khz)} kHz"
        } else {
            String.format(Locale.ROOT, "%.1f kHz", khz)
        }
    }

    /** "+3.5 dB", "-2 dB", "0 dB"; up to two decimals when the level is not a whole or half decibel. */
    fun formatGain(levelMb: Int): String {
        if (levelMb == 0) return "0 dB"
        val sign = if (levelMb > 0) "+" else "-"
        val abs = Math.abs(levelMb)
        val text = when {
            abs % 100 == 0 -> (abs / 100).toString()
            abs % 10 == 0 -> String.format(Locale.ROOT, "%.1f", abs / 100.0)
            else -> String.format(Locale.ROOT, "%.2f", abs / 100.0)
        }
        return "$sign$text dB"
    }

    /** What the status line says. */
    fun statusText(state: EqualizerUiState): String = when {
        !state.loaded || state.support is EqualizerSupport.Checking -> "CHECKING"
        state.support is EqualizerSupport.Unsupported -> "NOT SUPPORTED"
        !state.enabled -> "OFF"
        else -> when (state.link) {
            EffectLink.Attached -> "ACTIVE"
            EffectLink.ControlLost -> "ANOTHER APP HAS CONTROL"
            EffectLink.Failed -> "NOT AVAILABLE ON THIS OUTPUT"
            EffectLink.NoPlayer, EffectLink.Idle -> "WAITING FOR PLAYBACK"
        }
    }

    fun encodeLevels(levels: List<Int>): String = levels.joinToString(",")

    /** Empty for missing or damaged text: one bad value discards the whole list rather than shifting bands. */
    fun decodeLevels(text: String?): List<Int> {
        if (text.isNullOrBlank()) return emptyList()
        val parsed = text.split(',').map { it.trim().toIntOrNull() }
        return if (parsed.any { it == null }) emptyList() else parsed.filterNotNull()
    }
}
