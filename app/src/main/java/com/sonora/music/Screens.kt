package com.sonora.music

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/** What a screen may ask of the app shell. MainActivity implements it. */
interface Host {
    val ctx: Context
    fun open(screen: Screen)
    fun back()
    fun selectTab(index: Int)
    fun openPlayer()
    fun closePlayer()
    fun playQueue(tracks: List<Track>, index: Int)
    fun shuffleAll()
    fun hasPermission(): Boolean
    fun requestPermission()
    fun requestScan()
    fun toast(message: String)
    fun setDark(dark: Boolean)
    fun openEqualizer()
    fun showSleepTimer()
    fun toggleLike(track: Track)
    fun trackMenu(anchor: View, track: Track, playlistName: String?)
    fun newPlaylist()
    fun confirmDeletePlaylist(name: String)
}

abstract class Screen(val host: Host) {
    private var built: View? = null
    val view: View
        get() {
            var v = built
            if (v == null) {
                v = createView()
                built = v
            }
            return v
        }

    protected abstract fun createView(): View
    open fun onLibraryChanged() {}
    open fun onPlaybackChanged() {}
    open fun onShow() {}
}

class Page(ctx: Context) {
    val scroll = ScrollView(ctx)
    val col = LinearLayout(ctx)

    init {
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(ctx.dp(20), ctx.dp(12), ctx.dp(20), ctx.dp(24))
        scroll.addView(col, ViewGroup.LayoutParams(MATCH, WRAP))
        scroll.isVerticalScrollBarEnabled = false
        scroll.overScrollMode = View.OVER_SCROLL_NEVER
        scroll.clipToPadding = false
    }
}

class Row(
    val track: Track? = null,
    val title: String = "",
    val subtitle: String = "",
    val iconRes: Int = 0,
    val onClick: (() -> Unit)? = null,
    val onLongClick: (() -> Unit)? = null
)

// ---------------------------------------------------------------- shared builders

private fun Context.appHeader(onSettings: () -> Unit): LinearLayout {
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    row.gravity = Gravity.CENTER_VERTICAL
    row.addView(logoMark(34f))
    val word = tv("SONARA", 18f, Theme.p.text, W_BOLD)
    word.letterSpacing = 0.2f
    word.setPadding(dp(10), 0, 0, 0)
    row.addView(word, LinearLayout.LayoutParams(0, WRAP, 1f))
    val settings = roundButton(R.drawable.ic_settings, "Settings", 44f, NeoLevel.SUBTLE, Theme.p.textSecondary) { onSettings() }
    val lp = settings.layoutParams as LinearLayout.LayoutParams
    lp.rightMargin = -settings.neoMargin()
    row.addView(settings, lp)
    return row
}

private fun Context.backHeader(host: Host, title: String, subtitle: String): LinearLayout {
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    val back = roundButton(R.drawable.ic_back, "Back", 44f, NeoLevel.SUBTLE, Theme.p.text) { host.back() }
    val lp = back.layoutParams as LinearLayout.LayoutParams
    lp.leftMargin = -back.neoMargin()
    col.addView(back, lp)
    val t = tv(title, 30f, Theme.p.text, W_BOLD)
    t.setPadding(0, dp(8), 0, 0)
    t.oneLine()
    col.addView(t)
    val s = tv(subtitle, 14f, Theme.p.textSecondary)
    s.setPadding(0, dp(6), 0, 0)
    col.addView(s)
    return col
}

fun addTrackRows(host: Host, parent: LinearLayout, tracks: List<Track>, queue: List<Track>, playlist: String? = null) {
    val c = host.ctx
    val currentId = PlaybackHub.current?.id
    for (t in tracks) {
        val rowView = TrackRowView(c)
        rowView.bind(
            t,
            currentId == t.id,
            { host.playQueue(queue, queue.indexOf(t)) },
            { anchor -> host.trackMenu(anchor, t, playlist) }
        )
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.leftMargin = -c.dp(8)
        lp.rightMargin = -c.dp(8)
        parent.addView(rowView, lp)
    }
}

// ---------------------------------------------------------------- collections

object Collections {
    fun songs(host: Host) = CollectionScreen(host, "Songs", "Everything on this device", canShuffle = true) {
        Library.tracks.map { Row(track = it) }
    }

    fun liked(host: Host) = CollectionScreen(host, "Liked songs", "Tap the heart on any song to save it here", canShuffle = true) {
        Store.likedTracks().map { Row(track = it) }
    }

