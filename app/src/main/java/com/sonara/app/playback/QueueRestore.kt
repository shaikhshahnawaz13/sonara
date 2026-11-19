package com.sonara.app.playback

/** A queue ready to hand to the player. */
data class RestoredQueue(
    val trackIds: List<Long>,
    val startIndex: Int,
    val startPositionMs: Long,
)

/** Pure rules for rebuilding the saved queue when some of its tracks may have been deleted. */
object QueueRestore {

    /**
     * Drops saved tracks that no longer exist. If the track that was playing survives, playback resumes in it
     * at the saved position; otherwise it resumes at the track that now occupies its place, from the start.
     * Returns null when nothing is left to play.
     */
    fun resolve(
        savedIds: List<Long>,
        available: Set<Long>,
        savedIndex: Int,
        savedPositionMs: Long,
    ): RestoredQueue? {
        if (savedIds.isEmpty()) return null
        val kept = ArrayList<Long>(savedIds.size)
        var keptBeforeSaved = 0
        var currentSurvived = false
        savedIds.forEachIndexed { index, id ->
            if (id in available) {
                if (index < savedIndex) keptBeforeSaved++
                if (index == savedIndex) currentSurvived = true
                kept += id
            }
        }
        if (kept.isEmpty()) return null
        val startIndex = keptBeforeSaved.coerceIn(0, kept.lastIndex)
        val position = if (currentSurvived) savedPositionMs.coerceAtLeast(0L) else 0L
        return RestoredQueue(kept, startIndex, position)
    }

    /**
     * Given which requested items could be resolved, the start index to use for the resolved list:
     * the number of surviving items before [startIndex]. An unset index (negative) stays unset.
     */
    fun adjustStartIndex(survived: List<Boolean>, startIndex: Int): Int {
        if (startIndex < 0) return startIndex
        val kept = survived.count { it }
        if (kept == 0) return 0
        val before = survived.take(startIndex.coerceAtMost(survived.size)).count { it }
        return before.coerceIn(0, kept - 1)
    }
}
