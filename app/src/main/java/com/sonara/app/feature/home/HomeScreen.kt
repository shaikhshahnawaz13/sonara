package com.sonara.app.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sonara.app.core.design.components.SonaraIconButton
import com.sonara.app.core.design.components.SonaraPlaceholderBody
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.core.design.components.SonaraWordmark

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(
            title = { SonaraWordmark() },
            actions = {
                SonaraIconButton(Icons.Filled.Search, "Search", onClick = onOpenSearch)
                SonaraIconButton(Icons.Filled.Settings, "Settings", onClick = onOpenSettings)
            },
        )
        SonaraPlaceholderBody(
            milestone = "M3",
            summary = "Home will show your real library: track, album, artist and playlist counts, " +
                "Continue Listening and Recently Added. No sample data will ever be shown here.",
        )
    }
}