    fun albums(host: Host) = CollectionScreen(host, "Albums", "${Library.albums.size} albums") {
        Library.albums.map { a ->
            Row(
                title = a.name,
                subtitle = "${a.artist} · ${a.tracks.size} songs",
                iconRes = R.drawable.ic_album,
                onClick = { host.open(album(host, a)) }
            )
        }
    }

    fun album(host: Host, a: Album) = CollectionScreen(host, a.name, a.artist, canShuffle = true) {
        a.tracks.map { Row(track = it) }
    }

    fun artists(host: Host) = CollectionScreen(host, "Artists", "${Library.artists.size} artists") {
        Library.artists.map { a ->
            Row(
                title = a.name,
                subtitle = "${a.tracks.size} songs",
                iconRes = R.drawable.ic_person,
                onClick = { host.open(artist(host, a)) }
            )
        }
    }

    fun artist(host: Host, a: Artist) = CollectionScreen(host, a.name, "${a.tracks.size} songs", canShuffle = true) {
        a.tracks.map { Row(track = it) }
    }

    fun playlists(host: Host) = CollectionScreen(host, "Playlists", "Your own mixes, stored on this device", canCreatePlaylist = true) {
        Store.playlists.keys.toList().map { name ->
            Row(
                title = name,
                subtitle = "${Store.playlists[name]?.size ?: 0} songs · hold to delete",
                iconRes = R.drawable.ic_playlist,
                onClick = { host.open(playlist(host, name)) },
                onLongClick = { host.confirmDeletePlaylist(name) }
            )
        }
    }

    fun playlist(host: Host, name: String) = CollectionScreen(host, name, "Playlist", canShuffle = true, playlistName = name) {
        Store.playlistTracks(name).map { Row(track = it) }
    }
}

class GroupRowView(ctx: Context) : LinearLayout(ctx) {
    private val icon = context.iconView(R.drawable.ic_album, Theme.p.accentText)
    private val title = context.tv("", 16f, Theme.p.text, W_MEDIUM).oneLine()
    private val subtitle = context.tv("", 13f, Theme.p.textSecondary).oneLine()
    private val idleBg = StateListDrawable()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(context.dp(8), context.dp(8), context.dp(8), context.dp(8))
        minimumHeight = context.dp(64)
        isClickable = true
        isFocusable = true
        idleBg.addState(intArrayOf(android.R.attr.state_pressed), context.neo(NeoStyle.INSET, NeoLevel.SUBTLE, 16f))
        idleBg.addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
        background = idleBg
        val wrap = FrameLayout(ctx)
        wrap.background = context.neo(NeoStyle.INSET, NeoLevel.SUBTLE, -1f)
        wrap.addView(icon, FrameLayout.LayoutParams(context.dp(22), context.dp(22), Gravity.CENTER))
        addView(wrap, LayoutParams(context.dp(48), context.dp(48)))
        val texts = LinearLayout(ctx)
        texts.orientation = VERTICAL
        texts.addView(title)
        subtitle.setPadding(0, context.dp(3), 0, 0)
        texts.addView(subtitle)
        val tlp = LayoutParams(0, WRAP, 1f)
        tlp.leftMargin = context.dp(14)
        addView(texts, tlp)
        val chevron = context.iconView(R.drawable.ic_chevron_right, Theme.p.textMuted)
        addView(chevron, LayoutParams(context.dp(24), context.dp(24)))
    }

    fun bind(row: Row) {
        icon.setImageResource(row.iconRes)
        title.text = row.title
        subtitle.text = row.subtitle
        setOnClickListener { row.onClick?.invoke() }
        val long = row.onLongClick
        if (long != null) setOnLongClickListener { long(); true } else setOnLongClickListener(null)
        isLongClickable = long != null
    }
}

