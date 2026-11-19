package com.sonara.app.data.media

import android.content.Context
import android.provider.MediaStore

/**
 * A cheap "has anything changed?" token for the external MediaStore volume. If it equals the value stored
 * after the last scan, the full scan is skipped on launch.
 */
class MediaStoreMarker(private val context: Context) {
    fun current(): String {
        val version = MediaStore.getVersion(context)
        val generation = MediaStore.getGeneration(context, MediaStore.VOLUME_EXTERNAL)
        return "$version:$generation"
    }
}
