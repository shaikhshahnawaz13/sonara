package com.sonara.app.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sonara.app.core.design.components.SonaraIconButton
import com.sonara.app.core.design.components.SonaraPlaceholderBody
import com.sonara.app.core.design.components.SonaraTopBar

@Composable
fun LibraryScreen(onOpenSearch: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(
            title = "LIBRARY",
            actions = {
                SonaraIconButton(Icons.Filled.Search, "Search", onClick = onOpenSearch)
            },
        )
        SonaraPlaceholderBody(
            milestone = "M3",
            summary = "Library will list Songs, Albums, Artists and Genres discovered on this device.",
        )
        // Temporary M2 verification of real discovery and playback; replaced by the M3 and M5 screens.
        PlaybackCheckPanel(modifier = Modifier.weight(1f))
    }
}