class CollectionScreen(
    host: Host,
    private val title: String,
    private val subtitle: String,
    private val canShuffle: Boolean = false,
    private val canCreatePlaylist: Boolean = false,
    private val playlistName: String? = null,
    private val provider: () -> List<Row>
) : Screen(host) {

    private var rows: List<Row> = emptyList()
    private var footer: FrameLayout? = null

    private val adapter = object : BaseAdapter() {
        override fun getCount(): Int = rows.size
        override fun getItem(position: Int): Any = rows[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getViewTypeCount(): Int = 2
        override fun getItemViewType(position: Int): Int = if (rows[position].track != null) 0 else 1

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = rows[position]
            val track = row.track
            if (track != null) {
                val v = (convertView as? TrackRowView) ?: TrackRowView(host.ctx)
                v.bind(
                    track,
                    PlaybackHub.current?.id == track.id,
                    {
                        val queue = rows.mapNotNull { it.track }
                        host.playQueue(queue, queue.indexOf(track))
                    },
                    { anchor -> host.trackMenu(anchor, track, playlistName) }
                )
                return v
            }
            val g = (convertView as? GroupRowView) ?: GroupRowView(host.ctx)
            g.bind(row)
            return g
        }
    }

    override fun createView(): View {
        val c = host.ctx
        val list = ListView(c)
        list.divider = null
        list.dividerHeight = 0
        list.selector = ColorDrawable(Color.TRANSPARENT)
        list.isVerticalScrollBarEnabled = false
        list.overScrollMode = View.OVER_SCROLL_NEVER
        list.setPadding(c.dp(12), c.dp(12), c.dp(12), c.dp(16))
        list.clipToPadding = false

        val header = LinearLayout(c)
        header.orientation = LinearLayout.VERTICAL
        header.setPadding(c.dp(8), 0, c.dp(8), c.dp(12))
        header.addView(c.backHeader(host, title, subtitle))
        if (canShuffle) {
            val b = c.primaryButton("Shuffle play", R.drawable.ic_shuffle) {
                val queue = rows.mapNotNull { it.track }
                if (queue.isNotEmpty()) {
                    PlaybackHub.applyShuffle(true)
                    host.playQueue(queue, (0 until queue.size).random())
                }
            }
            header.addNeo(b, topGap = c.dp(16))
        }
        if (canCreatePlaylist) {
            val b = c.primaryButton("New playlist", R.drawable.ic_add) { host.newPlaylist() }
            header.addNeo(b, topGap = c.dp(16))
        }
        list.addHeaderView(header, null, false)

        val f = FrameLayout(c)
        f.setPadding(c.dp(8), c.dp(8), c.dp(8), 0)
        f.addView(c.infoCard("Nothing here yet", emptyMessage()), FrameLayout.LayoutParams(MATCH, WRAP))
        list.addFooterView(f, null, false)
        footer = f

        list.adapter = adapter
        refresh()
        return list
    }

    private fun emptyMessage(): String = when {
        playlistName != null -> "Add songs from any song's ••• menu."
        canCreatePlaylist -> "Create a playlist, then add songs from any song's ••• menu."
        title == "Liked songs" -> "Tap the heart in the player to like a song."
        else -> "Scan your device from the Library tab to bring music in."
    }

    private fun refresh() {
        rows = provider()
        footer?.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        adapter.notifyDataSetChanged()
    }

    override fun onLibraryChanged() { if (footer != null) refresh() }
    override fun onPlaybackChanged() { adapter.notifyDataSetChanged() }
}

// ---------------------------------------------------------------- Home

class HomeScreen(host: Host) : Screen(host) {
    private var greeting: TextView? = null
    private var body: LinearLayout? = null
    private var songsBox: LinearLayout? = null

    private fun greetingText(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (h < 12) "Good morning" else if (h < 17) "Good afternoon" else "Good evening"
    }

    override fun createView(): View {
        val c = host.ctx
        val page = Page(c)
        page.col.addView(c.appHeader { host.open(SettingsScreen(host)) })
        val g = c.tv(greetingText(), 15f, Theme.p.textSecondary)
        g.setPadding(0, c.dp(22), 0, 0)
        page.col.addView(g)
        greeting = g
        val h = c.tv("Your sound space", 30f, Theme.p.text, W_BOLD)
        h.setPadding(0, c.dp(4), 0, 0)
        page.col.addView(h)
        val d = LinearLayout(c)
        d.orientation = LinearLayout.VERTICAL
        page.col.addView(d)
        body = d
        render()
        return page.scroll
    }

    override fun onShow() { greeting?.text = greetingText() }
    override fun onLibraryChanged() { if (body != null) render() }
    override fun onPlaybackChanged() { renderSongs() }

