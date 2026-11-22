package com.sonara.app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.SonaraTopBar

/** The open-source projects SONARA is built with and the licence each one carries. */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val colors = SonaraTheme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(title = "LICENCES", onBack = onBack)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsNote(OpenSourceNotices.ACKNOWLEDGEMENT)
            SettingsSectionHeading("BUILT WITH", "Open-source projects used in SONARA.")
            OpenSourceNotices.all.forEachIndexed { index, notice ->
                if (index > 0) SettingsDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SonaraSpacing.s16, vertical = SonaraSpacing.s12),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(SonaraSpacing.s4),
                ) {
                    Text(notice.name, style = SonaraTheme.typography.trackTitle, color = colors.foreground)
                    Text("${notice.author} · ${notice.licence}", style = SonaraTheme.typography.metadata, color = colors.accent)
                    Text(notice.usedFor, style = SonaraTheme.typography.metadata, color = colors.foregroundMuted)
                }
            }
            SettingsDivider()
            SettingsNote(OpenSourceNotices.LICENCE_NOTE)
        }
    }
}
