package com.sonara.app.data.storage

import java.io.File
import java.nio.file.Files

/** What a clear did. [failed] counts items that could not be removed. */
data class ClearResult(val freedBytes: Long, val removedItems: Int, val failed: Int)

/**
 * Measures and empties the app's own cache directory. Deliberately small and strict:
 *  - it only ever deletes below the directory it is given, never the directory itself;
 *  - symbolic links are removed as links and never followed, so a link cannot lead it elsewhere;
 *  - it knows nothing about music, the database or settings, and is never pointed at them.
 */
object CacheCleaner {

    fun sizeOf(dir: File): Long {
        if (!dir.isDirectory) return 0L
        var total = 0L
        val stack = ArrayDeque<File>()
        stack.add(dir)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val children = current.listFiles() ?: continue
            for (child in children) {
                when {
                    Files.isSymbolicLink(child.toPath()) -> Unit
                    child.isDirectory -> stack.add(child)
                    else -> total += child.length()
                }
            }
        }
        return total
    }

    fun clearContents(dir: File): ClearResult {
        if (!dir.isDirectory) return ClearResult(0, 0, 0)
        var freed = 0L
        var removed = 0
        var failed = 0
        fun delete(file: File) {
            val isLink = Files.isSymbolicLink(file.toPath())
            if (!isLink && file.isDirectory) {
                file.listFiles()?.forEach { delete(it) }
            }
            val size = if (!isLink && file.isFile) file.length() else 0L
            if (file.delete()) {
                freed += size
                removed++
            } else {
                failed++
            }
        }
        dir.listFiles()?.forEach { delete(it) }
        return ClearResult(freed, removed, failed)
    }
}

/** Sizes shown in Settings. Every figure is measured from files or memory; none is estimated. */
data class StorageReport(
    val artworkMemoryBytes: Long,
    val cacheBytes: Long,
    val libraryDatabaseBytes: Long,
)

object ByteFormat {
    /** 0 B, 512 B, 1.5 KB, 12 MB, 1.2 GB. Binary units, one decimal below 10. */
    fun format(bytes: Long): String {
        if (bytes < 1024) return "${bytes.coerceAtLeast(0)} B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes / 1024.0
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        val text = if (value < 10) "%.1f".format(java.util.Locale.US, value) else "%d".format(java.util.Locale.US, Math.round(value))
        return "$text ${units[unit]}"
    }
}
