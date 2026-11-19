package com.sonara.app.core.common

import java.util.Locale

/** "m:ss", or "h:mm:ss" from one hour up. Negative values show as zero. */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L)) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** Total length for headers: "45 MIN" or "1 H 05 MIN". Rounds to the nearest minute, at least 1 for non-empty. */
fun formatTotalDuration(millis: Long): String {
    if (millis <= 0L) return "0 MIN"
    val totalMinutes = ((millis + 30_000L) / 60_000L).coerceAtLeast(1L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0) String.format(Locale.ROOT, "%d H %02d MIN", hours, minutes) else "$minutes MIN"
}
