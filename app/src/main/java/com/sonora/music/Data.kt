package com.sonora.music

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAdded: Long,
    val uri: Uri
) {
    val searchKey: String = "$title $artist $album".lowercase()
}

class Album(val id: Long, val name: String, val artist: String, val tracks: List<Track>)
class Artist(val name: String, val tracks: List<Track>)

fun formatTime(ms: Int): String {
    val total = if (ms < 0) 0 else ms / 1000
    val m = total / 60
    val s = total % 60
    return "$m:${if (s < 10) "0" else ""}$s"
}

/** In-memory snapshot of the device's music, read from MediaStore on a background thread. */
object Library {
    var tracks: List<Track> = emptyList()
        private set
    var albums: List<Album> = emptyList()
        private set
    var artists: List<Artist> = emptyList()
        private set
    var recent: List<Track> = emptyList()
        private set
    var scanning = false
        private set
    var loadedOnce = false
        private set
    var lastError: String? = null
        private set

    private var byId: Map<Long, Track> = emptyMap()
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val listeners = ArrayList<() -> Unit>()

    fun addListener(l: () -> Unit) { if (!listeners.contains(l)) listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }
    private fun fire() { for (l in ArrayList(listeners)) l() }

    fun trackById(id: Long): Track? = byId[id]

    fun scan(context: Context, hideShort: Boolean) {
        if (scanning) return
        scanning = true
        lastError = null
        fire()
        val app = context.applicationContext
        executor.execute {
            var error: String? = null
            var result: List<Track> = emptyList()
            try {
                result = query(app, if (hideShort) 30000L else 0L)
            } catch (e: SecurityException) {
                error = "SONARA doesn't have permission to read your audio."
            } catch (e: Exception) {
                error = "Couldn't read your music library."
            }
            main.post { publish(result, error) }
        }
    }

    private fun publish(list: List<Track>, error: String?) {
        scanning = false
        lastError = error
        if (error == null) {
            tracks = list
            byId = list.associateBy { it.id }
            albums = list.groupBy { it.albumId }
                .map { entry -> Album(entry.key, entry.value.first().album, entry.value.first().artist, entry.value) }
                .sortedBy { it.name.lowercase() }
            artists = list.groupBy { it.artist }
                .map { entry -> Artist(entry.key, entry.value) }
                .sortedBy { it.name.lowercase() }
            recent = list.sortedByDescending { it.dateAdded }.take(12)
            loadedOnce = true
        }
        fire()
    }

    private fun query(ctx: Context, minDuration: Long): List<Track> {
        val out = ArrayList<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val args = arrayOf(minDuration.toString())
        val order = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        val cursor = ctx.contentResolver.query(collection, projection, selection, args, order)
        cursor?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val iDuration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(iId)
                val title = c.getString(iTitle)
                val artist = c.getString(iArtist)
                val album = c.getString(iAlbum)
                out.add(
                    Track(
                        id = id,
                        title = if (title.isNullOrBlank()) "Unknown track" else title,
                        artist = if (artist.isNullOrBlank() || artist == "<unknown>") "Unknown artist" else artist,
                        album = if (album.isNullOrBlank() || album == "<unknown>") "Unknown album" else album,
                        albumId = c.getLong(iAlbumId),
                        durationMs = c.getLong(iDuration),
                        dateAdded = c.getLong(iAdded),
                        uri = android.content.ContentUris.withAppendedId(collection, id)
                    )
                )
            }
        }
        return out
    }
}

/** Everything the user creates inside SONARA: liked songs, playlists and settings. Stored on device only. */
object Store {
    private var prefs: SharedPreferences? = null
    private val liked = LinkedHashSet<Long>()
    val playlists = LinkedHashMap<String, MutableList<Long>>()
    private val listeners = ArrayList<() -> Unit>()

    var hideShort: Boolean = true
        private set

    fun init(ctx: Context) {
        if (prefs != null) return
        val p = ctx.applicationContext.getSharedPreferences("sonara_store", Context.MODE_PRIVATE)
        prefs = p
        hideShort = p.getBoolean("hide_short", true)
        val likedRaw = p.getString("liked", "") ?: ""
        for (part in likedRaw.split(",")) {
            val id = part.toLongOrNull()
            if (id != null) liked.add(id)
        }
        val plRaw = p.getString("playlists", null)
        if (plRaw != null) {
            try {
                val obj = JSONObject(plRaw)
                val names = obj.keys()
                while (names.hasNext()) {
                    val name = names.next()
                    val arr = obj.getJSONArray(name)
                    val ids = ArrayList<Long>()
                    for (i in 0 until arr.length()) ids.add(arr.getLong(i))
                    playlists[name] = ids
                }
            } catch (e: Exception) {
                playlists.clear()
            }
        }
    }

    fun addListener(l: () -> Unit) { if (!listeners.contains(l)) listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }
    private fun fire() { for (l in ArrayList(listeners)) l() }

    fun isLiked(id: Long): Boolean = liked.contains(id)
    fun likedCount(): Int = liked.size

    fun likedTracks(): List<Track> = liked.toList().asReversed().mapNotNull { Library.trackById(it) }

    fun toggleLike(id: Long): Boolean {
        val nowLiked = if (liked.contains(id)) { liked.remove(id); false } else { liked.add(id); true }
        prefs?.edit()?.putString("liked", liked.joinToString(","))?.apply()
        fire()
        return nowLiked
    }

    fun setHideShort(v: Boolean) {
        hideShort = v
        prefs?.edit()?.putBoolean("hide_short", v)?.apply()
    }

    fun playlistTracks(name: String): List<Track> =
        (playlists[name] ?: emptyList<Long>()).mapNotNull { Library.trackById(it) }

    fun createPlaylist(name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty() || playlists.containsKey(clean)) return false
        playlists[clean] = ArrayList()
        savePlaylists()
        return true
    }

    fun addToPlaylist(name: String, trackId: Long): Boolean {
        val list = playlists[name] ?: return false
        if (list.contains(trackId)) return false
        list.add(trackId)
        savePlaylists()
        return true
    }

    fun removeFromPlaylist(name: String, trackId: Long) {
        playlists[name]?.remove(trackId)
        savePlaylists()
    }

    fun deletePlaylist(name: String) {
        playlists.remove(name)
        savePlaylists()
    }

    private fun savePlaylists() {
        val obj = JSONObject()
        for ((name, ids) in playlists) {
            val arr = JSONArray()
            for (id in ids) arr.put(id)
            obj.put(name, arr)
        }
        prefs?.edit()?.putString("playlists", obj.toString())?.apply()
        fire()
    }
}
