package com.sonara.app.playback

import androidx.media3.common.Player

/** Repeat modes as SONARA names them; mapped to and from Media3's constants in one place. */
enum class RepeatSetting(val playerValue: Int) {
    Off(Player.REPEAT_MODE_OFF),
    All(Player.REPEAT_MODE_ALL),
    One(Player.REPEAT_MODE_ONE);

    fun next(): RepeatSetting = when (this) {
        Off -> All
        All -> One
        One -> Off
    }

    companion object {
        fun fromPlayerValue(value: Int): RepeatSetting = entries.firstOrNull { it.playerValue == value } ?: Off
    }
}

/**
 * What the UI may show about playback. Position is deliberately not part of this (it changes several times
 * a second); it has its own flow on [PlaybackController].
 */
data class PlaybackUiState(
    val connected: Boolean = false,
    val currentTrackId: Long? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeat: RepeatSetting = RepeatSetting.Off,
    val queueSize: Int = 0,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val lastError: String? = null,
) {
    val hasMedia: Boolean get() = currentTrackId != null
}
