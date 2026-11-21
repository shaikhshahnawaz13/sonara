package com.sonara.app.audio

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sonara.app.data.local.preferences.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The equalizer, connected to the audio the player is actually producing.
 *
 * It lives for the whole process (created by the app container) while the playback service, which owns the only
 * ExoPlayer, attaches and detaches it: [attach] when the player is created, [detach] when it is released. The
 * effect is created on the player's own audio session and follows it if the session id changes. At most one
 * [Equalizer] instance exists at any time, and none exists while the equalizer is off, so a disabled equalizer
 * does not touch the audio path at all. Every call into the audio framework is guarded: a device or output that
 * refuses the effect ends in [EffectLink.Failed], never in a crash or an interruption of playback.
 *
 * Everything here runs on the main thread (the app scope is Main.immediate); only the one-off device probe runs
 * on a background thread. The choice is persisted a moment after the last change, and immediately on detach.
 */
class AudioEffectsManager(
    private val appContext: Context,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(EqualizerUiState())
    val state: StateFlow<EqualizerUiState> = _state

    private var desired = EqualizerSettings()
    private var loaded = false
    private var loadStarted = false
    private var support: EqualizerSupport = EqualizerSupport.Checking
    private var probing = false

    private var player: ExoPlayer? = null
    private var sessionId = C.AUDIO_SESSION_ID_UNSET
    private var effect: Equalizer? = null
    private var effectSession = C.AUDIO_SESSION_ID_UNSET
    private var link = EffectLink.NoPlayer
    private var persistJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            bind(audioSessionId)
        }
    }

    /** Called by the playback service once its player exists. Safe to call again; the previous attachment is dropped first. */
    fun attach(exoPlayer: ExoPlayer) {
        detach()
        player = exoPlayer
        exoPlayer.addListener(playerListener)
        sessionId = exoPlayer.audioSessionId
        link = EffectLink.Idle
        load()
        if (loaded) bind(sessionId) else publish()
    }

    /** Called by the playback service before its player is released: the effect is released first, then the choice saved. */
    fun detach() {
        player?.removeListener(playerListener)
        player = null
        sessionId = C.AUDIO_SESSION_ID_UNSET
        releaseEffect()
        link = EffectLink.NoPlayer
        flushPersist()
        publish()
    }

    /** Asks the device what its equalizer offers, once. The screen calls this when it opens. */
    fun ensureProbed() {
        if (support !is EqualizerSupport.Checking || probing) return
        probing = true
        scope.launch {
            val result = withContext(Dispatchers.Default) { AndroidEqualizerProbe.probe(appContext) }
            support = result
            probing = false
            publish()
        }
    }

    fun setEnabled(on: Boolean) {
        if (!loaded || desired.enabled == on) return
        desired = desired.copy(enabled = on)
        onDesiredChanged()
    }

    fun selectPreset(name: String) {
        val profile = profile() ?: return
        desired = EqualizerLogic.withPreset(desired, profile, name) ?: return
        onDesiredChanged()
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        val profile = profile() ?: return
        val next = EqualizerLogic.withBandLevel(desired, profile, band, levelMb)
        if (next == desired) return
        desired = next
        onDesiredChanged()
    }

    fun resetFlat() {
        val profile = profile() ?: return
        desired = EqualizerLogic.flat(desired, profile)
        onDesiredChanged()
    }

    private fun profile(): EqualizerProfile? = if (loaded) (support as? EqualizerSupport.Supported)?.profile else null

    private fun load() {
        if (loadStarted) return
        loadStarted = true
        scope.launch {
            desired = try {
                settings.equalizerSettings.first()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                EqualizerSettings()
            }
            loaded = true
            // Playback may already be running: connect the saved choice to it now.
            if (player != null) bind(sessionId) else publish()
        }
    }

    private fun onDesiredChanged() {
        if (desired.enabled) {
            if (effect == null) bind(sessionId) else applyToEffect()
        } else {
            releaseEffect()
            link = if (player == null) EffectLink.NoPlayer else EffectLink.Idle
        }
        schedulePersist()
        publish()
    }

    /** Puts the effect on [newSession] (or removes it when it should not exist). */
    private fun bind(newSession: Int) {
        sessionId = newSession
        if (!loaded || player == null) {
            publish()
            return
        }
        if (effect != null && effectSession == newSession) {
            applyToEffect()
            publish()
            return
        }
        releaseEffect()
        link = EffectLink.Idle
        if (desired.enabled && newSession != C.AUDIO_SESSION_ID_UNSET) createEffect(newSession)
        publish()
    }

    private fun createEffect(session: Int) {
        try {
            val created = Equalizer(EFFECT_PRIORITY, session)
            created.setControlStatusListener { _, granted -> scope.launch { onControlChanged(granted) } }
            effect = created
            effectSession = session
            applyToEffect()
        } catch (e: Exception) {
            // Unsupported on this device or output, or the framework refused. Playback is unaffected.
            releaseEffect()
            link = EffectLink.Failed
        }
    }

    /** Writes the chosen levels to the live effect and switches it on. Any refusal ends in [EffectLink.Failed]. */
    private fun applyToEffect() {
        val live = effect ?: return
        try {
            if (!live.hasControl()) {
                link = EffectLink.ControlLost
                return
            }
            val bands = live.getNumberOfBands().toInt()
            val range = live.getBandLevelRange()
            val levels = EqualizerLogic.resolveLevels(desired.bandLevelsMb, bands, range[0].toInt(), range[1].toInt())
            levels.forEachIndexed { band, mb -> live.setBandLevel(band.toShort(), mb.toShort()) }
            link = if (live.setEnabled(true) == AudioEffect.SUCCESS) EffectLink.Attached else EffectLink.Failed
        } catch (e: Exception) {
            releaseEffect()
            link = EffectLink.Failed
        }
    }

    private fun onControlChanged(granted: Boolean) {
        if (effect == null) return
        if (granted) applyToEffect() else link = EffectLink.ControlLost
        publish()
    }

    private fun releaseEffect() {
        val old = effect ?: return
        effect = null
        effectSession = C.AUDIO_SESSION_ID_UNSET
        try {
            old.setControlStatusListener(null)
            old.release()
        } catch (_: Exception) {
            // Releasing an effect that is already gone is not an error worth surfacing.
        }
    }

    private fun schedulePersist() {
        persistJob?.cancel()
        val snapshot = desired
        persistJob = scope.launch {
            delay(PERSIST_DELAY_MS)
            save(snapshot)
        }
    }

    private fun flushPersist() {
        val pending = persistJob ?: return
        if (!pending.isActive) return
        pending.cancel()
        val snapshot = desired
        scope.launch { save(snapshot) }
    }

    private suspend fun save(value: EqualizerSettings) {
        try {
            settings.setEqualizerSettings(value)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            // The choice stays in effect for this session; it just will not survive a restart.
        }
    }

    private fun publish() {
        val supported = support as? EqualizerSupport.Supported
        _state.value = if (supported != null) {
            val profile = supported.profile
            EqualizerUiState(
                loaded = loaded,
                support = support,
                enabled = desired.enabled,
                presetName = EqualizerLogic.effectivePreset(desired, profile),
                levelsMb = EqualizerLogic.resolveLevels(desired.bandLevelsMb, profile.bandCount, profile.minLevelMb, profile.maxLevelMb),
                link = link,
            )
        } else {
            EqualizerUiState(loaded = loaded, support = support, enabled = desired.enabled, link = link)
        }
    }

    private companion object {
        const val EFFECT_PRIORITY = 0
        const val PERSIST_DELAY_MS = 300L
    }
}

