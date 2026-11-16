package com.sonora.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import java.util.concurrent.Executors

enum class PlayState { IDLE, PREPARING, PLAYING, PAUSED, ERROR }
enum class RepeatMode { OFF, ALL, ONE }

/**
 * Shared playback state. The service owns the MediaPlayer; the UI only reads this object and calls its
 * functions, so rotating, recreating or closing the activity never interrupts the music.
 */
object PlaybackHub {
    var queue: List<Track> = emptyList()
    var index: Int = -1
    var state: PlayState = PlayState.IDLE
    var shuffle: Boolean = false
    var repeat: RepeatMode = RepeatMode.OFF
    var error: String? = null
    var sleepEndsAt: Long = 0L
    var service: PlaybackService? = null

    private val listeners = ArrayList<() -> Unit>()

    val current: Track? get() = queue.getOrNull(index)
    val isPlaying: Boolean get() = state == PlayState.PLAYING

    fun addListener(l: () -> Unit) { if (!listeners.contains(l)) listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }
    fun notifyChanged() { for (l in ArrayList(listeners)) l() }

    fun positionMs(): Int = service?.position() ?: 0
    fun durationMs(): Int {
        val d = service?.duration() ?: 0
        return if (d > 0) d else (current?.durationMs ?: 0L).toInt()
    }

    fun playQueue(ctx: Context, tracks: List<Track>, start: Int) {
        if (tracks.isEmpty() || start !in tracks.indices) return
        queue = tracks
        index = start
        error = null
        val s = service
        if (s != null) s.playAt(start) else send(ctx, PlaybackService.ACTION_PLAY_AT, start)
    }

    fun toggle(ctx: Context) {
        val s = service
        if (s != null) s.togglePlay() else if (queue.isNotEmpty()) send(ctx, PlaybackService.ACTION_TOGGLE, -1)
    }

    fun next(ctx: Context) {
        val s = service
        if (s != null) s.next(false) else if (queue.isNotEmpty()) send(ctx, PlaybackService.ACTION_NEXT, -1)
    }

    fun previous(ctx: Context) {
        val s = service
        if (s != null) s.previous() else if (queue.isNotEmpty()) send(ctx, PlaybackService.ACTION_PREV, -1)
    }

    fun seekTo(ms: Int) { service?.seekTo(ms) }
    fun audioSessionId(): Int = service?.audioSession() ?: 0

    fun applyShuffle(on: Boolean) {
        shuffle = on
        service?.onShuffleChanged()
        notifyChanged()
    }

    fun cycleRepeat() {
        repeat = when (repeat) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        notifyChanged()
    }

    fun setSleepTimer(minutes: Int): Boolean {
        val s = service ?: return false
        s.setSleepTimer(minutes)
        return true
    }

    private fun send(ctx: Context, action: String, index: Int) {
        val i = Intent(ctx, PlaybackService::class.java)
        i.action = action
        i.putExtra("index", index)
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
    }
}

class PlaybackService : Service() {

    companion object {
        const val ACTION_PLAY_AT = "com.sonora.music.PLAY_AT"
        const val ACTION_TOGGLE = "com.sonora.music.TOGGLE"
        const val ACTION_NEXT = "com.sonora.music.NEXT"
        const val ACTION_PREV = "com.sonora.music.PREV"
        const val ACTION_STOP = "com.sonora.music.STOP"
        private const val CHANNEL_ID = "sonara_playback"
        private const val NOTIFICATION_ID = 41
    }

    private var mp: MediaPlayer? = null
    private var prepared = false
    private lateinit var session: MediaSession
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private val artExecutor = Executors.newSingleThreadExecutor()
    private var order = ArrayList<Int>()
    private var orderPos = 0
    private var errorStreak = 0
    private var foreground = false
    private var hasFocus = false
    private var resumeOnFocus = false
    private var focusRequest: AudioFocusRequest? = null
    private var noisyRegistered = false
    private var sleepRunnable: Runnable? = null
    private var art: Bitmap? = null
    private var artTrackId = -1L

