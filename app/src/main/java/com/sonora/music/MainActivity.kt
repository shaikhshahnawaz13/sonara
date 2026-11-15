package com.sonora.music

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

private data class Track(val id: Long, val title: String, val artist: String, val uri: Uri)

class MainActivity : Activity() {
    private val bg = Color.rgb(7, 10, 16)
    private val surface = Color.rgb(17, 23, 33)
    private val cyan = Color.rgb(0, 220, 245)
    private val muted = Color.rgb(151, 164, 180)
    private var player: MediaPlayer? = null
    private val tracks = mutableListOf<Track>()
    private var current: Track? = null
    private lateinit var content: LinearLayout
    private lateinit var count: TextView
    private lateinit var nowTitle: TextView
    private lateinit var playButton: TextView
    private lateinit var query: EditText
    private var page = "Home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        buildShell()
        requestAudioPermission()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 24, stroke: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }
    private fun label(s: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, Typeface.BOLD)
        includeFontPadding = false
    }
    private fun glass(): GradientDrawable = GradientDrawable(GradientDrawable.Orientation.TL_BR,
        intArrayOf(Color.rgb(31, 43, 57), Color.rgb(15, 22, 32), Color.rgb(20, 30, 42))).apply {
        cornerRadius = dp(25).toFloat(); setStroke(dp(1), Color.argb(65, 180, 225, 245))
    }
    private fun buildShell() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(14), dp(20), dp(10)); setBackgroundColor(bg) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val logo = ImageView(this).apply { setImageResource(com.sonora.music.R.drawable.sonora_logo); scaleType = ImageView.ScaleType.FIT_CENTER }
        header.addView(logo, LinearLayout.LayoutParams(dp(34), dp(34)))
        val brand = label("SONORA", 21f, Color.WHITE, true).apply { setPadding(dp(9), 0, 0, 0); letterSpacing = .08f }
        header.addView(brand)
        val live = label("  ●", 9f, cyan, true); header.addView(live)
        val spacer = View(this); header.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        val profile = label("◉", 25f, cyan, true); header.addView(profile)
        root.addView(header)
        val tagline = label("SOUND, IN ITS ELEMENT.", 9f, muted, true)
        tagline.letterSpacing = .16f
        root.addView(tagline)

        val scroll = ScrollView(this).apply { clipToPadding = false; isFillViewport = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(22), 0, dp(18)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val mini = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(13), dp(16), dp(13)); background = glass() }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val disc = label("◉", 28f, cyan, true); row.addView(disc)
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
        nowTitle = label("Nothing playing", 13f, Color.WHITE, true)
        info.addView(nowTitle); info.addView(label("LOCAL LIBRARY  ·  READY WHEN YOU ARE", 8f, muted, true))
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        playButton = label("▶", 21f, cyan, true).apply { gravity = Gravity.CENTER; setOnClickListener { togglePlayback() } }
        row.addView(playButton, LinearLayout.LayoutParams(dp(48), dp(44)))
        mini.addView(row)
        val miniLp = LinearLayout.LayoutParams(-1, -2); miniLp.bottomMargin = dp(10); root.addView(mini, miniLp)

        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(5), dp(8), dp(5), dp(8)); background = glass() }
        listOf("⌂" to "Home", "⌕" to "Search", "♫" to "Library").forEach { (icon, name) ->
            val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(12), dp(4), dp(12), dp(4)); isClickable = true; setOnClickListener { showPage(name) } }
            item.addView(label(icon, 21f, if (name == page) cyan else muted, true))
            item.addView(label(name, 9f, if (name == page) Color.WHITE else muted, true))
            nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)
        setContentView(root)
        showPage("Home")
    }

    private fun showPage(name: String) {
        page = name; content.removeAllViews()
        when (name) {
            "Home" -> homePage()
            "Search" -> searchPage()
            else -> libraryPage()
        }
    }
    private fun heading(kicker: String, title: String, subtitle: String) {
        content.addView(label(kicker.uppercase(), 9f, cyan, true))
        val h = label(title, 32f, Color.WHITE, true); h.setPadding(0, dp(8), 0, dp(7)); content.addView(h)
        content.addView(label(subtitle, 13f, muted))
    }
    private fun card(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(19), dp(19), dp(19), dp(19)); background = glass() }
    private fun action(text: String, onClick: () -> Unit): TextView = label(text, 13f, bg, true).apply {
        gravity = Gravity.CENTER; background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.rgb(0, 230, 248), Color.rgb(0, 170, 220))).apply { cornerRadius = dp(15).toFloat() }
        setPadding(dp(14), dp(15), dp(14), dp(15)); isClickable = true; setOnTouchListener { v, event -> if (event.action == android.view.MotionEvent.ACTION_DOWN) v.animate().scaleX(.985f).scaleY(.985f).setDuration(90).start() else if (event.action == android.view.MotionEvent.ACTION_UP || event.action == android.view.MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).setDuration(150).start(); false }; setOnClickListener { onClick() }
    }
    private fun homePage() {
        heading("YOUR SOUND. YOUR SPACE.", "Find your\nfrequency.", "Your music, beautifully gathered in one place.")
        val hero = card(); val lp = LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(23); content.addView(hero, lp)
        hero.addView(label("YOUR PERSONAL LIBRARY", 9f, cyan, true))
        val title = label("A world of sound,\nright on your device.", 23f, Color.WHITE, true); title.setPadding(0, dp(13), 0, dp(8)); hero.addView(title)
        count = label("${tracks.size} tracks found on this device", 12f, muted); hero.addView(count)
        val scan = action("SCAN MY MUSIC   ↗") { loadTracks() }; val scanLp = LinearLayout.LayoutParams(-1, -2); scanLp.topMargin = dp(20); hero.addView(scan, scanLp)
        sectionTitle("Recently added", "YOUR COLLECTION")
        renderTrackList(content, tracks.take(5))
    }
    private fun searchPage() {
        heading("DISCOVER YOUR COLLECTION", "Search.", "Find a track or artist in your offline library.")
        query = EditText(this).apply { hint = "  Search tracks or artists"; setHintTextColor(muted); setTextColor(Color.WHITE); textSize = 14f; singleLine = true; background = glass(); setPadding(dp(16), dp(13), dp(16), dp(13)) }
        val p = LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(22); content.addView(query, p)
        query.addTextChangedListener(SimpleWatcher { renderTrackList(content, tracks.filter { it.title.contains(query.text, true) || it.artist.contains(query.text, true) }) })
        sectionTitle("Matches", "LOCAL SEARCH"); renderTrackList(content, tracks)
    }
    private fun libraryPage() {
        heading("YOUR MUSIC, OFFLINE", "Library.", "Everything stored locally on this device.")
        val c = card(); val p = LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(22); content.addView(c, p)
        c.addView(label("${tracks.size} TRACKS", 21f, Color.WHITE, true)); c.addView(label("Available for local playback", 12f, muted))
        val scan = action("RESCAN DEVICE  ↗") { loadTracks() }; val a = LinearLayout.LayoutParams(-1, -2); a.topMargin = dp(15); c.addView(scan, a)
        sectionTitle("All tracks", "OFFLINE LIBRARY"); renderTrackList(content, tracks)
    }
    private fun sectionTitle(title: String, over: String) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(25), 0, dp(12)) }
        box.addView(label(over, 9f, cyan, true)); box.addView(label(title, 21f, Color.WHITE, true)); content.addView(box)
    }
    private fun renderTrackList(parent: LinearLayout, items: List<Track>) {
        if (items.isEmpty()) {
            val empty = card(); empty.addView(label(if (tracks.isEmpty()) "Your library is quiet for now." else "No matching tracks.", 15f, Color.WHITE, true))
            empty.addView(label(if (tracks.isEmpty()) "Scan your device to find music files." else "Try another title or artist.", 12f, muted))
            parent.addView(empty); return
        }
        items.forEachIndexed { i, track ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(11), dp(12), dp(11)); background = shape(surface, 17, Color.rgb(35, 51, 65)); isClickable = true; setOnClickListener { play(track) } }
            val art = label("♫", 22f, cyan, true).apply { gravity = Gravity.CENTER; background = shape(Color.rgb(14, 48, 60), 13) }
            row.addView(art, LinearLayout.LayoutParams(dp(45), dp(45)))
            val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(6), 0) }
            texts.addView(label(track.title, 13f, Color.WHITE, true)); texts.addView(label(track.artist, 11f, muted))
            row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(label("⋯", 21f, muted))
            val p = LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(8); parent.addView(row, p)
        }
    }
    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) loadTracks() else requestPermissions(arrayOf(permission), 41)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 41 && results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) loadTracks()
    }
    private fun loadTracks() {
        tracks.clear(); val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.IS_MUSIC)
        val cursor: Cursor? = contentResolver.query(collection, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")
        cursor?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID); val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE); val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (c.moveToNext()) { val n = c.getLong(id); tracks.add(Track(n, c.getString(title) ?: "Unknown track", c.getString(artist) ?: "Unknown artist", ContentUris.withAppendedId(collection, n))) }
        }
        if (::count.isInitialized) count.text = "${tracks.size} tracks found on this device"
        showPage(page)
    }
    private fun play(track: Track) {
        try {
            player?.release(); current = track
            player = MediaPlayer().apply { setDataSource(this@MainActivity, track.uri); setOnPreparedListener { it.start(); playButton.text = "Ⅱ" }; setOnCompletionListener { playButton.text = "▶" }; prepareAsync() }
            nowTitle.text = track.title
        } catch (e: Exception) { nowTitle.text = "Unable to play this track" }
    }
    private fun togglePlayback() {
        val p = player ?: run { if (tracks.isNotEmpty()) play(tracks.first()); return }
        if (p.isPlaying) { p.pause(); playButton.text = "▶" } else { p.start(); playButton.text = "Ⅱ" }
    }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}

private class SimpleWatcher(val changed: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed() }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
