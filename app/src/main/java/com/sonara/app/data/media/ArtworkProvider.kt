package com.sonara.app.data.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Loads the artwork of a track: embedded art when the file has it, otherwise the album art MediaStore
 * knows about. Returns null when there is none; the UI then shows its own empty state, never a fake image.
 *
 * Built for scrolling:
 *  - [peek] answers from memory without suspending, so a row that scrolls back into view draws its
 *    artwork in its first frame instead of flashing empty;
 *  - decoding happens off the main thread, at most [MAX_PARALLEL_DECODES] at a time;
 *  - a request whose row has already scrolled away is cancelled while it waits for a slot, so a fast fling
 *    never decodes artwork nobody will see.
 */
class ArtworkProvider(
    private val resolver: ContentResolver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private data class Key(val trackId: Long, val sizePx: Int)

    private val cache = object : LruCache<Key, Bitmap>(cacheBytes()) {
        override fun sizeOf(key: Key, value: Bitmap): Int = value.byteCount
    }

    /** Tracks known to have no artwork, so scrolling past them does not retry the lookup. */
    private val missing = HashSet<Key>()

    private val decodeSlots = Semaphore(MAX_PARALLEL_DECODES)

    /** The cached bitmap, or null when it is not in memory (which does not mean there is none). */
    fun peek(trackId: Long, sizePx: Int): Bitmap? = synchronized(cache) { cache.get(Key(trackId, sizePx)) }

    suspend fun load(trackId: Long, sizePx: Int): Bitmap? {
        val key = Key(trackId, sizePx)
        synchronized(cache) {
            cache.get(key)?.let { return it }
            if (key in missing) return null
        }
        // Waiting here is cancellable: a row that left the screen stops waiting and decodes nothing.
        val bitmap = decodeSlots.withPermit {
            synchronized(cache) { cache.get(key) }
                ?: withContext(dispatcher) {
                    try {
                        resolver.loadThumbnail(MediaStoreScanner.trackUri(trackId), Size(sizePx, sizePx), null)
                    } catch (_: Exception) {
                        null
                    }
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
        const val MAX_PARALLEL_DECODES = 3
        const val MIN_CACHE_BYTES = 16 * 1024 * 1024
        const val MAX_CACHE_BYTES = 48 * 1024 * 1024

        /** An eighth of the app's heap, bounded, so a long scroll through albums stays cached. */
        fun cacheBytes(): Int =
            (Runtime.getRuntime().maxMemory() / 8).coerceIn(MIN_CACHE_BYTES.toLong(), MAX_CACHE_BYTES.toLong()).toInt()
    }
}
