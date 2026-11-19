package com.sonara.app.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sonara.app.core.design.components.SonaraEmptyState
import com.sonara.app.core.design.components.SonaraErrorState
import com.sonara.app.core.design.components.SonaraLoadingState
import com.sonara.app.data.repository.SyncState

/**
 * What to show when the library has no tracks: still scanning, the scan failed, or nothing was found.
 * Only used once audio access is granted.
 */
@Composable
fun LibraryEmptyState(
    sync: SyncState,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (sync) {
        SyncState.Idle, SyncState.Scanning -> SonaraLoadingState(
            modifier = modifier,
            label = "SCANNING",
            detail = "Looking for music on this device.",
        )
        is SyncState.Failed -> SonaraErrorState(
            message = "The library scan failed: ${sync.message}",
            onRetry = onRescan,
            modifier = modifier,
        )
        is SyncState.Done -> SonaraEmptyState(
            title = "NO MUSIC FOUND",
            body = "SONARA found no music files on this device. Add music to the device, then rescan.",
            actionLabel = "RESCAN",
            onAction = onRescan,
            modifier = modifier,
        )
    }
}