    private fun render() {
        val c = host.ctx
        val d = body ?: return
        d.removeAllViews()
        val tracks = Library.tracks
        if (!host.hasPermission()) {
            d.addNeo(c.infoCard("Let SONARA find your music", "SONARA plays the audio files stored on this phone. Allow access so it can list them. Nothing leaves your device.", "Allow access") { host.requestPermission() }, topGap = c.dp(24))
            return
        }
        if (Library.scanning && !Library.loadedOnce) {
            d.addNeo(c.infoCard("Looking for your music…", "This only takes a moment."), topGap = c.dp(24))
            return
        }
        if (tracks.isEmpty()) {
            d.addNeo(c.infoCard("No music found", Library.lastError ?: "Copy some audio files to this phone, then scan again.", "Scan device") { host.requestScan() }, topGap = c.dp(24))
            return
        }
        d.addNeo(hero(tracks.size), topGap = c.dp(24))

        val stats = LinearLayout(c)
        stats.orientation = LinearLayout.HORIZONTAL
        val entries = listOf(
            Triple(tracks.size, "Songs", { host.open(Collections.songs(host)) }),
            Triple(Library.albums.size, "Albums", { host.open(Collections.albums(host)) }),
            Triple(Library.artists.size, "Artists", { host.open(Collections.artists(host)) })
        )
        entries.forEachIndexed { i, e ->
            val tile = stat(e.first, e.second, e.third)
            val m = tile.neoMargin()
            val lp = LinearLayout.LayoutParams(0, WRAP, 1f)
            lp.leftMargin = if (i == 0) -m else c.dp(6) - m
            lp.rightMargin = if (i == 2) -m else c.dp(6) - m
            lp.topMargin = -m
            lp.bottomMargin = -m
            stats.addView(tile, lp)
        }
        val statsLp = LinearLayout.LayoutParams(MATCH, WRAP)
        statsLp.topMargin = c.dp(16)
        d.addView(stats, statsLp)

        val recent = Library.recent
        if (recent.isNotEmpty()) {
            val head = c.sectionHeader("Recently added")
            val hl = LinearLayout.LayoutParams(MATCH, WRAP)
            hl.topMargin = c.dp(28)
            d.addView(head, hl)
            val strip = LinearLayout(c)
            strip.orientation = LinearLayout.HORIZONTAL
            recent.forEachIndexed { i, t ->
                val tile = albumTile(t) { host.playQueue(recent, i) }
                val m = tile.neoMargin()
                val lp = LinearLayout.LayoutParams(WRAP, WRAP)
                lp.leftMargin = if (i == 0) 0 else -c.dp(4)
                lp.topMargin = -m + c.dp(8)
                lp.bottomMargin = -m
                strip.addView(tile, lp)
            }
            val scroller = HorizontalScrollView(c)
            scroller.isHorizontalScrollBarEnabled = false
            scroller.overScrollMode = View.OVER_SCROLL_NEVER
            scroller.clipToPadding = false
            scroller.setPadding(c.dp(8), 0, c.dp(20), 0)
            scroller.addView(strip)
            val sl = LinearLayout.LayoutParams(MATCH, WRAP)
            sl.leftMargin = -c.dp(20)
            sl.rightMargin = -c.dp(20)
            d.addView(scroller, sl)
        }

        val head = c.sectionHeader("Your songs", "See all") { host.open(Collections.songs(host)) }
        val hl = LinearLayout.LayoutParams(MATCH, WRAP)
        hl.topMargin = c.dp(24)
        d.addView(head, hl)
        val box = LinearLayout(c)
        box.orientation = LinearLayout.VERTICAL
        val bl = LinearLayout.LayoutParams(MATCH, WRAP)
        bl.topMargin = c.dp(4)
        d.addView(box, bl)
        songsBox = box
        renderSongs()
    }

    private fun renderSongs() {
        val box = songsBox ?: return
        box.removeAllViews()
        val first = Library.tracks.take(5)
        addTrackRows(host, box, first, Library.tracks)
    }

    private fun hero(count: Int): View {
        val c = host.ctx
        val pal = Theme.p
        val card = LinearLayout(c)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.applyNeo(c.neo(NeoStyle.RAISED, NeoLevel.PROMINENT, 28f), c.dp(22), c.dp(20), c.dp(14), c.dp(20))
        val texts = LinearLayout(c)
        texts.orientation = LinearLayout.VERTICAL
        val k = c.tv("SHUFFLE PLAY", 12f, pal.accentText, W_BOLD)
        k.letterSpacing = 0.14f
        texts.addView(k)
        val t = c.tv("Your whole library", 22f, pal.text, W_BOLD)
        t.setPadding(0, c.dp(8), 0, 0)
        texts.addView(t)
        val s = c.tv("$count songs on this device", 14f, pal.textSecondary)
        s.setPadding(0, c.dp(6), 0, 0)
        texts.addView(s)
        card.addView(texts, LinearLayout.LayoutParams(0, WRAP, 1f))
        card.addView(c.roundButton(R.drawable.ic_play, "Shuffle play", 60f, NeoLevel.SUBTLE, pal.onAccent, pal.accent) { host.shuffleAll() })
        card.isClickable = true
        card.setOnClickListener { host.shuffleAll() }
        return card
    }

