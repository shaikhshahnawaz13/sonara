package com.sonara.app.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraLayout
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.minTouchTarget

/**
 * Text-only category selector: condensed upper-case labels, the chosen one in ink with a 2dp forest rule under it,
 * the others in secondary text, and one hairline under the whole row. No pills, no fills.
 *
 * It only reports taps; what a category shows (and any slide between them) belongs to the screen. Labels past the
 * screen width scroll sideways instead of shrinking.
 */
@Composable
fun SonaraCategorySelector(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = SonaraLayout.screenMargin),
            horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s24),
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val text by animateColorAsState(
                    targetValue = if (selected) colors.foreground else colors.foregroundMuted,
                    animationSpec = SonaraTheme.motion.selectionColorSpec<Color>(),
                    label = "categoryText",
                )
                val rule by animateColorAsState(
                    targetValue = if (selected) colors.accent else colors.background,
                    animationSpec = SonaraTheme.motion.selectionColorSpec<Color>(),
                    label = "categoryRule",
                )
                Column(
                    // IntrinsicSize.Max gives the column the label's width, so the rule below can fill it even
                    // though the scrolling row offers unbounded width.
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .minTouchTarget()
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(modifier = Modifier.height(SonaraSpacing.s12))
                    Text(text = label, style = SonaraTheme.typography.button, color = text, maxLines = 1)
                    Box(modifier = Modifier.height(SonaraSpacing.s8))
                    Box(modifier = Modifier.fillMaxWidth().height(SonaraBorders.rule).background(rule))
                }
            }
        }
        SonaraDivider()
    }
}
