package com.sonora.music

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar

private fun plural(n: Int, one: String, many: String) = if (n == 1) "$n $one" else "$n $many"

/** Small raised card with an inset icon well, a two-line label and a chevron. Used by Library shortcuts. */
private fun Context2.shortcut(icon: Int, title: String, subtitle: String, onClick: () -> Unit): LinearLayout {
    val pal = Theme.p
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    row.gravity = Gravity.CENTER_VERTICAL
    row.applyNeo(neo(NeoStyle.RAISED, NeoLevel.STANDARD, 24f), dp(12), dp(12), dp(10), dp(12))
    val well = FrameLayout(this)
    well.background = neo(NeoStyle.INSET, NeoLevel.SUBTLE, -1f)
    well.addView(iconView(icon, pal.accentText), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
    row.addView(well, LinearLayout.LayoutParams(dp(42), dp(42)))
    val texts = LinearLayout(this)
    texts.orientation = LinearLayout.VERTICAL
    texts.addView(tv(title, 15f, pal.text, W_MEDIUM).oneLine())
    val s = tv(subtitle, 12.5f, pal.textSecondary).oneLine()
    s.setPadding(0, dp(2), 0, 0)
    texts.addView(s)
    val tl = LinearLayout.LayoutParams(0, WRAP, 1f)
    tl.leftMargin = dp(12)
    row.addView(texts, tl)
    row.addView(iconView(R.drawable.ic_chevron_right, pal.textMuted), LinearLayout.LayoutParams(dp(20), dp(20)))
    row.isClickable = true
    row.isFocusable = true
    row.contentDescription = "$title, $subtitle"
    row.setOnClickListener { onClick() }
    row.pressScale()
    return row
}

private typealias Context2 = android.content.Context

private fun Context2.eyebrowTitle(kicker: String, title: String): LinearLayout {
    val col = screenTitle(kicker, title)
    col.setPadding(0, dp(22), 0, 0)
    return col
}

private fun Context2.emptyState(host: Host): View? {
    if (!host.hasPermission()) {
        return infoCard("Let SONARA find your music", "SONARA plays the audio files stored on this phone. Allow access so it can list them. Nothing leaves your device.", "Allow access") { host.requestPermission() }
    }
    if (Library.scanning && !Library.loadedOnce) return infoCard("Looking for your music…", "This only takes a moment.")
    if (Library.tracks.isEmpty()) {
        return infoCard("No music found", Library.lastError ?: "Copy some audio files to this phone, then scan again.", "Scan device") { host.requestScan() }
    }
    return null
}

private fun LinearLayout.addSection(title: String, topDp: Int, action: String? = null, onAction: (() -> Unit)? = null) {
    val h = context.sectionHeader(title, action, onAction)
    val lp = LinearLayout.LayoutParams(MATCH, WRAP)
    lp.topMargin = context.dp(topDp)
    addView(h, lp)
}

private fun albumCarousel(host: Host, albums: List<Album>, record: (() -> Unit)? = null): HorizontalScrollView {
    val c = host.ctx
    return c.carousel(albums.map { a ->
        c.mediaItem(a.tracks.firstOrNull(), a.name, a.artist, 104f, 18f) {
            record?.invoke()
            host.open(Collections.album(host, a))
        }
    })
}

private fun artistCarousel(host: Host, artists: List<Artist>, record: (() -> Unit)? = null): HorizontalScrollView {
    val c = host.ctx
    return c.carousel(artists.map { a ->
        c.mediaItem(a.tracks.firstOrNull(), a.name, null, 80f, 40f) {
            record?.invoke()
            host.open(Collections.artist(host, a))
        }
    }, 12)
}

// ---------------------------------------------------------------- Home

class HomeScreen(host: Host) : Screen(host) {
    private var greeting: TextView? = null
    private var body: LinearLayout? = null

    private fun greetingText(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (h < 12) "Good morning" else if (h < 17) "Good afternoon" else "Good evening"
    }

    override fun createView(): View {
        val c = host.ctx
        val page = Page(c)
        page.col.addView(c.appHeader { host.open(SettingsScreen(host)) })
        val title = c.eyebrowTitle(greetingText(), "Your sound space")
        greeting = ((title.getChildAt(0)) as? TextView)
        page.col.addView(title)
        val d = LinearLayout(c)
        d.orientation = LinearLayout.VERTICAL
        page.col.addView(d)
        body = d
        render()
        return page.scroll
    }

    override fun onShow() {
        greeting?.text = greetingText().uppercase()
        if (body != null) render()
    }
    override fun onLibraryChanged() { if (body != null) render() }
    override fun onPlaybackChanged() {}

    private fun render() {
        val c = host.ctx
        val d = body ?: return
        d.removeAllViews()
        val empty = c.emptyState(host)
        if (empty != null) { d.addNeo(empty, topGap = c.dp(24)); return }
        val all = Library.tracks
        val played = Store.recentlyPlayed()
        val featured = played.firstOrNull() ?: Library.recent.firstOrNull() ?: all.first()
        val kicker = if (played.isNotEmpty()) "JUMP BACK IN" else "NEWEST ON YOUR PHONE"
        d.addNeo(featuredCard(featured, kicker, all), topGap = c.dp(24))

        val recent = Library.recent.filter { it.id != featured.id }.take(3)
        if (recent.isNotEmpty()) {
            d.addSection("Recently added", 28)
            d.addView(recentGrid(recent), LinearLayout.LayoutParams(MATCH, WRAP).also { it.topMargin = c.dp(14) })
        }
        val rotation = played.filter { it.id != featured.id }.take(4)
        if (rotation.isNotEmpty()) {
            d.addSection("Your rotation", 24)
            val grid = LinearLayout(c)
            grid.orientation = LinearLayout.VERTICAL
            grid.clipChildren = false
            var i = 0
            while (i < rotation.size) {
                val line = LinearLayout(c)
                line.orientation = LinearLayout.HORIZONTAL
                line.clipChildren = false
                for (k in 0 until 2) {
                    val t = rotation.getOrNull(i + k)
                    if (t != null) {
                        val chip = compactTrack(t, all)
                        val m = chip.neoMargin()
                        val lp = LinearLayout.LayoutParams(0, WRAP, 1f)
                        lp.leftMargin = if (k == 0) -m else c.dp(5) - m
                        lp.rightMargin = if (k == 1) -m else c.dp(5) - m
                        lp.topMargin = -m
                        lp.bottomMargin = -m
                        line.addView(chip, lp)
                    } else {
                        line.addView(View(c), LinearLayout.LayoutParams(0, 1, 1f))
                    }
                }
                val ll = LinearLayout.LayoutParams(MATCH, WRAP)
                ll.topMargin = c.dp(if (i == 0) 14 else 6)
                grid.addView(line, ll)
                i += 2
            }
            d.addView(grid)
        }
        if (Library.albums.isNotEmpty()) {
            val row = discoverRow()
            d.addView(row, LinearLayout.LayoutParams(MATCH, WRAP).also { it.topMargin = c.dp(22) })
        }
    }

    private fun featuredCard(t: Track, kicker: String, queue: List<Track>): View {
        val c = host.ctx
        val pal = Theme.p
        val card = LinearLayout(c)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.applyNeo(c.neo(NeoStyle.RAISED, NeoLevel.PROMINENT, 28f), c.dp(14), c.dp(14), c.dp(14), c.dp(14))
        val side = c.dp(116)
        val art = ArtworkView(c, 20f)
        card.addView(art, LinearLayout.LayoutParams(side, side))
        Artwork.bind(art, t, side)
        val texts = LinearLayout(c)
        texts.orientation = LinearLayout.VERTICAL
        val k = c.tv(kicker, 11.5f, pal.accentText, W_BOLD)
        k.letterSpacing = 0.14f
        texts.addView(k)
        val title = c.tv(t.title, 19f, pal.text, W_BOLD)
        title.maxLines = 2
        title.ellipsize = android.text.TextUtils.TruncateAt.END
        title.setPadding(0, c.dp(6), 0, 0)
        texts.addView(title)
        val artist = c.tv(t.artist, 14f, pal.textSecondary).oneLine()
        artist.setPadding(0, c.dp(4), 0, 0)
        texts.addView(artist)
        val tl = LinearLayout.LayoutParams(0, WRAP, 1f)
        tl.leftMargin = c.dp(14)
        card.addView(texts, tl)
        val play = c.roundButton(R.drawable.ic_play, "Play ${t.title}", 46f, NeoLevel.SUBTLE, pal.onAccent, pal.accent) { host.playQueue(queue, queue.indexOf(t)) }
        val pl = play.layoutParams as LinearLayout.LayoutParams
        pl.leftMargin = c.dp(4) - play.neoMargin()
        pl.rightMargin = -play.neoMargin() + c.dp(2)
        card.addView(play, pl)
        card.isClickable = true
        card.setOnClickListener { host.playQueue(queue, queue.indexOf(t)) }
        card.pressScale(0.985f)
        return card
    }

    /** One large artwork card beside up to two compact rows; shrinks gracefully when there is less to show. */
    private fun recentGrid(items: List<Track>): View {
        val c = host.ctx
        val pal = Theme.p
        val line = LinearLayout(c)
        line.orientation = LinearLayout.HORIZONTAL
        line.clipChildren = false
        val queue = Library.recent
        val big = items[0]
        val colPx = (c.resources.displayMetrics.widthPixels - c.dp(40) - c.dp(12)) / 2
        val bigCard = LinearLayout(c)
        bigCard.orientation = LinearLayout.VERTICAL
        bigCard.applyNeo(c.neo(NeoStyle.RAISED, NeoLevel.STANDARD, 24f), c.dp(12), c.dp(12), c.dp(12), c.dp(14))
        val bm = bigCard.neoMargin()
        val artSide = (if (items.size == 1) c.resources.displayMetrics.widthPixels - c.dp(40) else colPx) - 2 * c.dp(12)
        val art = ArtworkView(c, 18f)
        bigCard.addView(art, LinearLayout.LayoutParams(artSide.coerceAtMost(c.dp(260)), artSide.coerceAtMost(c.dp(260))))
        Artwork.bind(art, big, artSide)
        bigCard.addView(c.tv(big.title, 15f, pal.text, W_MEDIUM).oneLine(), LinearLayout.LayoutParams(MATCH, WRAP).also { it.topMargin = c.dp(10) })
        bigCard.addView(c.tv(big.artist, 13f, pal.textSecondary).oneLine(), LinearLayout.LayoutParams(MATCH, WRAP).also { it.topMargin = c.dp(2) })
        bigCard.isClickable = true
        bigCard.setOnClickListener { host.playQueue(queue, queue.indexOf(big)) }
        bigCard.pressScale()
        val bl = LinearLayout.LayoutParams(if (items.size == 1) MATCH else 0, WRAP, if (items.size == 1) 0f else 1f)
        bl.leftMargin = -bm
        bl.rightMargin = if (items.size == 1) -bm else c.dp(6) - bm
        bl.topMargin = -bm
        bl.bottomMargin = -bm
        line.addView(bigCard, bl)
        if (items.size > 1) {
            val col = LinearLayout(c)
            col.orientation = LinearLayout.VERTICAL
            col.clipChildren = false
            items.drop(1).forEachIndexed { i, t ->
                val chip = compactTrack(t, queue)
                val m = chip.neoMargin()
                val lp = LinearLayout.LayoutParams(MATCH, 0, 1f)
                lp.leftMargin = -m
                lp.rightMargin = -m
                lp.topMargin = (if (i == 0) 0 else c.dp(4)) - m
                lp.bottomMargin = -m
                col.addView(chip, lp)
            }
            line.addView(col, LinearLayout.LayoutParams(0, MATCH, 1f).also { it.leftMargin = c.dp(6) })
        }
        return line
    }

    private fun compactTrack(t: Track, queue: List<Track>): LinearLayout {
        val c = host.ctx
        val pal = Theme.p
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.applyNeo(c.neo(NeoStyle.RAISED, NeoLevel.SUBTLE, 18f), c.dp(8), c.dp(8), c.dp(10), c.dp(8))
        val side = c.dp(44)
        val art = ArtworkView(c, 12f)
        row.addView(art, LinearLayout.LayoutParams(side, side))
        Artwork.bind(art, t, side)
        val texts = LinearLayout(c)
        texts.orientation = LinearLayout.VERTICAL
        texts.addView(c.tv(t.title, 14f, pal.text, W_MEDIUM).oneLine())
        texts.addView(c.tv(t.artist, 12f, pal.textSecondary).oneLine(), LinearLayout.LayoutParams(MATCH, WRAP).also { it.topMargin = c.dp(2) })
        row.addView(texts, LinearLayout.LayoutParams(0, WRAP, 1f).also { it.leftMargin = c.dp(10) })
        row.isClickable = true
        row.isFocusable = true
        row.contentDescription = "${t.title}, ${t.artist}"
        row.setOnClickListener { host.playQueue(queue, queue.indexOf(t).coerceAtLeast(0)) }
        row.pressScale()
        return row
    }

    private fun discoverRow(): View {
        val c = host.ctx
        val pal = Theme.p
        val first = Library.albums.first().tracks.firstOrNull()
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, c.dp(6), 0, c.dp(6))
        val side = c.dp(52)
        val art = ArtworkView(c, 14f)
        row.addView(art, LinearLayout.LayoutParams(side, side))
        Artwork.bind(art, first, side)
        val texts = LinearLayout(c)
        texts.orientation = LinearLayout.VERTICAL
        texts.addView(c.tv("Browse albums", 16f, pal.text, W_MEDIUM))
        texts.addView(c.tv(plural(Library.albums.size, "album", "albums") + " on this device", 13f, pal.textSecondary), LinearLayout.LayoutParams(WRAP, WRAP).also { it.topMargin = c.dp(2) })
        row.addView(texts, LinearLayout.LayoutParams(0, WRAP, 1f).also { it.leftMargin = c.dp(14) })
        row.addView(c.iconView(R.drawable.ic_chevron_right, pal.textMuted), LinearLayout.LayoutParams(c.dp(24), c.dp(24)))
        row.isClickable = true
        row.isFocusable = true
        row.contentDescription = "Browse albums"
        row.setOnClickListener { host.open(Collections.albums(host)) }
        row.pressScale(0.985f)
        return row
    }
}

