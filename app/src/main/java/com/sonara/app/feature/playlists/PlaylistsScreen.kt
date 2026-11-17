package com.sonara.app.feature.playlists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sonara.app.core.design.components.SonaraPlaceholderBody
import com.sonara.app.core.design.components.SonaraTopBar

@Composable
fun PlaylistsScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(title = "PLAYLISTS")
        SonaraPlaceholderBody(
            milestone = "M4",
            summary = "Playlists you create will be stored on this device and survive app restarts.",
        )
    }
}