/**
 * Reads what the device's equalizer offers by creating one on a throwaway audio session and releasing it at once.
 * The preset curves are read here too, so applying a preset later is just writing its levels.
 */
internal object AndroidEqualizerProbe {
    fun probe(context: Context): EqualizerSupport = try {
        probeOrThrow(context)
    } catch (e: Exception) {
        EqualizerSupport.Unsupported(e.message ?: DEFAULT_REASON)
    }

    private fun probeOrThrow(context: Context): EqualizerSupport {
        val available = AudioEffect.queryEffects()
        if (available != null && available.none { it.type == AudioEffect.EFFECT_TYPE_EQUALIZER }) {
            throw IllegalStateException("This device does not provide an equalizer.")
        }
        val audioManager = context.getSystemService(AudioManager::class.java)
        val session = audioManager?.generateAudioSessionId() ?: AudioManager.ERROR
        if (session == AudioManager.ERROR) throw IllegalStateException(DEFAULT_REASON)
        var eq: Equalizer? = null
        try {
            val created = Equalizer(0, session)
            eq = created
            val bands = created.getNumberOfBands().toInt()
            if (bands <= 0) throw IllegalStateException("This device's equalizer has no bands.")
            val range = created.getBandLevelRange()
            val centres = List(bands) { created.getCenterFreq(it.toShort()) }
            val presets = List(created.getNumberOfPresets().toInt()) { index ->
                created.usePreset(index.toShort())
                EqualizerPreset(
                    name = created.getPresetName(index.toShort()),
                    levelsMb = List(bands) { band -> created.getBandLevel(band.toShort()).toInt() },
                )
            }
            return EqualizerSupport.Supported(EqualizerProfile(centres, range[0].toInt(), range[1].toInt(), presets))
        } finally {
            try {
                eq?.release()
            } catch (_: Exception) {
            }
        }
    }

    private const val DEFAULT_REASON = "The equalizer could not be started on this device."
}
