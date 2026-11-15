package com.sonora.music

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.drawable.GradientDrawable

data class Track(val id: Long, val title: String, val artist: String, val uri: Uri)

class MainActivity : Activity() {
    private val bg = Color.rgb(9, 10, 13)
    private val panel = Color.rgb(20, 22, 27)
    private val accent = Color.rgb(185, 243, 107)
    private var player: MediaPlayer? = null
    private var tracks = mutableListOf<Track>()
    private var current: Track? = null
    private lateinit var list: LinearLayout
    private lateinit var nowTitle: TextView
    private lateinit var playButton: Button
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildScreen()
        requestAudioPermission()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 22): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(12))
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = text("SONORA", 23f, Color.WHITE, true)
        val dot = text("  ●", 13f, accent, true)
        header.addView(brand)
        header.addView(dot)
        val spacer = View(this)
        header.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(text("YOUR SOUND, YOUR SPACE", 9f, Color.LTGRAY, true))
        root.addView(header)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = rounded(panel, 26)
        }
        val eyebrow = text("YOUR PERSONAL LIBRARY", 10f, accent, true)
        hero.addView(eyebrow)
        val heading = text("Music,\nwithout limits.", 31f, Color.WHITE, true)
        heading.setPadding(0, dp(10), 0, dp(8))
        hero.addView(heading)
        status = text("Your on-device collection lives here.", 13f, Color.LTGRAY)
        hero.addView(status)
        val scan = Button(this).apply {
            text = "SCAN MY MUSIC"
            setTextColor(bg)
            setBackgroundColor(accent)
            isAllCaps = false
            setOnClickListener { loadTracks() }
        }
        val scanLp = LinearLayout.LayoutParams(-1, dp(48))
        scanLp.topMargin = dp(18)
        hero.addView(scan, scanLp)
        val heroLp = LinearLayout.LayoutParams(-1, -2)
        heroLp.topMargin = dp(24)
        root.addView(hero, heroLp)

        val section = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(24), 0, dp(12))
        }
        section.addView(text("Your tracks", 21f, Color.WHITE, true))
        val s2 = View(this)
        section.addView(s2, LinearLayout.LayoutParams(0, 1, 1f))
        section.addView(text("OFFLINE LIBRARY", 9f, accent, true))
        root.addView(section)

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(panel, 20)
        }
        nowTitle = text("Nothing playing yet", 14f, Color.WHITE, true)
        bottom.addView(nowTitle)
        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        controls.addView(text("LOCAL PLAYBACK", 9f, Color.LTGRAY, true), LinearLayout.LayoutParams(0, -2, 1f))
        playButton = Button(this).apply {
            text = "PLAY"
            setTextColor(bg)
            setBackgroundColor(accent)
            isAllCaps = false
            setOnClickListener { togglePlayback() }
        }
        controls.addView(playButton, LinearLayout.LayoutParams(dp(100), dp(42)))
        bottom.addView(controls)
        val bottomLp = LinearLayout.LayoutParams(-1, -2)
        bottomLp.topMargin = dp(12)
        root.addView(bottom, bottomLp)
        setContentView(root)
        emptyState()
    }

    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                         else Manifest.permission.READ_EXTERNAL_STORAGE
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) loadTracks()
        else requestPermissions(arrayOf(permission), 41)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 41 && results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) loadTracks()
        else status.text = "Allow audio access to scan music stored on this device."
    }

    private fun loadTracks() {
        tracks.clear()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.IS_MUSIC)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val cursor: Cursor? = contentResolver.query(collection, projection, selection, null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol) ?: "Unknown track"
                val artist = it.getString(artistCol) ?: "Unknown artist"
                tracks.add(Track(id, title, artist, ContentUris.withAppendedId(collection, id)))
            }
        }
        status.text = "${tracks.size} tracks found on this device."
        renderTracks()
    }

    private fun emptyState() {
        list.removeAllViews()
        list.addView(text("Your library is waiting.", 15f, Color.LTGRAY))
        list.addView(text("Tap SCAN MY MUSIC to find audio files stored on your phone.", 12f, Color.GRAY))
    }

    private fun renderTracks() {
        list.removeAllViews()
        if (tracks.isEmpty()) { emptyState(); return }
        tracks.forEach { track ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = rounded(panel, 18)
                setOnClickListener { play(track) }
            }
            val art = TextView(this).apply {
                text = "♫"
                textSize = 23f
                gravity = Gravity.CENTER
                setTextColor(accent)
                background = rounded(Color.rgb(38, 43, 35), 14)
            }
            row.addView(art, LinearLayout.LayoutParams(dp(48), dp(48)))
            val labels = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
            }
            labels.addView(text(track.title, 14f, Color.WHITE, true))
            labels.addView(text(track.artist, 11f, Color.LTGRAY))
            row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(text("›", 26f, accent))
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.bottomMargin = dp(9)
            list.addView(row, lp)
        }
    }

    private fun play(track: Track) {
        try {
            player?.release()
            current = track
            player = MediaPlayer().apply {
                setDataSource(this@MainActivity, track.uri)
                setOnPreparedListener { it.start(); playButton.text = "PAUSE" }
                setOnCompletionListener { playButton.text = "PLAY" }
                prepareAsync()
            }
            nowTitle.text = track.title
        } catch (e: Exception) {
            status.text = "Couldn't play this track: ${e.localizedMessage ?: "unknown error"}"
        }
    }

    private fun togglePlayback() {
        val p = player
        if (p == null) {
            if (tracks.isNotEmpty()) play(tracks.first())
            else status.text = "Scan your music first."
            return
        }
        if (p.isPlaying) { p.pause(); playButton.text = "PLAY" }
        else { p.start(); playButton.text = "PAUSE" }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