// ---------------------------------------------------------------- Search

class SearchScreen(host: Host) : Screen(host) {
    private enum class Scope(val label: String) { ALL("All"), SONGS("Songs"), ALBUMS("Albums"), ARTISTS("Artists") }

    private var input: EditText? = null
    private var clear: ImageView? = null
    private var chips: LinearLayout? = null
    private var body: LinearLayout? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var scope = Scope.ALL

    private fun query(): String = input?.text?.toString()?.trim().orEmpty()

    override fun createView(): View {
        val c = host.ctx
        val pal = Theme.p
        val page = Page(c)
        page.col.addView(c.appHeader { host.open(SettingsScreen(host)) })
        page.col.addView(c.eyebrowTitle("Find your sound", "Search"))

        val box = LinearLayout(c)
        box.orientation = LinearLayout.HORIZONTAL
        box.gravity = Gravity.CENTER_VERTICAL
        box.background = c.neo(NeoStyle.INSET, NeoLevel.STANDARD, -1f)
        box.setPadding(c.dp(18), 0, c.dp(6), 0)
        box.addView(c.iconView(R.drawable.ic_search, pal.textMuted), LinearLayout.LayoutParams(c.dp(22), c.dp(22)))
        val field = EditText(c)
        field.background = null
        field.hint = "Songs, artists, albums"
        field.setHintTextColor(pal.textMuted)
        field.setTextColor(pal.text)
        field.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
        field.setSingleLine(true)
        field.inputType = InputType.TYPE_CLASS_TEXT
        field.imeOptions = EditorInfo.IME_ACTION_SEARCH
        field.setPadding(c.dp(12), 0, c.dp(8), 0)
        field.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Store.recordSearch(query())
                c.hideKeyboard(v)
                render()
                true
            } else false
        }
        box.addView(field, LinearLayout.LayoutParams(0, MATCH, 1f))
        val x = c.iconView(R.drawable.ic_close, pal.textSecondary)
        x.setPadding(c.dp(10), c.dp(10), c.dp(10), c.dp(10))
        x.contentDescription = "Clear search"
        x.visibility = View.GONE
        x.setOnClickListener { field.setText("") }
        box.addView(x, LinearLayout.LayoutParams(c.dp(44), c.dp(44)))
        val boxLp = LinearLayout.LayoutParams(MATCH, c.dp(56))
        boxLp.topMargin = c.dp(20)
        page.col.addView(box, boxLp)
        input = field
        clear = x

        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        chips = row
        val sc = HorizontalScrollView(c)
        sc.isHorizontalScrollBarEnabled = false
        sc.overScrollMode = View.OVER_SCROLL_NEVER
        sc.clipToPadding = false
        sc.setPadding(c.dp(20), c.dp(10), c.dp(20), c.dp(14))
        sc.addView(row)
        val scl = LinearLayout.LayoutParams(MATCH, WRAP)
        scl.leftMargin = -c.dp(20)
        scl.rightMargin = -c.dp(20)
        scl.topMargin = c.dp(6)
        page.col.addView(sc, scl)

        val b = LinearLayout(c)
        b.orientation = LinearLayout.VERTICAL
        body = b
        page.col.addView(b)

        field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                x.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                pending?.let { handler.removeCallbacks(it) }
                val run = Runnable { render() }
                pending = run
                handler.postDelayed(run, 140)
            }
        })
        renderChips()
        render()
        return page.scroll
    }

    override fun onLibraryChanged() { if (body != null) render() }
    override fun onPlaybackChanged() { if (query().isNotEmpty() && body != null) render() }

    private fun renderChips() {
        val c = host.ctx
        val row = chips ?: return
        row.removeAllViews()
        row.clipChildren = false
        Scope.values().forEachIndexed { i, s ->
            val chip = c.chip(s.label, s == scope) { scope = s; renderChips(); render() }
            val m = chip.neoMargin()
            val lp = LinearLayout.LayoutParams(WRAP, WRAP)
            lp.leftMargin = (if (i == 0) 0 else c.dp(8)) - m
            lp.rightMargin = -m
            lp.topMargin = -m
            lp.bottomMargin = -m
            row.addView(chip, lp)
        }
    }

    private fun render() {
        val c = host.ctx
        val d = body ?: return
        d.removeAllViews()
        val empty = c.emptyState(host)
        if (empty != null) { d.addNeo(empty, topGap = c.dp(8)); return }
        val q = query().lowercase()
        if (q.isEmpty()) renderBrowse(d) else renderResults(d, q)
    }

    private fun renderBrowse(d: LinearLayout) {
        val c = host.ctx
        val artists = Library.artists.sortedByDescending { it.tracks.size }.take(12)
        val albums = Library.albums.take(12)
        if ((scope == Scope.ALL || scope == Scope.ARTISTS) && artists.isNotEmpty()) {
            d.addSection("Artists", 8, "See all") { host.open(Collections.artists(host)) }
            d.addCarousel(artistCarousel(host, artists), c.dp(4))
        }
        if ((scope == Scope.ALL || scope == Scope.ALBUMS) && albums.isNotEmpty()) {
            d.addSection("Albums", 8, "See all") { host.open(Collections.albums(host)) }
            d.addCarousel(albumCarousel(host, albums), c.dp(4))
        }
        if (scope == Scope.SONGS) {
            d.addSection("Songs", 8, "See all") { host.open(Collections.songs(host)) }
            val box = LinearLayout(c)
            box.orientation = LinearLayout.VERTICAL
            addTrackRows(host, box, Library.tracks.take(20), Library.tracks)
            d.addView(box)
        }
        if (scope == Scope.ALL) {
            val recents = Store.recentSearches()
            if (recents.isNotEmpty()) {
                d.addSection("Recent searches", 14, "Clear all") { Store.clearSearches(); render() }
                for (term in recents) d.addView(recentRow(term), LinearLayout.LayoutParams(MATCH, WRAP).also { it.topMargin = c.dp(8) })
            }
        }
    }

    private fun recentRow(term: String): View {
        val c = host.ctx
        val pal = Theme.p
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(c.dp(4), c.dp(2), c.dp(0), c.dp(2))
        row.addView(c.iconView(R.drawable.ic_history, pal.textMuted), LinearLayout.LayoutParams(c.dp(20), c.dp(20)))
        val t = c.tv(term, 16f, pal.text).oneLine()
        val tl = LinearLayout.LayoutParams(0, WRAP, 1f)
        tl.leftMargin = c.dp(14)
        row.addView(t, tl)
        val x = c.iconView(R.drawable.ic_close, pal.textSecondary)
        x.setPadding(c.dp(12), c.dp(12), c.dp(12), c.dp(12))
        x.contentDescription = "Remove $term"
        x.setOnClickListener { Store.removeSearch(term); render() }
        row.addView(x, LinearLayout.LayoutParams(c.dp(44), c.dp(44)))
        row.minimumHeight = c.dp(48)
        row.isClickable = true
        row.isFocusable = true
        row.contentDescription = "Search for $term"
        row.setOnClickListener { input?.setText(term); input?.setSelection(term.length) }
        return row
    }

    private fun renderResults(d: LinearLayout, q: String) {
        val c = host.ctx
        val songs = if (scope == Scope.ALL || scope == Scope.SONGS) Library.tracks.filter { it.searchKey.contains(q) } else emptyList()
        val albums = if (scope == Scope.ALL || scope == Scope.ALBUMS) Library.albums.filter { "${it.name} ${it.artist}".lowercase().contains(q) } else emptyList()
        val artists = if (scope == Scope.ALL || scope == Scope.ARTISTS) Library.artists.filter { it.name.lowercase().contains(q) } else emptyList()
        if (songs.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
            d.addNeo(c.infoCard("No matches", "Nothing on this device matches “${query()}”."), topGap = c.dp(8))
            return
        }
        if (artists.isNotEmpty()) {
            d.addSection("Artists", 8)
            d.addCarousel(artistCarousel(host, artists.take(12)) { Store.recordSearch(query()) }, c.dp(4))
        }
        if (albums.isNotEmpty()) {
            d.addSection("Albums", 8)
            d.addCarousel(albumCarousel(host, albums.take(12)) { Store.recordSearch(query()) }, c.dp(4))
        }
        if (songs.isNotEmpty()) {
            val shown = songs.take(40)
            d.addSection(if (songs.size > shown.size) "Songs · first 40 of ${songs.size}" else "Songs · ${songs.size}", 12)
            val box = LinearLayout(c)
            box.orientation = LinearLayout.VERTICAL
            addTrackRows(host, box, shown, songs)
            d.addView(box, LinearLayout.LayoutParams(MATCH, WRAP).also { it.topMargin = c.dp(4) })
        }
    }
}