    private fun stat(value: Int, label: String, onClick: () -> Unit): LinearLayout {
        val c = host.ctx
        val pal = Theme.p
        val tile = LinearLayout(c)
        tile.orientation = LinearLayout.VERTICAL
        tile.gravity = Gravity.CENTER
        tile.applyNeo(c.neo(NeoStyle.RAISED, NeoLevel.SUBTLE, 20f), c.dp(8), c.dp(16), c.dp(8), c.dp(16))
        tile.addView(c.tv(value.toString(), 24f, pal.accentText, W_BOLD))
        val l = c.tv(label, 13f, pal.textSecondary)
        l.setPadding(0, c.dp(4), 0, 0)
        tile.addView(l)
        tile.isClickable = true
        tile.contentDescription = "$value $label"
        tile.setOnClickListener { onClick() }
        tile.pressScale()
        return tile
    }

    private fun albumTile(t: Track, onClick: () -> Unit): LinearLayout {
        val c = host.ctx
        val pal = Theme.p
        val side = c.dp(126)
        val card = LinearLayout(c)
        card.orientation = LinearLayout.VERTICAL
        card.applyNeo(c.neo(NeoStyle.RAISED, NeoLevel.STANDARD, 22f), c.dp(12), c.dp(12), c.dp(12), c.dp(12))
        val art = ArtworkView(c, 14f)
        card.addView(art, LinearLayout.LayoutParams(side, side))
        Artwork.bind(art, t, side)
        val title = c.tv(t.title, 14f, pal.text, W_MEDIUM).oneLine()
        val tl = LinearLayout.LayoutParams(side, WRAP)
        tl.topMargin = c.dp(10)
        card.addView(title, tl)
        val artist = c.tv(t.artist, 12f, pal.textSecondary).oneLine()
        val al = LinearLayout.LayoutParams(side, WRAP)
        al.topMargin = c.dp(3)
        card.addView(artist, al)
        card.isClickable = true
        card.setOnClickListener { onClick() }
        card.pressScale()
        return card
    }
}

// ---------------------------------------------------------------- Search

class SearchScreen(host: Host) : Screen(host) {
    private var input: EditText? = null
    private var clear: ImageView? = null
    private var results: LinearLayout? = null
    private var browse: LinearLayout? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    override fun createView(): View {
        val c = host.ctx
        val pal = Theme.p
        val page = Page(c)
        page.col.addView(c.screenTitle("Find your sound", "Search"))

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
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { c.hideKeyboard(v); true } else false
        }
        box.addView(field, LinearLayout.LayoutParams(0, MATCH, 1f))
        val x = c.iconView(R.drawable.ic_close, pal.textSecondary)
        x.setPadding(c.dp(10), c.dp(10), c.dp(10), c.dp(10))
        x.contentDescription = "Clear search"
        x.visibility = View.GONE
        x.setOnClickListener { field.setText("") }
        box.addView(x, LinearLayout.LayoutParams(c.dp(44), c.dp(44)))
        val boxLp = LinearLayout.LayoutParams(MATCH, c.dp(56))
        boxLp.topMargin = c.dp(22)
        page.col.addView(box, boxLp)
        input = field
        clear = x

        val b = LinearLayout(c)
        b.orientation = LinearLayout.VERTICAL
        b.addView(c.tv("Browse", 20f, pal.text, W_BOLD).also { it.setPadding(0, c.dp(28), 0, c.dp(4)) })
        browse = b
        page.col.addView(b)
        val r = LinearLayout(c)
        r.orientation = LinearLayout.VERTICAL
        results = r
        page.col.addView(r)

