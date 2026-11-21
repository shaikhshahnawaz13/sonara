package com.sonara.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackUiStateTest {

    private fun state(
        playing: Boolean = false,
        buffering: Boolean = false,
        playWhenReady: Boolean = false,
        track: Long? = 1L,
    ) = PlaybackUiState(
        connected = true,
        currentTrackId = track,
        isPlaying = playing,
        isBuffering = buffering,
        playWhenReady = playWhenReady,
    )

    @Test
    fun pausedShowsPlay() {
        assertFalse(state().showsPause)
    }

    @Test
    fun playingShowsPause() {
        assertTrue(state(playing = true, playWhenReady = true).showsPause)
    }

    @Test
    fun bufferingAfterPressingPlayAlreadyShowsPauseAndIsWaiting() {
        val s = state(buffering = true, playWhenReady = true)
        assertTrue(s.showsPause)
        assertTrue(s.isWaitingToPlay)
    }

    @Test
    fun bufferingWhilePausedShowsPlayAndIsNotWaiting() {
        val s = state(buffering = true, playWhenReady = false)
        assertFalse(s.showsPause)
        assertFalse(s.isWaitingToPlay)
    }

    @Test
    fun hasMediaFollowsTheCurrentTrack() {
        assertTrue(state(track = 5L).hasMedia)
        assertFalse(state(track = null).hasMedia)
        assertFalse(PlaybackUiState().hasMedia)
    }

    @Test
    fun theDefaultStateHasNoQueue() {
        val s = PlaybackUiState()
        assertEquals(-1, s.queueIndex)
        assertEquals(0, s.queueSize)
    }

    @Test
    fun theQueueEntryKeepsWhatItWasGiven() {
        val e = QueueEntry(trackId = 7L, title = "T", artist = "A")
        assertEquals(7L, e.trackId)
        assertEquals("T", e.title)
        assertEquals("A", e.artist)
    }
}
