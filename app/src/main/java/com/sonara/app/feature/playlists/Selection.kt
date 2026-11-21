package com.sonara.app.feature.playlists

import androidx.compose.runtime.Immutable

/**
 * An ordered set of selected ids. Order is the order of selection ("Select all" appends in list order), so
 * songs are added to a playlist in the order the person picked them. Every operation returns a new value.
 */
@Immutable
class Selection(val ids: Set<Long> = emptySet()) {
    val count: Int get() = ids.size
    val isEmpty: Boolean get() = ids.isEmpty()

    operator fun contains(id: Long): Boolean = id in ids

    fun toggle(id: Long): Selection = Selection(if (id in ids) ids - id else ids + id)

    /** Adds every candidate not already selected. */
    fun selectAll(candidates: Collection<Long>): Selection = Selection(ids + candidates)

    fun clear(): Selection = if (ids.isEmpty()) this else NONE

    /** True when [candidates] is not empty and every one of them is selected. */
    fun coversAll(candidates: Collection<Long>): Boolean = candidates.isNotEmpty() && candidates.all { it in ids }

    /** Drops ids that are no longer valid (a song left the library, an entry was removed). Same instance if nothing changes. */
    fun retainOnly(valid: Collection<Long>): Selection {
        if (ids.isEmpty()) return this
        val keep = valid.toHashSet()
        return if (ids.all { it in keep }) this else Selection(ids.filterTo(LinkedHashSet()) { it in keep })
    }

    companion object {
        val NONE = Selection()
    }
}