        field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                x.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                pending?.let { handler.removeCallbacks(it) }
                val run = Runnable { renderResults() }
                pending = run
                handler.postDelayed(run, 140)
            }
        })
        renderBrowse()
        return page.scroll
    }

    override fun onLibraryChanged() { if (results != null) { renderBrowse(); renderResults() } }
    override fun onPlaybackChanged() { if (!input?.text.isNullOrBlank()) renderResults() }

    private fun renderBrowse() {
        val c = host.ctx
        val b = browse ?: return
        while (b.childCount > 1) b.removeViewAt(1)
        val grid = LinearLayout(c)
        grid.orientation = LinearLayout.VERTICAL
        val items = listOf(
            arrayOf<Any>(R.drawable.ic_music_note, "Songs", "${Library.tracks.size} tracks"),
            arrayOf<Any>(R.drawable.ic_album, "Albums", "${Library.albums.size} albums"),
            arrayOf<Any>(R.drawable.ic_person, "Artists", "${Library.artists.size} artists"),
            arrayOf<Any>(R.drawable.ic_heart, "Liked", "${Store.likedCount()} songs")
        )
        val actions = listOf<() -> Unit>(
            { host.open(Collections.songs(host)) },
            { host.open(Collections.albums(host)) },
            { host.open(Collections.artists(host)) },
            { host.open(Collections.liked(host)) }
        )
        for (r in 0 until 2) {
            val line = LinearLayout(c)
            line.orientation = LinearLayout.HORIZONTAL
            for (col in 0 until 2) {
                val i = r * 2 + col
                val tile = browseTile(items[i][0] as Int, items[i][1] as String, items[i][2] as String, actions[i])
                val m = tile.neoMargin()
                val lp = LinearLayout.LayoutParams(0, WRAP, 1f)
                lp.leftMargin = if (col == 0) -m else c.dp(6) - m
                lp.rightMargin = if (col == 1) -m else c.dp(6) - m
                lp.topMargin = -m
                lp.bottomMargin = -m
                line.addView(tile, lp)
            }
            val ll = LinearLayout.LayoutParams(MATCH, WRAP)
            ll.topMargin = c.dp(12)
            grid.addView(line, ll)
        }
        b.addView(grid)
    }

    private fun browseTile(icon: Int, title: String, subtitle: String, onClick: () -> Unit): LinearLayout {
        val c = host.ctx
        val pal = Theme.p
        val tile = LinearLayout(c)
        tile.orientation = LinearLayout.VERTICAL
        tile.applyNeo(c.neo(NeoStyle.RAISED, NeoLevel.STANDARD, 22f), c.dp(18), c.dp(18), c.dp(18), c.dp(18))
        tile.addView(c.iconView(icon, pal.accentText), LinearLayout.LayoutParams(c.dp(28), c.dp(28)))
        val t = c.tv(title, 17f, pal.text, W_BOLD)
        t.setPadding(0, c.dp(16), 0, 0)
        tile.addView(t)
        val s = c.tv(subtitle, 13f, pal.textSecondary)
        s.setPadding(0, c.dp(4), 0, 0)
        tile.addView(s)
        tile.isClickable = true
        tile.setOnClickListener { onClick() }
        tile.pressScale()
        return tile
    }

    private fun renderResults() {
        val c = host.ctx
        val r = results ?: return
        val b = browse ?: return
        r.removeAllViews()
        val q = input?.text?.toString()?.trim()?.lowercase().orEmpty()
        if (q.isEmpty()) {
            b.visibility = View.VISIBLE
            return
        }
        b.visibility = View.GONE
        val matches = Library.tracks.filter { it.searchKey.contains(q) }
        if (matches.isEmpty()) {
            r.addNeo(c.infoCard("No matches", "Nothing on this device matches “${input?.text}”."), topGap = c.dp(24))
            return
        }
        val shown = matches.take(50)
        val label = if (matches.size > shown.size) "Showing the first 50 of ${matches.size} songs" else "${matches.size} songs"
        val head = c.tv(label, 14f, Theme.p.textSecondary)
        head.setPadding(0, c.dp(24), 0, c.dp(8))
        r.addView(head)
        addTrackRows(host, r, shown, matches)
    }
}

// ---------------------------------------------------------------- Library

class LibraryScreen(host: Host) : Screen(host) {
    private var body: LinearLayout? = null

    override fun createView(): View {
        val c = host.ctx
        val page = Page(c)
        page.col.addView(c.screenTitle("Your collection", "Library"))
        val d = LinearLayout(c)
        d.orientation = LinearLayout.VERTICAL
        page.col.addView(d)
        body = d
        render()
        return page.scroll
    }

