package com.sonora.music

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Color
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
        contentFrame = FrameLayout(this)
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
        nav.visibility = if (keyboardOpen) View.GONE else View.VISIBLE
        val has = PlaybackHub.current != null
        mini.visibility = if (!keyboardOpen && has) View.VISIBLE else View.GONE
        if (has) mini.bind()
    }

    // ------------------------------------------------------------ navigation

    private fun show(screen: Screen) {
        contentFrame.removeAllViews()
        val v = screen.view
        (v.parent as? ViewGroup)?.removeView(v)
        v.animate().cancel()
        v.alpha = 1f
        v.translationY = 0f
        contentFrame.addView(v, FrameLayout.LayoutParams(MATCH, MATCH))
        screen.onShow()
        v.revealIn()
    }

    override fun selectTab(index: Int) {
        tabIndex = index
        stack.clear()
        nav.select(index, true)
        show(tabs[index])
    }

    override fun open(screen: Screen) {
        stack.add(screen)
        show(screen)
    }

    override fun back() {
        if (playerOpen) { closePlayer(); return }
        if (stack.isEmpty()) return
        stack.removeAt(stack.size - 1)
        show(if (stack.isEmpty()) tabs[tabIndex] else stack[stack.size - 1])
    }

    override fun openPlayer() {
        if (PlaybackHub.current == null) return
        playerOpen = true
        val v = playerScreen.view
        v.animate().cancel()
        v.visibility = View.VISIBLE
        playerScreen.refresh()
        playerScreen.onShown()
        if (animationsEnabled()) {
            v.translationY = rootFrame.height.toFloat()
            v.animate().translationY(0f).setDuration(280).setInterpolator(DecelerateInterpolator(1.4f)).start()
        } else {
            v.translationY = 0f
        }
    }

    override fun closePlayer() {
        if (!playerOpen) return
        playerOpen = false
        playerScreen.onHidden()
        val v = playerScreen.view
        v.animate().cancel()
        if (animationsEnabled()) {
            v.animate().translationY(rootFrame.height.toFloat()).setDuration(230).withEndAction {
                if (!playerOpen) v.visibility = View.GONE
            }.start()
        } else {
            v.visibility = View.GONE
        }
    }

    private fun onPlaybackChanged() {
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
        PlaybackHub.setShuffle(true)
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
