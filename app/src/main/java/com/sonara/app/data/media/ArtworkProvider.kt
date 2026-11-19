package com.sonara.app.data.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the artwork of a track: embedded art when the file has it, otherwise the album art MediaStore
 * knows about. Returns null when there is none; the UI then shows its own empty state, never a fake image.
 */
class ArtworkProvider(
    private val resolver: ContentResolver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private data class Key(val trackId: Long, val sizePx: Int)

    private val cache = object : LruCache<Key, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: Key, value: Bitmap): Int = value.byteCount
    }

    /** Tracks known to have no artwork, so scrolling past them does not retry the lookup. */
    private val missing = HashSet<Key>()

    suspend fun load(trackId: Long, sizePx: Int): Bitmap? {
        val key = Key(trackId, sizePx)
        synchronized(cache) {
            cache.get(key)?.let { return it }
            if (key in missing) return null
        }
        val bitmap = withContext(dispatcher) {
            try {
                resolver.loadThumbnail(MediaStoreScanner.trackUri(trackId), Size(sizePx, sizePx), null)
            } catch (_: Exception) {
                null
            }
        }
        synchronized(cache) {
            if (bitmap != null) cache.put(key, bitmap) else missing += key
        }
        return bitmap
    }

    /** Forget "no artwork" results, e.g. after a rescan, since tags may have changed. */
    fun clearMisses() = synchronized(cache) { missing.clear() }

    private companion object {
        const val CACHE_BYTES = 16 * 1024 * 1024
    }
}