    override fun onLibraryChanged() { if (body != null) render() }
    override fun onPlaybackChanged() { if (body != null) render() }

    private fun entry(icon: Int, title: String, subtitle: String, onClick: () -> Unit): LinearLayout {
        val c = host.ctx
        val pal = Theme.p
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.applyNeo(c.neo(NeoStyle.RAISED, NeoLevel.STANDARD, 22f), c.dp(14), c.dp(14), c.dp(12), c.dp(14))
        val wrap = FrameLayout(c)
        wrap.background = c.neo(NeoStyle.INSET, NeoLevel.SUBTLE, -1f)
        wrap.addView(c.iconView(icon, pal.accentText), FrameLayout.LayoutParams(c.dp(22), c.dp(22), Gravity.CENTER))
        row.addView(wrap, LinearLayout.LayoutParams(c.dp(48), c.dp(48)))
        val texts = LinearLayout(c)
        texts.orientation = LinearLayout.VERTICAL
        texts.addView(c.tv(title, 16f, pal.text, W_MEDIUM))
        val s = c.tv(subtitle, 13f, pal.textSecondary)
        s.setPadding(0, c.dp(3), 0, 0)
        texts.addView(s)
        val tl = LinearLayout.LayoutParams(0, WRAP, 1f)
        tl.leftMargin = c.dp(14)
        row.addView(texts, tl)
        row.addView(c.iconView(R.drawable.ic_chevron_right, pal.textMuted), LinearLayout.LayoutParams(c.dp(24), c.dp(24)))
        row.isClickable = true
        row.isFocusable = true
        row.contentDescription = "$title, $subtitle"
        row.setOnClickListener { onClick() }
        row.pressScale()
        return row
    }

    private fun render() {
        val c = host.ctx
        val d = body ?: return
        d.removeAllViews()
        d.addNeo(entry(R.drawable.ic_heart, "Liked songs", "${Store.likedCount()} songs") { host.open(Collections.liked(host)) }, topGap = c.dp(24))
        d.addNeo(entry(R.drawable.ic_playlist, "Playlists", "${Store.playlists.size} playlists") { host.open(Collections.playlists(host)) }, topGap = c.dp(12))
        d.addNeo(entry(R.drawable.ic_album, "Albums", "${Library.albums.size} albums") { host.open(Collections.albums(host)) }, topGap = c.dp(12))
        d.addNeo(entry(R.drawable.ic_person, "Artists", "${Library.artists.size} artists") { host.open(Collections.artists(host)) }, topGap = c.dp(12))
        d.addNeo(entry(R.drawable.ic_phone, "On this device", "${Library.tracks.size} songs") { host.open(Collections.songs(host)) }, topGap = c.dp(12))

        val head = c.sectionHeader("Recently added")
        val hl = LinearLayout.LayoutParams(MATCH, WRAP)
        hl.topMargin = c.dp(28)
        d.addView(head, hl)
        val box = LinearLayout(c)
        box.orientation = LinearLayout.VERTICAL
        val bl = LinearLayout.LayoutParams(MATCH, WRAP)
        bl.topMargin = c.dp(4)
        d.addView(box, bl)
        if (!host.hasPermission()) {
            box.addNeo(c.infoCard("Access needed", "Allow SONARA to read audio on this phone to build your library.", "Allow access") { host.requestPermission() }, topGap = c.dp(8))
        } else if (Library.tracks.isEmpty()) {
            box.addNeo(c.infoCard("Your library is quiet", Library.lastError ?: "Add audio files to this phone, then scan."), topGap = c.dp(8))
        } else {
            addTrackRows(host, box, Library.recent.take(8), Library.recent)
        }
        val scan = c.primaryButton(if (Library.scanning) "Scanning…" else "Scan device", R.drawable.ic_refresh) {
            if (!Library.scanning) host.requestScan()
        }
        d.addNeo(scan, topGap = c.dp(20))
    }
}

// ---------------------------------------------------------------- Settings

class SettingsScreen(host: Host) : Screen(host) {

    private fun card(title: String): LinearLayout {
        val c = host.ctx
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.applyNeo(c.neo(NeoStyle.RAISED, NeoLevel.STANDARD, 24f), c.dp(20), c.dp(18), c.dp(20), c.dp(12))
        val k = c.tv(title.uppercase(), 12f, Theme.p.accentText, W_BOLD)
        k.letterSpacing = 0.14f
        col.addView(k)
        return col
    }

