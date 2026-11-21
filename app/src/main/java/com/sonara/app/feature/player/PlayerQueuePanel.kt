package com.sonara.app.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.SonaraIconButton
import com.sonara.app.core.design.components.SonaraIcons
import com.sonara.app.playback.QueueEntry

/**
 * The play queue, over the Full Player. Tap a song to play it, tap the cross to take it out of the queue.
 * It slides up from the bottom; it is a panel of the player, not a screen, so Back closes it first.
 */
@Composable
internal fun PlayerQueuePanel(
    visible: Boolean,
    queue: List<QueueEntry>,
    currentIndex: Int,
    onClose: () -> Unit,
    onJump: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = SonaraTheme.motion
    val colors = SonaraTheme.colors
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(animationSpec = motion.queuePanelSpec<IntOffset>()) { it },
        exit = slideOutVertically(animationSpec = motion.queuePanelSpec<IntOffset>()) { it },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                // Swallow touches so nothing behind the panel reacts.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = SonaraSpacing.s16, end = SonaraSpacing.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "QUEUE · ${queue.size}",
                    style = SonaraTheme.typography.section,
                    color = colors.foreground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SonaraIconButton(SonaraIcons.Close, "Close queue", onClick = onClose)
            }
            Box(Modifier.fillMaxWidth().height(SonaraBorders.standard).background(colors.borderMuted))
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 2).coerceAtLeast(0))
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(count = queue.size, key = { it }, contentType = { "entry" }) { index ->
                    QueueRow(
                        index = index,
                        entry = queue[index],
                        isCurrent = index == currentIndex,
                        onClick = { onJump(index) },
                        onRemove = { onRemove(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    index: Int,
    entry: QueueEntry,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .background(if (isCurrent) colors.surfaceRaised else Color.Transparent)
            .drawBehind {
                if (isCurrent) drawRect(colors.accent, size = Size(3.dp.toPx(), size.height))
            }
            .semantics { selected = isCurrent }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = SonaraSpacing.s16, end = SonaraSpacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
    ) {
        Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = (index + 1).toString(),
                style = typography.metadata,
                color = if (isCurrent) colors.accent else colors.foregroundMuted,
                maxLines = 1,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = typography.trackTitle,
                color = if (isCurrent) colors.accent else colors.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.artist,
                style = typography.metadata,
                color = colors.foregroundMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SonaraIconButton(SonaraIcons.Close, "Remove ${entry.title} from queue", onClick = onRemove)
    }
}