// ---------------------------------------------------------------- Library

class LibraryScreen(host: Host) : Screen(host) {
    private var body: LinearLayout? = null

    override fun createView(): View {
        val c = host.ctx
        val page = Page(c)
        page.col.addView(c.appHeader { host.open(SettingsScreen(host)) })
        page.col.addView(c.eyebrowTitle("Your collection", "Library"))
        val d = LinearLayout(c)
        d.orientation = LinearLayout.VERTICAL
        page.col.addView(d)
        body = d
        render()
        return page.scroll
    }

    override fun onLibraryChanged() { if (body != null) render() }

    private fun render() {
        val c = host.ctx
        val pal = Theme.p
        val d = body ?: return
        d.removeAllViews()
        val empty = c.emptyState(host)
        if (empty != null) { d.addNeo(empty, topGap = c.dp(24)); return }

        // Liked songs (tall) beside Playlists and Albums
        val top = LinearLayout(c)
        top.orientation = LinearLayout.HORIZONTAL
        top.clipChildren = false
        val liked = LinearLayout(c)
        liked.orientation = LinearLayout.VERTICAL
        liked.applyNeo(c.neo(NeoStyle.RAISED, NeoLevel.STANDARD, 26f), c.dp(16), c.dp(16), c.dp(12), c.dp(16))
        val well = FrameLayout(c)
        well.background = c.neo(NeoStyle.INSET, NeoLevel.SUBTLE, -1f)
        well.addView(c.iconView(R.drawable.ic_heart, pal.accentText), FrameLayout.LayoutParams(c.dp(24), c.dp(24), Gravity.CENTER))
        liked.addView(well, LinearLayout.LayoutParams(c.dp(48), c.dp(48)))
        liked.addView(c.tv("Liked songs", 17f, pal.text, W_BOLD).oneLine(), LinearLayout.LayoutParams(MATCH, WRAP).also { it.topMargin = c.dp(16) })
        liked.addView(c.tv(plural(Store.likedCount(), "song", "songs"), 13f, pal.textSecondary).oneLine(), LinearLayout.LayoutParams(MATCH, WRAP).also { it.topMargin = c.dp(3) })
        liked.isClickable = true
        liked.isFocusable = true
        liked.contentDescription = "Liked songs, ${plural(Store.likedCount(), "song", "songs")}"
        liked.setOnClickListener { host.open(Collections.liked(host)) }
        liked.pressScale()
        val lm = liked.neoMargin()
        top.addView(liked, LinearLayout.LayoutParams(0, MATCH, 1f).also { it.leftMargin = -lm; it.topMargin = -lm; it.bottomMargin = -lm; it.rightMargin = c.dp(6) - lm })

        val right = LinearLayout(c)
        right.orientation = LinearLayout.VERTICAL
        right.clipChildren = false
        val pl = c.shortcut(R.drawable.ic_playlist, "Playlists", plural(Store.playlists.size, "playlist", "playlists")) { host.open(Collections.playlists(host)) }
        val al = c.shortcut(R.drawable.ic_album, "Albums", plural(Library.albums.size, "album", "albums")) { host.open(Collections.albums(host)) }
        right.addAligned(pl, MATCH, 0, 0, 0, 0, 0)
        (pl.layoutParams as LinearLayout.LayoutParams).weight = 1f
        right.addAligned(al, MATCH, 0, 0, c.dp(8), 0, 0)
        (al.layoutParams as LinearLayout.LayoutParams).weight = 1f
        top.addView(right, LinearLayout.LayoutParams(0, MATCH, 1.12f).also { it.leftMargin = c.dp(6) })
        d.addView(top, LinearLayout.LayoutParams(MATCH, WRAP).also { it.topMargin = c.dp(24) })

        val second = LinearLayout(c)
        second.orientation = LinearLayout.HORIZONTAL
        second.clipChildren = false
        val ar = c.shortcut(R.drawable.ic_person, "Artists", plural(Library.artists.size, "artist", "artists")) { host.open(Collections.artists(host)) }
        val dev = c.shortcut(R.drawable.ic_phone, "On this device", plural(Library.tracks.size, "song", "songs")) { host.open(Collections.songs(host)) }
        second.addAligned(ar, 0, WRAP, 0, 0, c.dp(6), 0)
        (ar.layoutParams as LinearLayout.LayoutParams).weight = 1f
        second.addAligned(dev, 0, WRAP, c.dp(6), 0, 0, 0)
        (dev.layoutParams as LinearLayout.LayoutParams).weight = 1f
        d.addView(second, LinearLayout.LayoutParams(MATCH, WRAP).also { it.topMargin = c.dp(12) })

        val recent = Library.recent
        if (recent.isNotEmpty()) {
            d.addSection("Recently added", 26)
            d.addCarousel(c.carousel(recent.take(10).map { t -> c.mediaItem(t, t.title, t.artist, 104f, 18f) { host.playQueue(recent, recent.indexOf(t)) } }), c.dp(4))
        }
        val artists = Library.artists.sortedByDescending { it.tracks.size }.take(12)
        if (artists.isNotEmpty()) {
            d.addSection("Artists", 8, "See all") { host.open(Collections.artists(host)) }
            d.addCarousel(artistCarousel(host, artists), c.dp(4))
        }
    }
}
