package com.sonora.music

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.drawable.Drawable
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
import android.text.TextWatcher
import android.text.Editable

private data class Track(val id: Long, val title: String, val artist: String, val uri: Uri)

class MainActivity : Activity() {
    private val bg = Color.rgb(5, 9, 14)
    private val glassTop = Color.rgb(29, 43, 55)
    private val glassBottom = Color.rgb(12, 20, 29)
    private val cyan = Color.rgb(54, 218, 239)
    private val muted = Color.rgb(157, 172, 187)
    private val white = Color.rgb(245, 248, 250)
    private val tracks = mutableListOf<Track>()
    private var player: MediaPlayer? = null
    private var current: Track? = null
    private var page = "Home"
    private var category = "Music"
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var miniTitle: TextView
    private lateinit var miniArtist: TextView
    private lateinit var miniPlay: TextView
    private lateinit var trackCount: TextView
    private var miniVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        showSplash()
        requestAudioPermission()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dp(v: Float) = (v * resources.displayMetrics.density).toInt()
    private fun text(s: String, size: Float, color: Int = white, bold: Boolean = false): TextView = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); includeFontPadding = false
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun gradient(colors: IntArray, radius: Int = 22, stroke: Int = Color.argb(75, 174, 211, 230)): GradientDrawable = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
        cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke)
    }
    /** Shared premium glass material. Visual-only: no timing or motion behaviour is introduced here. */
    private fun glass(radius: Int = 22): Drawable = LiquidGlassDrawable(dp(radius).toFloat())

    private inner class LiquidGlassDrawable(private val corner: Float) : Drawable() {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val light = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val path = Path()
        private val rect = RectF()

        override fun draw(canvas: Canvas) {
            val b = bounds
            if (b.isEmpty) return
            rect.set(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
            path.reset(); path.addRoundRect(rect, corner, corner, Path.Direction.CW)
            val save = canvas.save(); canvas.clipPath(path)

            // Deep smoked-glass body. Lower opacity lets the underlying artwork contribute to the surface.
            fill.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                intArrayOf(Color.argb(150, 34, 48, 62), Color.argb(174, 16, 27, 39), Color.argb(194, 5, 11, 19)),
                floatArrayOf(0f, .52f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(rect, corner, corner, fill)

            // Broad, low-contrast reflected environment light.
            fill.shader = LinearGradient(rect.left, rect.top, rect.left + rect.width() * .7f, rect.bottom * .55f,
                intArrayOf(Color.argb(35, 225, 240, 248), Color.argb(8, 155, 194, 216), Color.TRANSPARENT),
                floatArrayOf(0f, .42f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(rect, corner, corner, fill)

            // Fine softbox reflection along the upper edge, not a neon glow.
            val topBand = RectF(rect.left + corner * .45f, rect.top + dp(1).toFloat(), rect.right - corner * .45f, rect.top + rect.height() * .19f)
            light.shader = LinearGradient(0f, topBand.top, 0f, topBand.bottom,
                intArrayOf(Color.argb(52, 246, 252, 255), Color.argb(13, 210, 231, 242), Color.TRANSPARENT),
                floatArrayOf(0f, .28f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(topBand, corner, corner, light)

            // Restrained diagonal sheen gives the glass a curved, reflective read.
            val sheen = Path().apply {
                moveTo(rect.left, rect.top + rect.height() * .28f)
                quadTo(rect.left + rect.width() * .28f, rect.top - rect.height() * .04f, rect.right * .78f, rect.top)
                lineTo(rect.right, rect.top)
                quadTo(rect.left + rect.width() * .56f, rect.top + rect.height() * .10f, rect.left, rect.top + rect.height() * .36f)
                close()
            }
            light.shader = LinearGradient(rect.left, rect.top, rect.right, rect.top + rect.height() * .2f,
                intArrayOf(Color.TRANSPARENT, Color.argb(17, 240, 249, 255), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            canvas.drawPath(sheen, light)
            canvas.restoreToCount(save)

            // Cool outer refraction line and a much quieter inner hairline.
            stroke.strokeWidth = dp(1.15f).toFloat()
            stroke.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                intArrayOf(Color.argb(180, 207, 232, 244), Color.argb(62, 110, 157, 185), Color.argb(126, 207, 226, 239)), null, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(rect, corner, corner, stroke)
            stroke.shader = null
            val inset = dp(1.6f).toFloat()
            val inner = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
            innerStroke.strokeWidth = dp(.65f).toFloat(); innerStroke.color = Color.argb(34, 244, 250, 255)
            canvas.drawRoundRect(inner, (corner - inset).coerceAtLeast(0f), (corner - inset).coerceAtLeast(0f), innerStroke)
        }
        override fun setAlpha(alpha: Int) { fill.alpha = alpha; light.alpha = alpha; stroke.alpha = alpha; innerStroke.alpha = alpha; invalidateSelf() }
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { fill.colorFilter = colorFilter; invalidateSelf() }
        @Deprecated("Deprecated in Android")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    }

    /** Raised refractive tab, inspired by the supplied floating-glass navigation reference. */
    private inner class ActiveNavGlassDrawable : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(1.2f).toFloat() }
        private val capsule = Paint(Paint.ANTI_ALIAS_FLAG)
        private val r = RectF()
        override fun draw(canvas: Canvas) {
            val b = bounds; if (b.isEmpty) return
            r.set(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
            val radius = minOf(r.width(), r.height()) * .28f
            paint.shader = LinearGradient(r.left, r.top, r.right, r.bottom,
                intArrayOf(Color.rgb(15, 19, 24), Color.rgb(3, 5, 8), Color.rgb(19, 25, 32)), null, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(r, radius, radius, paint); paint.shader = null
            // The characteristic bright upper and lower glass lips from the reference.
            val capW = r.width() * .68f; val capH = dp(8).toFloat(); val left = r.centerX() - capW / 2f
            val topCap = RectF(left, r.top + dp(2), left + capW, r.top + dp(2) + capH)
            capsule.shader = LinearGradient(0f, topCap.top, 0f, topCap.bottom,
                intArrayOf(Color.argb(238, 240, 247, 250), Color.argb(188, 185, 202, 214), Color.argb(18, 71, 135, 170)), null, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(topCap, capH, capH, capsule)
            val bottomCap = RectF(left, r.bottom - dp(2) - capH, left + capW, r.bottom - dp(2))
            capsule.shader = LinearGradient(0f, bottomCap.top, 0f, bottomCap.bottom,
                intArrayOf(Color.argb(18, 71, 135, 170), Color.argb(176, 184, 202, 215), Color.argb(220, 235, 243, 248)), null, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(bottomCap, capH, capH, capsule); capsule.shader = null
            rim.shader = LinearGradient(r.left, r.top, r.right, r.bottom,
                intArrayOf(Color.argb(210, 220, 237, 246), Color.argb(70, 111, 155, 181), Color.argb(180, 216, 233, 242)), null, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(r, radius, radius, rim); rim.shader = null
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha; capsule.alpha = alpha; rim.alpha = alpha; invalidateSelf() }
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
        @Deprecated("Deprecated in Android")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    }
    private fun column(pad: Int = 0) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; if (pad > 0) setPadding(dp(pad), dp(pad), dp(pad), dp(pad)) }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun labelView(s: String, size: Float, color: Int = cyan) = text(s.uppercase(), size, color, true).apply { letterSpacing = .08f }

    private fun showSplash() {
        val splash = FrameLayout(this).apply { setBackgroundColor(bg) }
        val center = column().apply { gravity = Gravity.CENTER }
        val mark = text("∿", 78f, cyan, true).apply { gravity = Gravity.CENTER; letterSpacing = -.08f }
        center.addView(mark)
        center.addView(text("S O N A R A", 24f, white).apply { gravity = Gravity.CENTER; letterSpacing = .12f })
        center.addView(text("I M M E R S I V E   M U S I C", 9f, muted).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, 0) })
        splash.addView(center, FrameLayout.LayoutParams(-1, -1))
        val line = View(this).apply { background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(cyan, Color.rgb(34, 105, 132))).apply { cornerRadius = dp(4).toFloat() } }
        val lp = FrameLayout.LayoutParams(dp(118), dp(2), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL); lp.bottomMargin = dp(54); splash.addView(line, lp)
        setContentView(splash)
        splash.postDelayed({ buildShell(); showPage("Home") }, 850)
    }

    private fun buildShell() {
        root = column().apply { setPadding(dp(18), dp(12), dp(18), dp(9)); setBackgroundColor(bg); clipChildren = false; clipToPadding = false }
        val header = row()
        header.addView(text("∿", 30f, cyan, true))
        header.addView(text("S O N A R A", 19f, white).apply { setPadding(dp(8), 0, 0, 0); letterSpacing = .08f })
        val spacer = View(this); header.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(text("◉", 24f, muted).apply { setOnClickListener { showPage("Settings") } })
        root.addView(header)
        root.addView(text("I M M E R S I V E   M U S I C", 8f, muted).apply { letterSpacing = .18f; setPadding(dp(3), dp(2), 0, dp(5)) })

        val scroll = ScrollView(this).apply { clipToPadding = false; isFillViewport = true }
        content = column().apply { setPadding(0, dp(17), 0, dp(18)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val mini = row().apply { background = glass(23); setPadding(dp(13), dp(11), dp(10), dp(11)); setOnClickListener { if (current != null) showPlayer() } }
        mini.addView(text("◉", 27f, cyan, true))
        val info = column().apply { setPadding(dp(11), 0, 0, 0) }
        miniTitle = text("Nothing playing", 13f, white, true)
        miniArtist = text("YOUR SONARA EXPERIENCE", 8f, muted)
        info.addView(miniTitle); info.addView(miniArtist)
        mini.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        miniPlay = text("▶", 20f, cyan, true).apply { gravity = Gravity.CENTER; setOnClickListener { togglePlayback() } }
        mini.addView(miniPlay, LinearLayout.LayoutParams(dp(42), dp(38)))
        val miniLp = LinearLayout.LayoutParams(-1, -2); miniLp.bottomMargin = dp(9); root.addView(mini, miniLp)

        root.addView(buildNavigationBar())
        setContentView(root)
    }

    private fun showPage(name: String) {
        page = name
        if (!::content.isInitialized) return
        content.removeAllViews()
        when (name) {
            "Home" -> homePage()
            "Search" -> searchPage()
            "Library" -> libraryPage()
            "Settings" -> settingsPage()
            else -> homePage()
        }
        refreshNav()
    }

    /**
     * SONARA v0.5 navigation: three-position black rail with a raised refractive
     * active capsule. The reference uses icon-only inactive tabs and an icon +
     * label inside the floating selected capsule. No new motion is introduced.
     */
    private fun buildNavigationBar(): FrameLayout {
        val bar = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        val rail = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(3, 4, 6), Color.rgb(7, 8, 10), Color.rgb(3, 4, 6))
            ).apply { cornerRadius = dp(40).toFloat() }
        }
        val railLp = FrameLayout.LayoutParams(-1, dp(58), Gravity.CENTER_VERTICAL)
        railLp.leftMargin = dp(2); railLp.rightMargin = dp(2)
        bar.addView(rail, railLp)

        val slots = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            clipChildren = false
            clipToPadding = false
        }
        val slotLp = FrameLayout.LayoutParams(-1, -1)
        bar.addView(slots, slotLp)

        val destinations = listOf(
            Triple("Home", NavGlyph.HOME, "Home"),
            Triple("Search", NavGlyph.SEARCH, "Search"),
            Triple("Library", NavGlyph.LIBRARY, "Library")
        )
        destinations.forEach { (name, glyph, label) ->
            val selected = page == name
            val slot = FrameLayout(this).apply {
                clipChildren = false
                clipToPadding = false
                isClickable = true
                isFocusable = true
                contentDescription = label
                setOnClickListener { if (page != name) showPage(name) }
            }
            if (selected) {
                val active = FrameLayout(this).apply {
                    background = ActiveNavGlassDrawable()
                    clipChildren = false
                    clipToPadding = false
                    elevation = dp(8).toFloat()
                }
                val activeContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                }
                activeContent.addView(NavGlyph(this@MainActivity, glyph, white, true),
                    LinearLayout.LayoutParams(dp(29), dp(29)))
                activeContent.addView(text(label, 14f, white, false).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(7), 0, 0)
                    letterSpacing = 0f
                })
                active.addView(activeContent, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
                slot.addView(active, FrameLayout.LayoutParams(-1, dp(82), Gravity.CENTER))
            } else {
                slot.addView(NavGlyph(this, glyph, Color.rgb(91, 94, 99), false),
                    FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER))
            }
            slots.addView(slot, LinearLayout.LayoutParams(0, -1, 1f))
        }
        val lp = LinearLayout.LayoutParams(-1, dp(82))
        lp.topMargin = dp(1)
        bar.layoutParams = lp
        return bar
    }

    private class NavGlyph(
        context: android.content.Context,
        private val glyph: Int,
        private val tint: Int,
        private val active: Boolean
    ) : View(context) {
        companion object { const val HOME = 0; const val SEARCH = 1; const val LIBRARY = 2 }
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
            style = Paint.Style.STROKE
            strokeWidth = context.resources.displayMetrics.density * 2.25f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val path = Path()
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val cx = width / 2f; val cy = height / 2f
            val scale = minOf(width, height) * .72f / 24f
            canvas.save(); canvas.translate(cx - 12f * scale, cy - 12f * scale); canvas.scale(scale, scale)
            when (glyph) {
                HOME -> {
                    path.reset(); path.moveTo(2.5f, 10.8f); path.lineTo(12f, 3f); path.lineTo(21.5f, 10.8f)
                    path.moveTo(5f, 9.5f); path.lineTo(5f, 21f); path.lineTo(19f, 21f); path.lineTo(19f, 9.5f)
                    path.moveTo(9.5f, 21f); path.lineTo(9.5f, 14.5f); path.lineTo(14.5f, 14.5f); path.lineTo(14.5f, 21f)
                    canvas.drawPath(path, paint)
                }
                SEARCH -> {
                    canvas.drawCircle(10.5f, 10.5f, 6.5f, paint)
                    path.reset(); path.moveTo(15.5f, 15.5f); path.lineTo(21f, 21f); canvas.drawPath(path, paint)
                }
                LIBRARY -> {
                    path.reset(); path.moveTo(7f, 4f); path.lineTo(7f, 18f)
                    path.moveTo(7f, 6f); path.lineTo(19f, 3f); path.lineTo(19f, 17f)
                    canvas.drawPath(path, paint)
                    canvas.drawOval(3.5f, 16.5f, 10.5f, 21.5f, paint)
                    canvas.drawOval(15.5f, 15.5f, 22.5f, 20.5f, paint)
                }
            }
            canvas.restore()
        }
    }

    private fun refreshNav() {
        val index = root.childCount - 1
        if (index < 0) return
        root.removeViewAt(index)
        root.addView(buildNavigationBar())
    }

    private fun heading(kicker: String, title: String, subtitle: String? = null) {
        content.addView(labelView(kicker, 9f))
        content.addView(text(title, 32f, white).apply { setPadding(0, dp(11), 0, dp(6)) })
        if (subtitle != null) content.addView(text(subtitle, 13f, muted))
    }
    private fun section(title: String, right: String? = null) {
        val r = row().apply { setPadding(0, dp(24), 0, dp(12)) }
        r.addView(text(title, 18f, white, true), LinearLayout.LayoutParams(0, -2, 1f))
        if (right != null) r.addView(text(right, 10f, muted))
        content.addView(r)
    }
    private fun panel(): LinearLayout = column(16).apply { background = glass(23) }
    private fun artCard(title: String, subtitle: String, colors: IntArray, width: Int = 150): LinearLayout {
        val card = column(0).apply { background = gradient(colors, 18, Color.argb(100, 178, 219, 237)); setPadding(dp(12), dp(12), dp(12), dp(11)) }
        val image = FrameLayout(this).apply { background = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply { cornerRadius = dp(13).toFloat() } }
        image.addView(text("◌", 42f, Color.argb(190, 220, 244, 250)).apply { gravity = Gravity.CENTER })
        card.addView(image, LinearLayout.LayoutParams(-1, dp(100)))
        card.addView(text(title, 12f, white, true).apply { setPadding(0, dp(9), 0, dp(3)) })
        card.addView(text(subtitle, 9f, muted))
        card.layoutParams = LinearLayout.LayoutParams(dp(width), -2)
        return card
    }
    private fun horizontalCards(items: List<Triple<String, String, IntArray>>) {
        val strip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        items.forEachIndexed { index, item ->
            val c = artCard(item.first, item.second, item.third)
            val lp = LinearLayout.LayoutParams(dp(150), -2); if (index > 0) lp.leftMargin = dp(10); strip.addView(c, lp)
        }
        val scroller = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(strip) }
        content.addView(scroller)
    }

    private fun homePage() {
        val greeting = row().apply { gravity = Gravity.CENTER_VERTICAL }
        val greet = column()
        greet.addView(text("Good evening,", 12f, muted))
        greet.addView(text("Your sound space", 23f, white, true).apply { setPadding(0, dp(3), 0, 0) })
        greeting.addView(greet, LinearLayout.LayoutParams(0, -2, 1f))
        greeting.addView(text("◉", 25f, muted).apply { setOnClickListener { showPage("Settings") } })
        content.addView(greeting)
        val tabs = row().apply { setPadding(0, dp(17), 0, dp(17)) }
        listOf("Music", "Podcasts", "Radio").forEach { name ->
            val t = text(name, 11f, if (category == name) Color.rgb(5, 22, 30) else muted, true).apply {
                gravity = Gravity.CENTER; setPadding(dp(18), dp(9), dp(18), dp(9)); background = if (category == name) GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.rgb(111, 218, 235), Color.rgb(188, 235, 240))).apply { cornerRadius = dp(22).toFloat() } else glass(22)
                setOnClickListener { category = name; showPage("Home") }
            }
            val lp = LinearLayout.LayoutParams(-2, -2); if (tabs.childCount > 0) lp.leftMargin = dp(8); tabs.addView(t, lp)
        }
        content.addView(tabs)
        val hero = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(13, 45, 61), Color.rgb(19, 35, 49), Color.rgb(7, 13, 21))).apply { cornerRadius = dp(22).toFloat(); setStroke(dp(1), Color.argb(100, 173, 213, 229)) }
            setPadding(dp(17), dp(18), dp(17), dp(16))
        }
        val moon = text("◌", 76f, Color.argb(190, 194, 226, 238)).apply { gravity = Gravity.RIGHT; alpha = .7f }
        hero.addView(moon, FrameLayout.LayoutParams(-1, dp(115), Gravity.TOP or Gravity.RIGHT))
        val copy = column().apply { gravity = Gravity.BOTTOM }
        copy.addView(text("FEATURED MIX", 8f, cyan, true))
        copy.addView(text("Midnight\nVibes", 25f, white, true).apply { setPadding(0, dp(8), 0, dp(5)) })
        copy.addView(text("A curated flow for your late nights.", 10f, muted))
        hero.addView(copy, FrameLayout.LayoutParams(-1, -1))
        hero.setOnClickListener { if (tracks.isNotEmpty()) play(tracks.first()) else showPage("Search") }
        content.addView(hero, LinearLayout.LayoutParams(-1, dp(190)))
        section("Made for You", "SEE ALL  ›")
        horizontalCards(listOf(
            Triple("Daily Mix 1", "Your favourites", intArrayOf(Color.rgb(64, 132, 157), Color.rgb(18, 41, 59))),
            Triple("Daily Mix 2", "New discoveries", intArrayOf(Color.rgb(37, 78, 98), Color.rgb(11, 25, 38))),
            Triple("After Hours", "Slow down", intArrayOf(Color.rgb(60, 82, 103), Color.rgb(12, 21, 34)))
        ))
        section("Recently played", "YOUR MUSIC")
        if (tracks.isEmpty()) emptyState("Your library is waiting.", "Scan your device to bring your music into SONARA.") else renderTrackList(content, tracks.take(4))
    }

    private fun searchPage() {
        heading("FIND YOUR SOUND", "Search", "Songs, artists, albums and moods.")
        val input = EditText(this).apply {
            hint = "  Songs, artists, albums..."; setHintTextColor(muted); setTextColor(white); textSize = 13f; singleLine = true; background = glass(28); setPadding(dp(18), dp(14), dp(18), dp(14))
        }
        val lp = LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(20); content.addView(input, lp)
        section("Browse", "EXPLORE")
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val moodRows = listOf(listOf("Chill", "Focus"), listOf("Workout", "Sleep"))
        val palettes = listOf(intArrayOf(Color.rgb(69, 126, 153), Color.rgb(15, 34, 52)), intArrayOf(Color.rgb(48, 72, 90), Color.rgb(10, 19, 29)), intArrayOf(Color.rgb(44, 76, 90), Color.rgb(12, 24, 34)), intArrayOf(Color.rgb(44, 63, 88), Color.rgb(8, 16, 29)))
        moodRows.forEachIndexed { r, names ->
            val line = row()
            names.forEachIndexed { c, name ->
                val index = r * 2 + c
                val tile = FrameLayout(this).apply { background = gradient(palettes[index], 17); setOnClickListener { input.setText(name) } }
                tile.addView(text("◌", 43f, Color.argb(170, 213, 236, 243)).apply { gravity = Gravity.TOP or Gravity.RIGHT; setPadding(0, dp(5), dp(9), 0) })
                tile.addView(text(name, 14f, white, true).apply { setPadding(dp(13), 0, 0, dp(12)); gravity = Gravity.BOTTOM })
                val params = LinearLayout.LayoutParams(0, dp(112), 1f); if (c == 1) params.leftMargin = dp(9); line.addView(tile, params)
            }
            val rowLp = LinearLayout.LayoutParams(-1, -2); if (r > 0) rowLp.topMargin = dp(9); grid.addView(line, rowLp)
        }
        content.addView(grid)
        section("Genres", "SEE ALL  ›")
        val chips = row()
        listOf("Pop", "Rock", "Hip-Hop", "Electronic").forEach { name ->
            val chip = text(name, 10f, white).apply { background = glass(20); setPadding(dp(13), dp(9), dp(13), dp(9)) }
            val p = LinearLayout.LayoutParams(-2, -2); p.rightMargin = dp(7); chips.addView(chip, p)
        }
        content.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) })
        section("Your offline matches", "ON DEVICE")
        val results = column()
        content.addView(results)
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { results.removeAllViews(); val q = s?.toString().orEmpty(); renderTrackList(results, if (q.isBlank()) emptyList() else tracks.filter { it.title.contains(q, true) || it.artist.contains(q, true) }) }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun libraryPage() {
        heading("YOUR COLLECTION", "Your Library", "Everything you save, gathered in one place.")
        val entries = listOf("Liked Songs" to "♡", "Playlists" to "♫", "Albums" to "◉", "Artists" to "♙", "Downloads" to "↓")
        entries.forEachIndexed { i, entry ->
            val item = row().apply { background = glass(17); setPadding(dp(14), dp(12), dp(13), dp(12)); isClickable = true; setOnClickListener { showCollection(entry.first) } }
            item.addView(text(entry.second, 22f, if (i == 0) cyan else muted, true).apply { gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(dp(36), dp(40)) })
            val labels = column().apply { setPadding(dp(10), 0, 0, 0) }
            labels.addView(text(entry.first, 13f, white, true))
            labels.addView(text(when (i) { 0 -> "${tracks.size} songs"; 4 -> "On this device"; else -> "Your collection" }, 9f, muted).apply { setPadding(0, dp(4), 0, 0) })
            item.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
            item.addView(text("›", 22f, muted))
            val p = LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(8); content.addView(item, p)
        }
        section("Recently added", "LOCAL FILES")
        if (tracks.isEmpty()) emptyState("Your library is quiet for now.", "Scan your device to find music files.") else renderTrackList(content, tracks.take(8))
        val scan = actionButton("SCAN DEVICE") { loadTracks() }
        val lp = LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(16); content.addView(scan, lp)
    }

    private fun settingsPage() {
        heading("PERSONALISE YOUR EXPERIENCE", "Settings", "Make SONARA feel like yours.")
        val brand = FrameLayout(this).apply { background = gradient(intArrayOf(Color.rgb(30, 61, 77), Color.rgb(10, 18, 28)), 20); setPadding(dp(18), dp(18), dp(18), dp(18)) }
        brand.addView(text("∿", 42f, cyan, true).apply { gravity = Gravity.RIGHT or Gravity.TOP })
        val b = column(); b.addView(text("S O N A R A", 20f, white).apply { letterSpacing = .12f }); b.addView(text("VERSION 0.4.0", 9f, muted).apply { setPadding(0, dp(5), 0, 0) })
        brand.addView(b, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER_VERTICAL))
        content.addView(brand, LinearLayout.LayoutParams(-1, dp(112)))
        section("Preferences")
        listOf("Appearance" to "Dark", "Audio" to "Playback settings", "Downloads" to "Device storage", "Equalizer" to "Sound profile", "About" to "SONARA 0.4.0").forEach { (name, detail) ->
            val item = row().apply { background = glass(18); setPadding(dp(14), dp(15), dp(13), dp(15)); isClickable = true; setOnClickListener { Toast.makeText(this@MainActivity, "$name settings will be available in the next feature pass", Toast.LENGTH_SHORT).show() } }
            item.addView(text("◈", 19f, muted).apply { setPadding(0, 0, dp(12), 0) })
            item.addView(text(name, 12f, white, true), LinearLayout.LayoutParams(0, -2, 1f))
            item.addView(text("$detail   ›", 9f, muted))
            val p = LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(8); content.addView(item, p)
        }
    }

    private fun showCollection(name: String) {
        page = "Collection"
        content.removeAllViews(); heading("YOUR MUSIC", name, "Your SONARA collection.")
        if (name == "Downloads" || name == "Albums" || name == "Artists" || name == "Playlists" || name == "Liked Songs") renderTrackList(content, tracks) else emptyState("Nothing here yet.", "Your collection will appear here.")
        val back = actionButton("BACK TO LIBRARY") { showPage("Library") }
        val p = LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(18); content.addView(back, p)
    }

    private fun showPlayer() {
        page = "Player"
        content.removeAllViews()
        val top = row().apply { setPadding(0, dp(4), 0, dp(20)) }
        top.addView(text("⌄", 27f, white).apply { setOnClickListener { showPage("Home") } })
        val from = text("PLAYING FROM\nYOUR LIBRARY", 8f, muted, true).apply { gravity = Gravity.CENTER; letterSpacing = .08f }
        top.addView(from, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(text("•••", 18f, white))
        content.addView(top)
        val artwork = FrameLayout(this).apply { background = gradient(intArrayOf(Color.rgb(31, 67, 83), Color.rgb(10, 22, 34), Color.rgb(4, 10, 17)), 23); setElevation(dp(5).toFloat()) }
        artwork.addView(text("◌", 112f, Color.argb(190, 200, 231, 240)).apply { gravity = Gravity.CENTER; alpha = .8f })
        val artLp = LinearLayout.LayoutParams(-1, dp(320)); artLp.bottomMargin = dp(25); content.addView(artwork, artLp)
        val track = current
        val meta = row().apply { gravity = Gravity.CENTER_VERTICAL }
        val names = column(); names.addView(text(track?.title ?: "Choose a track", 20f, white, true)); names.addView(text(track?.artist ?: "Your music library", 12f, muted).apply { setPadding(0, dp(6), 0, 0) })
        meta.addView(names, LinearLayout.LayoutParams(0, -2, 1f)); meta.addView(text("♡", 27f, white))
        content.addView(meta)
        val seek = SeekBar(this).apply { progressTintList = android.content.res.ColorStateList.valueOf(cyan); thumbTintList = android.content.res.ColorStateList.valueOf(cyan); max = 100; progress = 0 }
        val seekLp = LinearLayout.LayoutParams(-1, -2); seekLp.topMargin = dp(23); content.addView(seek, seekLp)
        val times = row().apply { addView(text("0:00", 9f, muted), LinearLayout.LayoutParams(0, -2, 1f)); addView(text("--:--", 9f, muted)) }; content.addView(times)
        val controls = row().apply { gravity = Gravity.CENTER; setPadding(0, dp(23), 0, dp(15)) }
        controls.addView(text("⤨", 21f, muted).apply { setPadding(dp(10), 0, dp(10), 0) })
        controls.addView(text("|◀", 23f, white).apply { setPadding(dp(10), 0, dp(10), 0); setOnClickListener { previousTrack() } })
        controls.addView(text(if (player?.isPlaying == true) "Ⅱ" else "▶", 26f, white, true).apply { gravity = Gravity.CENTER; background = glass(50); setOnClickListener { togglePlayback(); showPlayer() }; layoutParams = LinearLayout.LayoutParams(dp(64), dp(64)) })
        controls.addView(text("▶|", 23f, white).apply { setPadding(dp(10), 0, dp(10), 0); setOnClickListener { nextTrack() } })
        controls.addView(text("↻", 21f, muted).apply { setPadding(dp(10), 0, dp(10), 0) })
        content.addView(controls)
        content.addView(text("QUEUE                                      AUDIO OUTPUT", 9f, muted, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(15), 0, 0) })
    }

    private fun emptyState(title: String, subtitle: String) {
        val p = panel(); p.addView(text(title, 15f, white, true)); p.addView(text(subtitle, 11f, muted).apply { setPadding(0, dp(5), 0, 0) }); content.addView(p)
    }
    private fun actionButton(title: String, action: () -> Unit): TextView = text(title, 12f, Color.rgb(4, 20, 27), true).apply {
        gravity = Gravity.CENTER; background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.rgb(94, 220, 237), Color.rgb(34, 169, 202))).apply { cornerRadius = dp(18).toFloat() }; setPadding(dp(15), dp(15), dp(15), dp(15)); setOnClickListener { action() }
    }
    private fun renderTrackList(parent: LinearLayout, items: List<Track>) {
        if (items.isEmpty()) { if (parent.childCount == 0) { val empty = panel(); empty.addView(text("No tracks found", 14f, white, true)); empty.addView(text("Add audio files to your device and scan again.", 10f, muted)); parent.addView(empty) }; return }
        items.forEach { track ->
            val item = row().apply { background = glass(16); setPadding(dp(10), dp(9), dp(11), dp(9)); isClickable = true; setOnClickListener { play(track); showPlayer() } }
            item.addView(text("♫", 21f, cyan, true).apply { gravity = Gravity.CENTER; background = gradient(intArrayOf(Color.rgb(37, 77, 94), Color.rgb(12, 27, 39)), 12); layoutParams = LinearLayout.LayoutParams(dp(45), dp(45)) })
            val names = column().apply { setPadding(dp(11), 0, 0, 0) }; names.addView(text(track.title, 12f, white, true)); names.addView(text(track.artist, 9f, muted).apply { setPadding(0, dp(4), 0, 0) })
            item.addView(names, LinearLayout.LayoutParams(0, -2, 1f)); item.addView(text("•••", 15f, muted))
            val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(7); parent.addView(item, lp)
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
        if (::trackCount.isInitialized) trackCount.text = "${tracks.size} tracks"
        if (::content.isInitialized) showPage(page)
        Toast.makeText(this, "Found ${tracks.size} tracks", Toast.LENGTH_SHORT).show()
    }
    private fun play(track: Track) {
        try {
            player?.release(); current = track
            player = MediaPlayer().apply { setDataSource(this@MainActivity, track.uri); setOnPreparedListener { it.start(); miniPlay.text = "Ⅱ" }; setOnCompletionListener { miniPlay.text = "▶" }; prepareAsync() }
            miniTitle.text = track.title; miniArtist.text = track.artist; miniVisible = true
        } catch (_: Exception) { miniTitle.text = "Unable to play this track" }
    }
    private fun togglePlayback() {
        val p = player ?: run { if (tracks.isNotEmpty()) play(tracks.first()); return }
        if (p.isPlaying) { p.pause(); miniPlay.text = "▶" } else { p.start(); miniPlay.text = "Ⅱ" }
    }
    private fun previousTrack() { val i = tracks.indexOf(current).coerceAtLeast(0); if (tracks.isNotEmpty()) play(tracks[(i - 1 + tracks.size) % tracks.size]) }
    private fun nextTrack() { val i = tracks.indexOf(current).coerceAtLeast(-1); if (tracks.isNotEmpty()) play(tracks[(i + 1) % tracks.size]) }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}
