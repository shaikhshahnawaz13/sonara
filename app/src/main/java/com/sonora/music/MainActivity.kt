package com.sonora.music

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast

/** Owns the window, navigation and permission hand-off only. All screens and playback live elsewhere. */
class MainActivity : Activity(), Host {

    override val ctx: Context get() = this

    private lateinit var rootFrame: FrameLayout
    private lateinit var shell: LinearLayout
    private lateinit var contentFrame: FrameLayout
    private lateinit var mini: MiniPlayer
    private lateinit var nav: NavBar
    private lateinit var playerScreen: PlayerScreen
    private lateinit var tabs: List<Screen>

    private val stack = ArrayList<Screen>()
    private var tabIndex = 0
    private var playerOpen = false
    private var keyboardOpen = false
    private var lastErrorShown: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var observer: ContentObserver? = null

    private val refreshAll: () -> Unit = {
        for (t in tabs) t.onLibraryChanged()
        for (s in stack) s.onLibraryChanged()
        if (playerOpen) playerScreen.refresh()
        contentFrame.post { contentFrame.unclip() }
    }

    private val hubListener: () -> Unit = { onPlaybackChanged() }

    private val rescan = Runnable { if (hasPermission()) startScan() }

    private val miniTick = object : Runnable {
        override fun run() {
            if (mini.visibility == View.VISIBLE) mini.updateProgress()
            handler.postDelayed(this, 500)
        }
    }

