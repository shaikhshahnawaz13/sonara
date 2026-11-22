package com.sonara.app.core.design.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.R
import com.sonara.app.core.animation.MotionTokens
import com.sonara.app.core.animation.StartupTimeline
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/**
 * The first moments of a cold start: the SONARA wordmark is wiped in, a lime rule draws under it, and the
 * panel leaves upward to show the app. The app is composed underneath from the first frame, so the reveal
 * hides no loading and delays nothing; a tap or Back plays the remainder four times faster.
 *
 * One clock drives everything and is read only while drawing, so the reveal recomposes nothing per frame.
 * The caller does not show it at all when motion is Off. It also should not show it again on rotation or
 * when the app returns from the background.
 */
@Composable
fun StartupReveal(onFinished: () -> Unit) {
    val colors = SonaraTheme.colors
    val motion = SonaraTheme.motion
    val finished by rememberUpdatedState(onFinished)
    val progress = remember { mutableFloatStateOf(0f) }
    var skipping by remember { mutableStateOf(false) }
    val speed by rememberUpdatedState(if (skipping) MotionTokens.STARTUP_SKIP_SPEEDUP else 1f)
    val totalMs = motion.startupMs

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (progress.floatValue < 1f) {
            val now = withFrameNanos { it }
            progress.floatValue = StartupTimeline.advance(progress.floatValue, now - last, totalMs, speed)
            last = now
        }
        finished()
    }
    BackHandler { skipping = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = -size.height * StartupTimeline.exit(progress.floatValue) }
            .background(colors.background)
            // The first touch skips; nothing underneath reacts to it while the panel is up.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    skipping = true
                }
            }
            .semantics { contentDescription = "SONARA" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_logo_mark),
                contentDescription = null,
                modifier = Modifier.height(56.dp),
            )
            Spacer(Modifier.height(SonaraSpacing.s24))
            // The word and its rule share one width, so the rule is exactly as long as the word.
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                Text(
                    text = "SONARA",
                    style = SonaraTheme.typography.hero.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp),
                    color = colors.foreground,
                    maxLines = 1,
                    modifier = Modifier.drawWithContent {
                        clipRect(right = size.width * StartupTimeline.wipe(progress.floatValue)) {
                            this@drawWithContent.drawContent()
                        }
                    },
                )
                Spacer(Modifier.height(SonaraSpacing.s8))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SonaraBorders.emphasis)
                        .drawBehind {
                            drawRect(
                                color = colors.accent,
                                size = Size(size.width * StartupTimeline.rule(progress.floatValue), size.height),
                            )
                        },
                )
            }
        }
    }
}
