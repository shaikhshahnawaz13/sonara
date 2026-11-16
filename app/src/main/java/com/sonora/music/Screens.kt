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
                v.unclip()
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
        scroll.clipChildren = false
        col.clipChildren = false
        col.clipToPadding = false
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

fun Context.appHeader(onSettings: () -> Unit): LinearLayout {
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
        clipChildren = false
        clipToPadding = false
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
        if (trailing != null) {
            val tl = LinearLayout.LayoutParams(trailing.layoutParams?.width ?: WRAP, trailing.layoutParams?.height ?: WRAP)
            if (trailing is NeoSwitch) tl.rightMargin = -trailing.m
            r.addView(trailing, tl)
        }
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
