package com.sonara.app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sonara.app.core.design.components.SonaraPlaceholderBody
import com.sonara.app.core.design.components.SonaraTopBar

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(title = "SETTINGS", onBack = onBack)
        SonaraPlaceholderBody(
            milestone = "M6",
            summary = "Settings will cover appearance, library, playback, audio and storage. " +
                "The persistence layer behind them already exists; the controls arrive with the feature.",
        )
    }
}
