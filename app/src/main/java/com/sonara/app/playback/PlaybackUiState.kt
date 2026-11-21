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

    /** The repeat button is lit for any mode but Off. */
    val isActive: Boolean get() = this != Off

    /** Repeat One draws a "1" inside the loop; the other modes use the plain loop. */
    val showsOneGlyph: Boolean get() = this == One

    /** What a screen reader says about the button. */
    val contentDescription: String
        get() = when (this) {
            Off -> "Repeat off"
            All -> "Repeat all"
            One -> "Repeat one"
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
    /** Position of the current track in the queue (0-based); -1 when there is none. */
    val queueIndex: Int = -1,
    /** The player is meant to be playing (play pressed) even if it is buffering right now. */
    val playWhenReady: Boolean = false,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val lastError: String? = null,
) {
    val hasMedia: Boolean get() = currentTrackId != null

    /** Play was pressed and the player is still loading data: the button should already offer "pause". */
    val isWaitingToPlay: Boolean get() = playWhenReady && isBuffering

    /** Whether the play/pause button shows "pause". Same rule the controller uses to decide what a tap does. */
    val showsPause: Boolean get() = isPlaying || isWaitingToPlay
}

/** One row of the queue as the UI shows it. */
data class QueueEntry(val trackId: Long?, val title: String, val artist: String)