    private val attrs: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()
        }
    }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pause()
                abandonFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (PlaybackHub.state == PlayState.PLAYING) {
                    resumeOnFocus = true
                    try { mp?.pause() } catch (e: IllegalStateException) { }
                    PlaybackHub.state = PlayState.PAUSED
                    publish()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mp?.setVolume(0.25f, 0.25f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                mp?.setVolume(1f, 1f)
                if (resumeOnFocus) {
                    resumeOnFocus = false
                    resume()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        PlaybackHub.service = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL_ID, "Playback", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            notificationManager.createNotificationChannel(ch)
        }
        session = MediaSession(this, "SONARA")
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { resume() }
            override fun onPause() { pause() }
            override fun onSkipToNext() { next(false) }
            override fun onSkipToPrevious() { previous() }
            override fun onSeekTo(pos: Long) { seekTo(pos.toInt()) }
            override fun onStop() { stopPlayback() }
        })
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        session.isActive = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A service started with startForegroundService() must promote itself immediately.
        showForeground(buildNotification())
        when (intent?.action) {
            ACTION_PLAY_AT -> playAt(intent.getIntExtra("index", 0))
            ACTION_TOGGLE -> togglePlay()
            ACTION_NEXT -> next(false)
            ACTION_PREV -> previous()
            ACTION_STOP -> stopPlayback()
        }
        if (PlaybackHub.queue.isEmpty()) stopPlayback()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        releasePlayer()
        unregisterNoisy()
        abandonFocus()
        session.isActive = false
        session.release()
        artExecutor.shutdownNow()
        PlaybackHub.state = PlayState.IDLE
        PlaybackHub.sleepEndsAt = 0L
        PlaybackHub.service = null
        PlaybackHub.notifyChanged()
        super.onDestroy()
    }

    // ------------------------------------------------------------ transport

    fun playAt(queueIndex: Int) {
        val q = PlaybackHub.queue
        if (queueIndex !in q.indices) { stopPlayback(); return }
        buildOrder(queueIndex)
        errorStreak = 0
        startCurrent()
    }

    private fun buildOrder(first: Int) {
        val n = PlaybackHub.queue.size
        order = ArrayList(n)
        if (PlaybackHub.shuffle) {
            order.add(first)
            order.addAll((0 until n).filter { it != first }.shuffled())
            orderPos = 0
        } else {
            for (i in 0 until n) order.add(i)
            orderPos = first
        }
    }

    fun onShuffleChanged() {
        if (order.isEmpty()) return
        buildOrder(PlaybackHub.index.coerceAtLeast(0))
    }

    private fun startCurrent() {
        val qi = order.getOrNull(orderPos)
        val track = if (qi == null) null else PlaybackHub.queue.getOrNull(qi)
        if (qi == null || track == null) { stopPlayback(); return }
        PlaybackHub.index = qi
        releasePlayer()
        PlaybackHub.state = PlayState.PREPARING
        PlaybackHub.error = null
        try {
            val p = MediaPlayer()
            mp = p
            prepared = false
            p.setAudioAttributes(attrs)
            p.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            p.setDataSource(applicationContext, track.uri)
            p.setOnPreparedListener { player -> if (mp === player) onPrepared() }
            p.setOnCompletionListener { player -> if (mp === player) onCompleted() }
            p.setOnErrorListener { player, _, _ ->
                if (mp === player) onPlayerError()
                true
            }
            p.prepareAsync()
        } catch (e: Exception) {
            onPlayerError()
            return
        }
        loadArt(track)
        publish()
    }

    private fun onPrepared() {
        if (PlaybackHub.state != PlayState.PREPARING) return
        prepared = true
        if (!requestFocus()) {
            PlaybackHub.state = PlayState.PAUSED
            publish()
            return
        }
        try {
            mp?.start()
        } catch (e: IllegalStateException) {
            onPlayerError()
            return
        }
        errorStreak = 0
        PlaybackHub.state = PlayState.PLAYING
        registerNoisy()
        publish()
    }

    private fun onCompleted() {
        if (PlaybackHub.repeat == RepeatMode.ONE) startCurrent() else advance(true)
    }

    private fun advance(auto: Boolean) {
        if (order.isEmpty()) return
        var nextPos = orderPos + 1
        if (nextPos >= order.size) {
            if (auto && PlaybackHub.repeat == RepeatMode.OFF) { finishQueue(); return }
            nextPos = 0
        }
        orderPos = nextPos
        startCurrent()
    }

    private fun finishQueue() {
        releasePlayer()
        orderPos = 0
        PlaybackHub.index = order.getOrNull(0) ?: 0
        PlaybackHub.state = PlayState.PAUSED
        unregisterNoisy()
        publish()
    }

    fun next(auto: Boolean) { advance(auto) }

    fun previous() {
        if (order.isEmpty()) return
        if (position() > 3000) { seekTo(0); return }
        orderPos = if (orderPos - 1 < 0) order.size - 1 else orderPos - 1
        startCurrent()
    }

    fun togglePlay() {
        when (PlaybackHub.state) {
            PlayState.PLAYING -> pause()
            PlayState.PAUSED -> resume()
            PlayState.PREPARING -> { }
            else -> {
                if (PlaybackHub.queue.isEmpty()) stopPlayback()
                else if (order.isEmpty()) playAt(PlaybackHub.index.coerceAtLeast(0))
                else startCurrent()
            }
        }
    }

    fun pause() {
        if (PlaybackHub.state != PlayState.PLAYING) return
        try { mp?.pause() } catch (e: IllegalStateException) { }
        resumeOnFocus = false
        PlaybackHub.state = PlayState.PAUSED
        unregisterNoisy()
        publish()
    }

    fun resume() {
        val p = mp
        if (p == null || !prepared) {
            if (PlaybackHub.state == PlayState.PREPARING) return
            if (order.isNotEmpty()) startCurrent() else playAt(PlaybackHub.index.coerceAtLeast(0))
            return
        }
        if (!requestFocus()) return
        try {
            p.start()
        } catch (e: IllegalStateException) {
            onPlayerError()
            return
        }
        PlaybackHub.state = PlayState.PLAYING
        registerNoisy()
        publish()
    }

    fun audioSession(): Int = try { mp?.audioSessionId ?: 0 } catch (e: IllegalStateException) { 0 }

    fun seekTo(ms: Int) {
        if (!prepared) return
        try { mp?.seekTo(ms) } catch (e: IllegalStateException) { }
        updateSession()
    }

    fun position(): Int {
        if (!prepared) return 0
        return try { mp?.currentPosition ?: 0 } catch (e: IllegalStateException) { 0 }
    }

    fun duration(): Int {
        if (!prepared) return 0
        return try { mp?.duration ?: 0 } catch (e: IllegalStateException) { 0 }
    }

    fun stopPlayback() {
        handler.removeCallbacksAndMessages(null)
        releasePlayer()
        unregisterNoisy()
        abandonFocus()
        PlaybackHub.state = PlayState.IDLE
        PlaybackHub.sleepEndsAt = 0L
        PlaybackHub.notifyChanged()
        stopForeground(true)
        foreground = false
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    fun setSleepTimer(minutes: Int) {
        sleepRunnable?.let { handler.removeCallbacks(it) }
        sleepRunnable = null
        PlaybackHub.sleepEndsAt = 0L
        if (minutes > 0) {
            val r = Runnable {
                PlaybackHub.sleepEndsAt = 0L
                sleepRunnable = null
                pause()
            }
            sleepRunnable = r
            handler.postDelayed(r, minutes * 60000L)
            PlaybackHub.sleepEndsAt = SystemClock.elapsedRealtime() + minutes * 60000L
        }
        PlaybackHub.notifyChanged()
    }

    private fun onPlayerError() {
        releasePlayer()
        errorStreak++
        val title = PlaybackHub.queue.getOrNull(order.getOrNull(orderPos) ?: -1)?.title ?: "this file"
        PlaybackHub.state = PlayState.ERROR
        PlaybackHub.error = "Couldn't play $title"
        publish()
        if (errorStreak < order.size) {
            handler.postDelayed({ if (PlaybackHub.state == PlayState.ERROR) advance(false) }, 400)
        }
    }

    private fun releasePlayer() {
        prepared = false
        val p = mp
        mp = null
        if (p != null) {
            try { p.setOnPreparedListener(null); p.setOnCompletionListener(null); p.setOnErrorListener(null) } catch (e: Exception) { }
            try { p.release() } catch (e: Exception) { }
        }
    }

    // ------------------------------------------------------------ audio focus and noisy output

    @Suppress("DEPRECATION")
    private fun requestFocus(): Boolean {
        if (hasFocus) return true
        val result = if (Build.VERSION.SDK_INT >= 26) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            focusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }

    @Suppress("DEPRECATION")
    private fun abandonFocus() {
        if (!hasFocus) return
        hasFocus = false
        if (Build.VERSION.SDK_INT >= 26) {
            val req = focusRequest
            if (req != null) audioManager.abandonAudioFocusRequest(req)
        } else {
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    private fun registerNoisy() {
        if (noisyRegistered) return
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        noisyRegistered = true
    }

    private fun unregisterNoisy() {
        if (!noisyRegistered) return
        noisyRegistered = false
        try { unregisterReceiver(noisyReceiver) } catch (e: IllegalArgumentException) { }
    }

    // ------------------------------------------------------------ session and notification

    private fun loadArt(track: Track) {
        art = null
        artTrackId = track.id
        artExecutor.execute {
            val b = Artwork.decodeSync(applicationContext, track, 400)
            handler.post {
                if (b != null && PlaybackHub.current?.id == track.id) {
                    art = b
                    updateSession()
                    updateNotification()
                }
            }
        }
    }

    private fun publish() {
        updateSession()
        updateNotification()
        PlaybackHub.notifyChanged()
    }

    private fun updateSession() {
        val t = PlaybackHub.current
        val playing = PlaybackHub.state == PlayState.PLAYING
        val st = when (PlaybackHub.state) {
            PlayState.PLAYING -> PlaybackState.STATE_PLAYING
            PlayState.PAUSED -> PlaybackState.STATE_PAUSED
            PlayState.PREPARING -> PlaybackState.STATE_BUFFERING
            else -> PlaybackState.STATE_STOPPED
        }
        val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_STOP
        session.setPlaybackState(
            PlaybackState.Builder().setActions(actions).setState(st, position().toLong(), if (playing) 1f else 0f).build()
        )
        if (t != null) {
            val mb = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, t.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, t.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, t.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, t.durationMs)
            val a = art
            if (a != null && artTrackId == t.id) mb.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, a)
            session.setMetadata(mb.build())
        }
    }

    private fun action(icon: Int, title: String, action: String, code: Int): Notification.Action {
        val i = Intent(this, PlaybackService::class.java)
        i.action = action
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pi = if (Build.VERSION.SDK_INT >= 26) PendingIntent.getForegroundService(this, code, i, flags)
        else PendingIntent.getService(this, code, i, flags)
        return Notification.Action.Builder(Icon.createWithResource(this, icon), title, pi).build()
    }

    @Suppress("DEPRECATION")
    private fun buildNotification(): Notification {
        val t = PlaybackHub.current
        val playing = PlaybackHub.state == PlayState.PLAYING || PlaybackHub.state == PlayState.PREPARING
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        b.setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(t?.title ?: "SONARA")
            .setContentText(t?.artist ?: "")
            .setContentIntent(open)
            .setOngoing(playing)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .addAction(action(R.drawable.ic_skip_previous, "Previous", ACTION_PREV, 1))
            .addAction(action(if (playing) R.drawable.ic_pause else R.drawable.ic_play, if (playing) "Pause" else "Play", ACTION_TOGGLE, 2))
            .addAction(action(R.drawable.ic_skip_next, "Next", ACTION_NEXT, 3))
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2))
        if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_LOW)
        val a = art
        if (a != null && t != null && artTrackId == t.id) b.setLargeIcon(a)
        return b.build()
    }

    private fun showForeground(n: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, n)
            }
            foreground = true
        } catch (e: Exception) {
            foreground = false
            notificationManager.notify(NOTIFICATION_ID, n)
        }
    }

    private fun updateNotification() {
        val state = PlaybackHub.state
        if (state == PlayState.IDLE) return
        val n = buildNotification()
        val active = state == PlayState.PLAYING || state == PlayState.PREPARING
        if (active) {
            if (foreground) notificationManager.notify(NOTIFICATION_ID, n) else showForeground(n)
        } else {
            if (foreground) {
                stopForeground(false)
                foreground = false
            }
            notificationManager.notify(NOTIFICATION_ID, n)
        }
    }
}