    private fun row(title: String, subtitle: String, trailing: View?, onClick: (() -> Unit)?): LinearLayout {
        val c = host.ctx
        val r = LinearLayout(c)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.minimumHeight = c.dp(60)
        r.setPadding(0, c.dp(10), 0, c.dp(10))
        val texts = LinearLayout(c)
        texts.orientation = LinearLayout.VERTICAL
        texts.addView(c.tv(title, 16f, Theme.p.text, W_MEDIUM))
        val s = c.tv(subtitle, 13f, Theme.p.textSecondary)
        s.setPadding(0, c.dp(3), 0, 0)
        texts.addView(s)
        r.addView(texts, LinearLayout.LayoutParams(0, WRAP, 1f))
        if (trailing != null) r.addView(trailing)
        if (onClick != null) {
            r.isClickable = true
            r.isFocusable = true
            r.setOnClickListener { onClick() }
        }
        return r
    }

    override fun createView(): View {
        val c = host.ctx
        val pal = Theme.p
        val page = Page(c)
        page.col.addView(c.backHeader(host, "Settings", "Make SONARA yours"))

        val appearance = card("Appearance")
        val chips = LinearLayout(c)
        chips.orientation = LinearLayout.HORIZONTAL
        val dark = c.chip("Dark", pal.dark) { if (!Theme.p.dark) host.setDark(true) }
        val light = c.chip("Light", !pal.dark) { if (Theme.p.dark) host.setDark(false) }
        chips.addView(dark)
        val ll = LinearLayout.LayoutParams(WRAP, WRAP)
        ll.leftMargin = c.dp(4)
        chips.addView(light, ll)
        val cl = LinearLayout.LayoutParams(MATCH, WRAP)
        cl.topMargin = c.dp(10)
        appearance.addView(chips, cl)
        page.col.addNeo(appearance, topGap = c.dp(24))

        val library = card("Library")
        val sw = NeoSwitch(c)
        sw.setChecked(Store.hideShort, false)
        sw.contentDescription = "Hide clips shorter than 30 seconds"
        sw.onChange = { v ->
            Store.saveHideShort(v)
            host.requestScan()
        }
        library.addView(row("Hide short clips", "Ignore audio shorter than 30 seconds", sw, { sw.performClick() }))
        library.addView(row("Rescan device", "Look for new music now", c.iconView(R.drawable.ic_refresh, pal.accentText).also { it.layoutParams = LinearLayout.LayoutParams(c.dp(24), c.dp(24)) }, { host.requestScan(); host.toast("Scanning…") }))
        page.col.addNeo(library, topGap = c.dp(16))

        val sound = card("Sound")
        sound.addView(row("Equalizer", "Open your phone's system equalizer", c.iconView(R.drawable.ic_equalizer, pal.accentText).also { it.layoutParams = LinearLayout.LayoutParams(c.dp(24), c.dp(24)) }, { host.openEqualizer() }))
        page.col.addNeo(sound, topGap = c.dp(16))

        val about = card("About")
        val brand = LinearLayout(c)
        brand.orientation = LinearLayout.HORIZONTAL
        brand.gravity = Gravity.CENTER_VERTICAL
        brand.setPadding(0, c.dp(12), 0, c.dp(8))
        brand.addView(c.logoMark(44f))
        val bt = LinearLayout(c)
        bt.orientation = LinearLayout.VERTICAL
        val name = c.tv("SONARA", 18f, pal.text, W_BOLD)
        name.letterSpacing = 0.18f
        bt.addView(name)
        val v = c.tv("Version ${versionName()}", 13f, pal.textSecondary)
        v.setPadding(0, c.dp(4), 0, 0)
        bt.addView(v)
        val bl = LinearLayout.LayoutParams(0, WRAP, 1f)
        bl.leftMargin = c.dp(14)
        brand.addView(bt, bl)
        about.addView(brand)
        val blurb = c.tv("An offline music player for the songs already on your phone. No account, no ads, and your listening never leaves this device.", 14f, pal.textSecondary)
        blurb.setLineSpacing(0f, 1.15f)
        blurb.setPadding(0, c.dp(4), 0, c.dp(8))
        about.addView(blurb)
        page.col.addNeo(about, topGap = c.dp(16))
        return page.scroll
    }

    private fun versionName(): String = try {
        val c = host.ctx
        c.packageManager.getPackageInfo(c.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }
}
