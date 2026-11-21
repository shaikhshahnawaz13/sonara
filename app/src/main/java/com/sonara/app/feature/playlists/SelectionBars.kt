package com.sonara.app.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/** "3 SELECTED" on the left (always present, also at 0) and the bar's actions on the right. */
@Composable
internal fun SelectionCountRow(
    count: Int,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = SonaraSpacing.s16, end = SonaraSpacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$count SELECTED",
            style = SonaraTheme.typography.microLabel,
            color = if (count > 0) SonaraTheme.colors.accent else SonaraTheme.colors.foregroundMuted,
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            maxLines = 1,
        )
        actions()
    }
}

/** One-pixel rule between a bar and the list. */
@Composable
internal fun BarDivider() {
    Box(Modifier.fillMaxWidth().height(SonaraBorders.standard).background(SonaraTheme.colors.borderMuted))
}
