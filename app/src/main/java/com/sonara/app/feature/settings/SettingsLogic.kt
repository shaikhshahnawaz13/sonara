package com.sonara.app.feature.settings

import com.sonara.app.data.repository.SyncState

/** Wording and small decisions for Settings, kept free of Android types so they can be tested. */
internal object SettingsLogic {

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    /** "Never", "Just now", "12 min ago", "3 h ago", "5 d ago". A clock set backwards reads as "Just now". */
    fun lastScanText(lastScanAt: Long?, now: Long): String {
        if (lastScanAt == null) return "Never scanned"
        val age = (now - lastScanAt).coerceAtLeast(0L)
        return when {
            age < MINUTE -> "Just now"
            age < HOUR -> "${age / MINUTE} min ago"
            age < DAY -> "${age / HOUR} h ago"
            else -> "${age / DAY} d ago"
        }
    }

    fun songs(count: Int): String = if (count == 1) "1 song" else "$count songs"

    /** What the last rescan did, in one line. Null while there is nothing to report. */
    fun scanResultText(state: SyncState): String? = when (state) {
        SyncState.Idle -> null
        SyncState.Scanning -> "Scanning your music…"
        is SyncState.Failed -> state.message
        is SyncState.Done -> when {
            state.skipped -> "Library is up to date (${songs(state.total)})"
            else -> {
                val changes = buildList {
                    if (state.added > 0) add("${state.added} added")
                    if (state.updated > 0) add("${state.updated} updated")
                    if (state.removed > 0) add("${state.removed} removed")
                }
                val head = if (changes.isEmpty()) "Library is up to date" else changes.joinToString(" · ")
                val tail = if (state.excluded > 0) " · ${songs(state.excluded)} kept out by excluded folders" else ""
                "$head · ${songs(state.total)}$tail"
            }
        }
    }

    /** "Version 1.0.0-m6.2 (11)", with the kind of build when it is a debug build. */
    fun versionText(versionName: String?, versionCode: Long, debuggable: Boolean): String {
        val name = versionName?.takeIf { it.isNotBlank() } ?: "unknown"
        return "$name ($versionCode)" + if (debuggable) " · debug build" else ""
    }

    fun notificationStatus(enabled: Boolean): String =
        if (enabled) "Allowed" else "Off. Playback still works, but the controls will not show in the notification"

    fun excludedCountText(count: Int): String = when (count) {
        0 -> "None"
        1 -> "1 folder"
        else -> "$count folders"
    }

    /** Sentence for the clear dialogs and results. */
    fun clearedText(freedBytes: Long, what: String): String =
        if (freedBytes <= 0L) "$what was already empty" else "Cleared $what (${com.sonara.app.data.storage.ByteFormat.format(freedBytes)})"
}