    // ------------------------------------------------------------ lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        Store.init(this)
        buildUi()
        configureWindow()
        selectTab(0)
        if (hasPermission()) startScan()
    }

    override fun onStart() {
        super.onStart()
        Library.addListener(refreshAll)
        Store.addListener(refreshAll)
        PlaybackHub.addListener(hubListener)
        val obs = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                handler.removeCallbacks(rescan)
                handler.postDelayed(rescan, 1500)
            }
        }
        observer = obs
        try {
            contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, obs)
        } catch (e: Exception) { }
        if (hasPermission() && !Library.loadedOnce && !Library.scanning) startScan()
        refreshAll()
        onPlaybackChanged()
        handler.post(miniTick)
    }

    override fun onStop() {
        handler.removeCallbacks(miniTick)
        handler.removeCallbacks(rescan)
        observer?.let { contentResolver.unregisterContentObserver(it) }
        observer = null
        Library.removeListener(refreshAll)
        Store.removeListener(refreshAll)
        PlaybackHub.removeListener(hubListener)
        playerScreen.onHidden()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (playerOpen) playerScreen.onShown()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        when {
            playerOpen -> closePlayer()
            stack.isNotEmpty() -> back()
            tabIndex != 0 -> selectTab(0)
            else -> super.onBackPressed()
        }
    }

    // ------------------------------------------------------------ window and layout

    private fun buildUi() {
        val pal = Theme.p
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.setBackgroundDrawable(ColorDrawable(pal.bg))

        rootFrame = FrameLayout(this)
        rootFrame.setBackgroundColor(pal.bg)
        shell = LinearLayout(this)
        shell.orientation = LinearLayout.VERTICAL
        shell.clipChildren = false
        shell.clipToPadding = false
        rootFrame.clipChildren = false
        contentFrame = SwipeFrame(this, { !playerOpen && stack.isEmpty() && !keyboardOpen }) { dir ->
            val target = tabIndex + dir
            if (target in 0..2) selectTab(target)
        }
        contentFrame.clipChildren = false
        shell.addView(contentFrame, LinearLayout.LayoutParams(MATCH, 0, 1f))

        mini = MiniPlayer(this, this)
        mini.visibility = View.GONE
        val mm = mini.neoMargin()
        val miniLp = LinearLayout.LayoutParams(MATCH, WRAP)
        miniLp.leftMargin = dp(20) - mm
        miniLp.rightMargin = dp(20) - mm
        miniLp.topMargin = -mm / 2
        miniLp.bottomMargin = -mm / 2
        shell.addView(mini, miniLp)

        nav = NavBar(this) { index -> selectTab(index) }
        val nm = nav.neoMargin()
        val navLp = LinearLayout.LayoutParams(MATCH, WRAP)
        navLp.leftMargin = dp(20) - nm
        navLp.rightMargin = dp(20) - nm
        navLp.bottomMargin = dp(4) - nm
        shell.addView(nav, navLp)

        rootFrame.addView(shell, FrameLayout.LayoutParams(MATCH, MATCH))
        playerScreen = PlayerScreen(this)
        playerScreen.view.visibility = View.GONE
        rootFrame.addView(playerScreen.view, FrameLayout.LayoutParams(MATCH, MATCH))
        tabs = listOf(HomeScreen(this), SearchScreen(this), LibraryScreen(this))
        setContentView(rootFrame)

        rootFrame.setOnApplyWindowInsetsListener { _, insets ->
            val left: Int
            val top: Int
            val right: Int
            val bottom: Int
            val keyboard: Boolean
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val ime = insets.getInsets(WindowInsets.Type.ime())
                left = bars.left
                top = bars.top
                right = bars.right
                bottom = if (ime.bottom > bars.bottom) ime.bottom else bars.bottom
                keyboard = ime.bottom > bars.bottom
            } else {
                left = insets.systemWindowInsetLeft
                top = insets.systemWindowInsetTop
                right = insets.systemWindowInsetRight
                bottom = insets.systemWindowInsetBottom
                keyboard = insets.systemWindowInsetBottom > insets.stableInsetBottom + dp(80)
            }
            shell.setPadding(left, top, right, bottom)
            playerScreen.view.setPadding(left, top, right, bottom)
            if (keyboard != keyboardOpen) {
                keyboardOpen = keyboard
                updateBottomBars()
            }
            insets
        }
    }

    private fun configureWindow() {
        val light = !Theme.p.dark
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (light) mask else 0, mask)
        } else {
            var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            if (light) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                if (Build.VERSION.SDK_INT >= 26) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            window.decorView.systemUiVisibility = flags
        }
        rootFrame.requestApplyInsets()
    }

    private fun updateBottomBars() {
        val settings = stack.lastOrNull() is SettingsScreen
        nav.visibility = if (keyboardOpen || settings) View.GONE else View.VISIBLE
        val has = PlaybackHub.current != null
        mini.visibility = if (!keyboardOpen && !settings && has) View.VISIBLE else View.GONE
        if (has) mini.bind()
    }

    // ------------------------------------------------------------ navigation

    private fun show(screen: Screen) {
        endSlide()
        contentFrame.removeAllViews()
        val v = screen.view
        v.translationX = 0f
        (v.parent as? ViewGroup)?.removeView(v)
        v.animate().cancel()
        v.alpha = 1f
        v.translationY = 0f
        contentFrame.addView(v, FrameLayout.LayoutParams(MATCH, MATCH))
        screen.onShow()
        v.unclip()
        v.revealIn()
    }

    private var slideAnim: android.animation.ValueAnimator? = null

    private fun endSlide() {
        slideAnim?.cancel()
        slideAnim = null
    }

    override fun selectTab(index: Int) {
        val from = tabIndex
        tabIndex = index
        val wasDetail = stack.isNotEmpty()
        stack.clear()
        updateBottomBars()
        nav.select(index, true)
        endSlide()
        val width = contentFrame.width
        if (index == from || wasDetail || width <= 0 || contentFrame.childCount == 0 || !animationsEnabled()) {
            show(tabs[index])
            return
        }
        slideTo(tabs[index], if (index > from) 1 else -1, width)
    }

    /** Slides the new tab in from the side its position implies while the old one leaves the other way. */
    private fun slideTo(screen: Screen, dir: Int, width: Int) {
        val outgoing = contentFrame.getChildAt(contentFrame.childCount - 1)
        val incoming = screen.view
        if (outgoing === incoming) { show(screen); return }
        (incoming.parent as? ViewGroup)?.removeView(incoming)
        incoming.animate().cancel()
        incoming.alpha = 1f
        incoming.translationY = 0f
        incoming.translationX = (dir * width).toFloat()
        contentFrame.addView(incoming, FrameLayout.LayoutParams(MATCH, MATCH))
        screen.onShow()
        incoming.unclip()
        outgoing.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        incoming.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val a = android.animation.ValueAnimator.ofFloat(0f, 1f)
        a.duration = SLIDE_MS
        a.interpolator = slideInterpolator()
        a.addUpdateListener {
            val f = it.animatedValue as Float
            outgoing.translationX = -dir * width * f
            incoming.translationX = dir * width * (1f - f)
        }
        var done = false
        val finish = {
            if (!done) {
                done = true
                contentFrame.removeView(outgoing)
                outgoing.translationX = 0f
                outgoing.setLayerType(View.LAYER_TYPE_NONE, null)
                incoming.translationX = 0f
                incoming.setLayerType(View.LAYER_TYPE_NONE, null)
                if (slideAnim === a) slideAnim = null
            }
        }
        a.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) { finish() }
            override fun onAnimationCancel(animation: android.animation.Animator) { finish() }
        })
        slideAnim = a
        a.start()
    }

    override fun open(screen: Screen) {
        stack.add(screen)
        show(screen)
        updateBottomBars()
    }

    override fun back() {
        if (playerOpen) { closePlayer(); return }
        if (stack.isEmpty()) return
        stack.removeAt(stack.size - 1)
        show(if (stack.isEmpty()) tabs[tabIndex] else stack[stack.size - 1])
        updateBottomBars()
    }

    private var morphAnim: android.animation.ValueAnimator? = null
    private var morphClip: ClipShape? = null

    private fun rectIn(v: View, out: RectF): RectF {
        val a = IntArray(2)
        val r = IntArray(2)
        v.getLocationInWindow(a)
        rootFrame.getLocationInWindow(r)
        val m = v.neoMargin()
        out.set((a[0] - r[0] + m).toFloat(), (a[1] - r[1] + m).toFloat(), (a[0] - r[0] + v.width - m).toFloat(), (a[1] - r[1] + v.height - m).toFloat())
        return out
    }

    private fun clearPlayerTransforms(pv: View, shared: List<Shared>) {
        for (s in shared) s.reset()
        for (v in playerScreen.fadeGroup) { v.alpha = 1f; v.translationY = 0f }
        playerScreen.setDockAlpha(1f)
        pv.alpha = 1f
        pv.translationY = 0f
        pv.clipToOutline = false
        pv.outlineProvider = ViewOutlineProvider.BACKGROUND
        pv.setBackgroundColor(Theme.p.bg)
    }

    /** The mini-player grows into the full player (open) or shrinks back into it (close) as one object. */
    private fun morph(open: Boolean) {
        val pv = playerScreen.view
        morphAnim?.cancel()
        val canMorph = animationsEnabled() && mini.visibility == View.VISIBLE && mini.width > 0 && rootFrame.width > 0
        if (!canMorph) {
            clearPlayerTransforms(pv, emptyList())
            pv.visibility = if (open) View.VISIBLE else View.GONE
            return
        }
        val start = {
            val miniSurface = rectIn(mini, RectF())
            val full = RectF(0f, 0f, rootFrame.width.toFloat(), rootFrame.height.toFloat())
            val shared = listOf(
                Shared(playerScreen.ring, rectIn(mini.ring, RectF()), rectIn(playerScreen.ring, RectF()), false),
                Shared(playerScreen.title, rectIn(mini.title, RectF()), rectIn(playerScreen.title, RectF()), true),
                Shared(playerScreen.artist, rectIn(mini.artist, RectF()), rectIn(playerScreen.artist, RectF()), true),
                Shared(playerScreen.prev, rectIn(mini.prev, RectF()), rectIn(playerScreen.prev, RectF()), false),
                Shared(playerScreen.play, rectIn(mini.play, RectF()), rectIn(playerScreen.play, RectF()), false),
                Shared(playerScreen.next, rectIn(mini.next, RectF()), rectIn(playerScreen.next, RectF()), false)
            )
            val clip = ClipShape()
            morphClip = clip
            pv.outlineProvider = clip
            pv.clipToOutline = true
            val miniRadius = dp(30).toFloat()
            val lift = dp(28).toFloat()
            val bgFrom = Theme.p.surfaceBottom
            val frame = { e: Float ->
                clip.l = miniSurface.left + (full.left - miniSurface.left) * e
                clip.t = miniSurface.top + (full.top - miniSurface.top) * e
                clip.r = miniSurface.right + (full.right - miniSurface.right) * e
                clip.b = miniSurface.bottom + (full.bottom - miniSurface.bottom) * e
                clip.radius = miniRadius * (1f - e)
                pv.invalidateOutline()
                pv.setBackgroundColor(Theme.blend(bgFrom, Theme.p.bg, e))
                pv.alpha = (e / 0.3f).coerceIn(0f, 1f)
                for (sh in shared) sh.apply(e)
                val fade = ((e - 0.3f) / 0.7f).coerceIn(0f, 1f)
                for (v in playerScreen.fadeGroup) { v.alpha = fade; v.translationY = lift * (1f - fade) }
                playerScreen.setDockAlpha(fade)
            }
            frame(if (open) 0f else 1f)
            if (open) pv.visibility = View.VISIBLE
            val a = if (open) android.animation.ValueAnimator.ofFloat(0f, 1f) else android.animation.ValueAnimator.ofFloat(1f, 0f)
            a.duration = if (open) 380L else 320L
            a.interpolator = android.view.animation.PathInterpolator(0.25f, 0.1f, 0.2f, 1f)
            a.addUpdateListener { frame(it.animatedValue as Float) }
            var done = false
            val finish = {
                if (!done) {
                    done = true
                    clearPlayerTransforms(pv, shared)
                    if (!open && !playerOpen) pv.visibility = View.GONE
                    if (morphAnim === a) morphAnim = null
                }
            }
            a.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) { finish() }
                override fun onAnimationCancel(animation: android.animation.Animator) { finish() }
            })
            morphAnim = a
            a.start()
        }
        if (open) {
            // lay the full player out first so its final positions are known, without a visible flash
            pv.alpha = 0f
            pv.visibility = View.VISIBLE
            pv.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    pv.viewTreeObserver.removeOnPreDrawListener(this)
                    start()
                    return true
                }
            })
            pv.requestLayout()
        } else {
            start()
        }
    }

    override fun openPlayer() {
        if (PlaybackHub.current == null || playerOpen) return
        playerOpen = true
        playerScreen.refresh()
        playerScreen.onShown()
        morph(true)
    }

    override fun closePlayer() {
        if (!playerOpen) return
        playerOpen = false
        playerScreen.onHidden()
        morph(false)
    }

    private var lastRecorded = -1L

    private fun onPlaybackChanged() {
        val cur = PlaybackHub.current
        if (cur != null && cur.id != lastRecorded && (PlaybackHub.state == PlayState.PLAYING || PlaybackHub.state == PlayState.PREPARING)) {
            lastRecorded = cur.id
            Store.recordPlayed(cur.id)
        }
        updateBottomBars()
        if (playerOpen) playerScreen.refresh()
        for (t in tabs) t.onPlaybackChanged()
        if (stack.isNotEmpty()) stack[stack.size - 1].onPlaybackChanged()
        val err = PlaybackHub.error
        if (err != null && err != lastErrorShown) {
            lastErrorShown = err
            toast(err)
        }
        if (err == null) lastErrorShown = null
    }

    // ------------------------------------------------------------ playback requests

    override fun playQueue(tracks: List<Track>, index: Int) {
        if (index < 0) return
        PlaybackHub.playQueue(this, tracks, index)
    }

    override fun shuffleAll() {
        val all = Library.tracks
        if (all.isEmpty()) return
        PlaybackHub.applyShuffle(true)
        PlaybackHub.playQueue(this, all, (0 until all.size).random())
    }

    override fun toggleLike(track: Track) {
        val liked = Store.toggleLike(track.id)
        toast(if (liked) "Added to liked songs" else "Removed from liked songs")
    }

    override fun showSleepTimer() {
        val labels = arrayOf("Off", "15 minutes", "30 minutes", "45 minutes", "60 minutes")
        val minutes = intArrayOf(0, 15, 30, 45, 60)
        dialog().setTitle("Sleep timer").setItems(labels) { _, which ->
            if (!PlaybackHub.setSleepTimer(minutes[which])) {
                toast("Start playing something first")
            } else if (minutes[which] > 0) {
                toast("Music will pause in ${minutes[which]} minutes")
            } else {
                toast("Sleep timer off")
            }
        }.show()
    }

    override fun openEqualizer() {
        val i = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        i.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        val session = PlaybackHub.audioSessionId()
        if (session > 0) i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, session)
        try {
            startActivity(i)
        } catch (e: Exception) {
            toast("No system equalizer was found on this phone")
        }
    }

    // ------------------------------------------------------------ menus and dialogs

    private fun dialog(): AlertDialog.Builder = AlertDialog.Builder(
        this,
        if (Theme.p.dark) android.R.style.Theme_Material_Dialog_Alert else android.R.style.Theme_Material_Light_Dialog_Alert
    )

    override fun trackMenu(anchor: View, track: Track, playlistName: String?) {
        val pm = PopupMenu(this, anchor)
        pm.menu.add(0, 1, 0, if (Store.isLiked(track.id)) "Remove from liked songs" else "Add to liked songs")
        pm.menu.add(0, 2, 1, "Add to playlist")
        if (playlistName != null) pm.menu.add(0, 3, 2, "Remove from this playlist")
        pm.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> toggleLike(track)
                2 -> pickPlaylist(track)
                3 -> if (playlistName != null) Store.removeFromPlaylist(playlistName, track.id)
            }
            true
        }
        pm.show()
    }

    private fun pickPlaylist(track: Track) {
        val names = Store.playlists.keys.toList()
        val items = (names + "New playlist…").toTypedArray()
        dialog().setTitle("Add to playlist").setItems(items) { _, which ->
            if (which < names.size) {
                val ok = Store.addToPlaylist(names[which], track.id)
                toast(if (ok) "Added to ${names[which]}" else "Already in ${names[which]}")
            } else {
                promptNewPlaylist(track)
            }
        }.show()
    }

    override fun newPlaylist() { promptNewPlaylist(null) }

    private fun promptNewPlaylist(addTrack: Track?) {
        val input = EditText(this)
        input.hint = "Playlist name"
        input.setSingleLine(true)
        input.setTextColor(Theme.p.text)
        input.setHintTextColor(Theme.p.textMuted)
        dialog().setTitle("New playlist").setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (Store.createPlaylist(name)) {
                    if (addTrack != null) Store.addToPlaylist(name, addTrack.id)
                    toast("Created $name")
                } else {
                    toast("Choose a name that isn't empty or already used")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun confirmDeletePlaylist(name: String) {
        dialog().setTitle("Delete “$name”?")
            .setMessage("The songs stay on your phone. Only the playlist is removed.")
            .setPositiveButton("Delete") { _, _ -> Store.deletePlaylist(name) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun setDark(dark: Boolean) {
        Theme.setDark(this, dark)
        recreate()
    }

    // ------------------------------------------------------------ permissions and scanning

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    override fun hasPermission(): Boolean = checkSelfPermission(audioPermission()) == PackageManager.PERMISSION_GRANTED

    override fun requestPermission() {
        val p = audioPermission()
        val prefs = getSharedPreferences("sonara_app", Context.MODE_PRIVATE)
        val asked = prefs.getBoolean("asked", false)
        if (asked && !shouldShowRequestPermissionRationale(p)) {
            toast("Turn on Music and audio access for SONARA in Settings")
            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            try { startActivity(i) } catch (e: Exception) { }
            return
        }
        prefs.edit().putBoolean("asked", true).apply()
        val perms = if (Build.VERSION.SDK_INT >= 33) arrayOf(p, Manifest.permission.POST_NOTIFICATIONS) else arrayOf(p)
        requestPermissions(perms, 41)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 41) return
        if (hasPermission()) startScan() else toast("Without access SONARA can't see your music")
        refreshAll()
    }

    override fun requestScan() {
        if (!hasPermission()) requestPermission() else startScan()
    }

    private fun startScan() {
        Library.scan(this, Store.hideShort)
    }
}
