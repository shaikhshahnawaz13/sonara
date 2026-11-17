package com.sonara.app.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sonara.app.core.design.components.SonaraPlaceholderBody
import com.sonara.app.core.design.components.SonaraTopBar

@Composable
fun SearchScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(title = "SEARCH", onBack = onBack)
        SonaraPlaceholderBody(
            milestone = "M3",
            summary = "Search will look through songs, albums, artists, playlists and folders on this device only.",
        )
    }
}
