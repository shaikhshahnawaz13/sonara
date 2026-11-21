package com.sonara.app.feature.playlists

import com.sonara.app.data.media.ArtworkProvider
import com.sonara.app.data.repository.PlaylistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What a playlist screen needs, fetched in the moment between a tap and the screen sliding in.
 *
 * Why: the slide-in is driven by the clock, so a heavy frame during it is a visible hitch, and the fastest part
 * of the slide is its first few frames. Without this, the screen composed an empty "loading" body in the first
 * frame, the database answered a frame or two later, and the whole list (header, rows, artwork) was composed
 * in the middle of the fastest part of the movement. With the data and the visible artwork already in memory,
 * all of that composition happens in the frame that starts the transition, before it moves.
 *
 * Nothing here changes what is shown: if the data is late or the fetch fails, the screen opens exactly as it
 * did before and loads for itself. The waiting is bounded ([DATA_BUDGET_MS] + [ART_BUDGET_MS]).
 */
class PlaylistPrefetch(
    private val repository: PlaylistRepository,
    private val artwork: ArtworkProvider,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val slot = PrefetchSlot<PlaylistDetail>(MAX_AGE_MS, clock)

    /**
     * Fetches the playlist and warms the artwork the first screenful shows, then returns. Never throws and
     * never waits longer than its budgets. [headerPx] and [rowPx] are the bitmap sizes the detail screen asks for.
     */
    suspend fun warm(playlistId: Long, headerPx: Int, rowPx: Int) {
        val fetched: PlaylistDetail? = try {
            withTimeoutOrNull(DATA_BUDGET_MS) { playlistDetailFlow(repository, playlistId).first() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        val detail = fetched ?: return
        if (detail.summary == null) return
        slot.put(playlistId, detail)

        // Decoding continues in the app scope even if the budget runs out, so it is never wasted.
        val jobs = buildList {
            detail.summary.artworkTrackId?.let { id -> add(scope.launch { artwork.load(id, headerPx) }) }
            detail.entries.asSequence().mapNotNull { it.track?.id }.take(FIRST_SCREEN_ROWS).distinct()
                .forEach { id -> add(scope.launch { artwork.load(id, rowPx) }) }
        }
        withTimeoutOrNull(ART_BUDGET_MS) { jobs.joinAll() }
    }

    /** The data fetched for [playlistId], once. Stale or unrelated data is never returned. */
    fun take(playlistId: Long): PlaylistDetail? = slot.take(playlistId)

    companion object {
        const val DATA_BUDGET_MS = 150L
        const val ART_BUDGET_MS = 80L
        const val MAX_AGE_MS = 3_000L
        const val FIRST_SCREEN_ROWS = 14
    }
}

/**
 * Holds one prefetched value for one key. It is returned once, only to the key it was fetched for and only while
 * it is fresh; a different key or an old value is dropped, so a screen can never open with someone else's or
 * out-of-date data. Not thread-safe by design: it is touched from the main thread only.
 */
class PrefetchSlot<T>(
    private val maxAgeMs: Long,
    private val clock: () -> Long,
) {
    private var key: Long? = null
    private var value: T? = null
    private var storedAt = 0L

    fun put(key: Long, value: T) {
        this.key = key
        this.value = value
        storedAt = clock()
    }

    fun take(key: Long): T? {
        val held = value
        val heldKey = this.key
        val fresh = clock() - storedAt <= maxAgeMs
        // Any look empties the slot: the value is for one open, and a look for another key means it was not used.
        this.key = null
        value = null
        return if (heldKey == key && fresh) held else null
    }
}
