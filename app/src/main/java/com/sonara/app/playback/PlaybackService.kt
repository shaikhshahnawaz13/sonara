package com.sonara.app.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.sonara.app.app.AppContainer
import com.sonara.app.app.MainActivity
import com.sonara.app.app.SonaraApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The single owner of playback: one ExoPlayer wrapped in one MediaSession, running as a media-playback
 * foreground service. Screens never touch the player; they talk to it through a MediaController
 * (see PlaybackController), so playback is independent of any Activity or Compose lifecycle.
 *
 * Audio focus, "becoming noisy" (headphones unplugged / Bluetooth disconnected) and the wake lock are
 * handled by ExoPlayer itself, configured below. The notification, lock-screen controls and media-button
 * handling come from Media3's MediaSessionService.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var persistence: PlaybackPersistence? = null
    private var historyRecorder: PlayHistoryRecorder? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var consecutiveErrors = 0

    private val container: AppContainer
        get() = (application as SonaraApplication).container

    override fun onCreate() {
        super.onCreate()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exoPlayer.addListener(ErrorHandler())
        player = exoPlayer

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(SessionCallback())
            .setSessionActivity(openApp)
            .build()

        val store = PlaybackStore(container.database.playbackDao(), container.database.trackDao())
        val persist = PlaybackPersistence(exoPlayer, store, scope)
        // Do not let the empty startup player overwrite the saved queue before it has been restored.
        persist.suspended = true
        persist.start()
        persistence = persist

        historyRecorder = PlayHistoryRecorder(exoPlayer, container.database.historyDao(), scope).also { it.start() }

        scope.launch { restoreSavedQueue(exoPlayer, store, persist) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        persistence?.flush(container.appScope)
        val p = player
        // Keep playing in the background if music is playing; otherwise there is nothing to keep alive.
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        persistence?.flush(container.appScope)
        persistence?.stop()
        historyRecorder?.stop()
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        scope.cancel()
        super.onDestroy()
    }

    /** Puts the last queue and position back, paused, unless something already loaded a queue. */
    private suspend fun restoreSavedQueue(
        exoPlayer: ExoPlayer,
        store: PlaybackStore,
        persist: PlaybackPersistence,
    ) {
        try {
            if (!container.settingsRepository.settings.first().resumePlayback) return
            val (queue, state) = store.load() ?: return
            val tracks = container.libraryRepository.tracksByIds(queue.trackIds)
            if (tracks.isEmpty() || exoPlayer.mediaItemCount != 0) return
            exoPlayer.setMediaItems(
                tracks.map { it.toMediaItem() },
                queue.startIndex.coerceIn(0, tracks.lastIndex),
                queue.startPositionMs,
            )
            exoPlayer.repeatMode = state.repeatMode
            exoPlayer.shuffleModeEnabled = state.shuffleEnabled
            exoPlayer.prepare()
        } catch (_: Exception) {
            // A failed restore only means starting with an empty queue.
        } finally {
            persist.suspended = false
        }
    }

    /** A track that cannot be played (deleted, unreadable, unsupported) is skipped, not allowed to end playback. */
    private inner class ErrorHandler : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            val p = player ?: return
            consecutiveErrors++
            if (p.hasNextMediaItem() && consecutiveErrors < p.mediaItemCount) {
                p.seekToNextMediaItem()
                p.prepare()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) consecutiveErrors = 0
        }
    }

    private inner class SessionCallback : MediaSession.Callback {

        /** Controllers may send id-only items; give them URIs and metadata from the library. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = launchFuture {
            resolve(mediaItems).map { it.second }.toMutableList()
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = launchFuture {
            val resolved = resolve(mediaItems)
            val survivors = BooleanArray(mediaItems.size)
            resolved.forEach { survivors[it.first] = true }
            val adjusted = QueueRestore.adjustStartIndex(survivors.toList(), startIndex)
            val startSurvived = startIndex in survivors.indices && survivors[startIndex]
            MediaSession.MediaItemsWithStartPosition(
                resolved.map { it.second },
                if (resolved.isEmpty()) C.INDEX_UNSET else adjusted,
                if (startSurvived) startPositionMs else C.TIME_UNSET,
            )
        }

        /** Media button (e.g. Bluetooth play) while nothing is loaded: bring back the saved queue. */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = launchFuture {
            val (queue, _) = PlaybackStore(container.database.playbackDao(), container.database.trackDao()).load()
                ?: throw IllegalStateException("No saved queue to resume")
            val tracks = container.libraryRepository.tracksByIds(queue.trackIds)
            check(tracks.isNotEmpty()) { "Saved queue has no playable tracks" }
            MediaSession.MediaItemsWithStartPosition(
                tracks.map { it.toMediaItem() },
                queue.startIndex.coerceIn(0, tracks.lastIndex),
                queue.startPositionMs,
            )
        }
    }

    /** Resolves id-only items. Returns (index in the request, playable item) for every item that could be resolved. */
    private suspend fun resolve(items: List<MediaItem>): List<Pair<Int, MediaItem>> {
        val ids = items.filter { it.localConfiguration == null }.mapNotNull { it.mediaId.toLongOrNull() }
        val found = container.libraryRepository.tracksByIds(ids).associateBy { it.id }
        return items.mapIndexedNotNull { index, item ->
            when {
                item.localConfiguration != null -> index to item
                else -> item.mediaId.toLongOrNull()?.let { found[it] }?.let { index to it.toMediaItem() }
            }
        }
    }

    private fun <T> launchFuture(block: suspend () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        scope.launch {
            try {
                future.set(block())
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }
}
