package com.sonara.app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.audio.EqualizerLogic
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.components.SonaraListRow
import com.sonara.app.core.design.components.SonaraSectionHeader
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.core.navigation.LocalSonaraNavigator

/**
 * Settings. M6.1 adds the Audio section with its Equalizer entry; the other sections (playback, library,
 * appearance, about) arrive with M6.4 and are not shown until they have real controls.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val navigator = LocalSonaraNavigator.current
    val equalizer by LocalAppContainer.current.audioEffects.state.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(title = "SETTINGS", onBack = onBack)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SonaraSectionHeader(title = "AUDIO", modifier = Modifier.padding(horizontal = SonaraSpacing.s16))
            SonaraListRow(
                title = "Equalizer",
                subtitle = EqualizerLogic.statusText(equalizer),
                onClick = navigator::openEqualizer,
            )
        }
    }
}
