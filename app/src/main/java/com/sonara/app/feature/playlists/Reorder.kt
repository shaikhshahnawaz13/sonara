package com.sonara.app.feature.playlists

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.sonara.app.data.model.PlaylistEntry
import com.sonara.app.data.model.PlaylistRules

/** Where one list item currently sits: its index among the entries, and its offset and size in pixels. */
internal data class ItemSlot(val index: Int, val offset: Int, val size: Int)

/**
 * The index the dragged item should take, or null to stay put: the entry whose slot contains the dragged
 * item's centre. Pure so the rule can be tested without a screen.
 */
internal fun findSwapTarget(draggedIndex: Int, draggedCentre: Float, slots: List<ItemSlot>): Int? =
    slots.firstOrNull { it.index != draggedIndex && draggedCentre >= it.offset && draggedCentre < it.offset + it.size }?.index

internal data class EntryMove(val from: Int, val to: Int)

/**
 * Drag-and-drop reordering for the playlist list.
 *
 * While a drag runs, the order lives in [working] and the dragged row follows the finger through a
 * translation computed from where the finger wants it ([top]) and where the list currently lays it out,
 * so a row that moves past a neighbour needs no compensation. The database is written once, on drop.
 * [working] stays until the database reports the same order, so the list never flashes the old order.
 */
@Stable
internal class EntryReorder(
    private val listState: LazyListState,
    /** Lazy-list items before the first entry (the header). */
    private val headerCount: Int,
) {
    var working: List<PlaylistEntry>? by mutableStateOf(null)
        private set
    var draggingKey: Long? by mutableStateOf(null)
        private set
    val isDragging: Boolean get() = draggingKey != null

    private var top by mutableFloatStateOf(0f)
    private var startIndex = -1

    fun start(entryId: Long, current: List<PlaylistEntry>): Boolean {
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == entryId } ?: return false
        working = current
        startIndex = info.index - headerCount
        top = info.offset.toFloat()
        draggingKey = entryId
        return true
    }

    fun dragBy(deltaY: Float) {
        top += deltaY
    }

    /** How far the dragged row must be translated from where the list lays it out. Read while drawing. */
    fun translationOf(entryId: Long): Float {
        val laidOut = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == entryId }?.offset ?: return 0f
        return top - laidOut
    }

    /** Ends the drag. Returns the move to save, or null when the row ended where it began. */
    fun finish(): EntryMove? {
        val key = draggingKey ?: return null
        draggingKey = null
        val order = working
        val to = order?.indexOfFirst { it.entryId == key } ?: -1
        if (to < 0 || to == startIndex) {
            working = null
            return null
        }
        return EntryMove(startIndex, to)
    }

    /** Drops the working order once the database shows it, or when it has been waiting too long. */
    fun settle(saved: List<PlaylistEntry>, force: Boolean = false) {
        if (isDragging) return
        val order = working ?: return
        if (force || order.map { it.entryId } == saved.map { it.entryId }) working = null
    }

    /** Swaps the dragged row past the row it overlaps and scrolls the list near its edges, once per frame. */
    suspend fun follow(autoScrollPx: Float, edgePx: Float) {
        val key = draggingKey ?: return
        var cooldown = 0
        while (draggingKey == key) {
            withFrameNanos { }
            // After a swap the list needs a frame or two to lay out again; deciding on stale positions would swap back.
            if (cooldown > 0) {
                cooldown--
                continue
            }
            val layout = listState.layoutInfo
            val dragged = layout.visibleItemsInfo.firstOrNull { it.key == key } ?: continue
            val slots = layout.visibleItemsInfo.mapNotNull { info ->
                if (info.key is Long) ItemSlot(info.index - headerCount, info.offset, info.size) else null
            }
            val from = dragged.index - headerCount
            val target = findSwapTarget(from, top + dragged.size / 2f, slots)
            val order = working
            if (target != null && order != null) {
                working = PlaylistRules.move(order, from, target)
                cooldown = SETTLE_FRAMES
                continue
            }
            val delta = when {
                top < layout.viewportStartOffset + edgePx -> -autoScrollPx
                top + dragged.size > layout.viewportEndOffset - edgePx -> autoScrollPx
                else -> 0f
            }
            if (delta != 0f) listState.scrollBy(delta)
        }
    }

    private companion object {
        const val SETTLE_FRAMES = 2
    }
}
