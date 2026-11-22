package com.sonara.app.feature.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.audio.AudioEffectsManager
import com.sonara.app.audio.EqualizerLogic
import com.sonara.app.audio.EqualizerProfile
import com.sonara.app.audio.EqualizerSupport
import com.sonara.app.audio.EqualizerUiState
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.LocalSonaraHaptics
import com.sonara.app.core.design.components.SonaraButton
import com.sonara.app.core.design.components.SonaraEmptyState
import com.sonara.app.core.design.components.SonaraLoadingState
import com.sonara.app.core.design.components.SonaraToggle
import com.sonara.app.core.design.components.SonaraTopBar

/**
 * The equalizer: a master switch, the presets this device offers and one slider per band the device reports.
 * Everything shown comes from the device and from the saved choice; when the device has no equalizer the screen
 * says so and shows no controls.
 */
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val manager = LocalAppContainer.current.audioEffects
    val state by manager.state.collectAsStateWithLifecycle()
    LaunchedEffect(manager) { manager.ensureProbed() }

    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(title = "EQUALIZER", onBack = onBack)
        val support = state.support
        when {
            !state.loaded || support is EqualizerSupport.Checking -> SonaraLoadingState()
            support is EqualizerSupport.Unsupported -> SonaraEmptyState("EQUALIZER NOT AVAILABLE", support.reason)
            support is EqualizerSupport.Supported -> EqualizerBody(state, support.profile, manager)
        }
    }
}

@Composable
private fun EqualizerBody(state: EqualizerUiState, profile: EqualizerProfile, manager: AudioEffectsManager) {
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SonaraSpacing.s16, vertical = SonaraSpacing.s8),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s16),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("EQUALIZER", style = typography.section, color = colors.foreground)
                Text(
                    text = EqualizerLogic.statusText(state),
                    style = typography.microLabel,
                    color = if (state.enabled) colors.accent else colors.foregroundMuted,
                )
            }
            SonaraToggle(checked = state.enabled, onCheckedChange = manager::setEnabled)
        }

        Column(verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s8)) {
            Text("PRESET", style = typography.microLabel, color = colors.foregroundMuted)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s8),
            ) {
                if (state.presetName == null) PresetChip(label = "CUSTOM", selected = true, onClick = null)
                profile.presets.forEach { preset ->
                    PresetChip(
                        label = preset.name.uppercase(),
                        selected = state.presetName == preset.name,
                        onClick = { manager.selectPreset(preset.name) },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s4),
        ) {
            profile.bandCentresMilliHz.forEachIndexed { band, centre ->
                val level = state.levelsMb.getOrElse(band) { 0 }
                BandSlider(
                    frequency = EqualizerLogic.formatFrequency(centre),
                    gain = EqualizerLogic.formatGain(level),
                    levelMb = level,
                    minMb = profile.minLevelMb,
                    maxMb = profile.maxLevelMb,
                    active = state.enabled,
                    onLevel = { manager.setBandLevel(band, it) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(EqualizerLogic.formatGain(profile.maxLevelMb), style = typography.microLabel, color = colors.foregroundMuted)
            Text(EqualizerLogic.formatGain(profile.minLevelMb), style = typography.microLabel, color = colors.foregroundMuted)
        }

        SonaraButton("RESET TO FLAT", onClick = manager::resetFlat, modifier = Modifier.fillMaxWidth())
    }
}

/** A preset. [onClick] null makes it a plain indicator (the CUSTOM chip is a state, not a choice). */
@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: (() -> Unit)?) {
    val colors = SonaraTheme.colors
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .background(if (selected) colors.accent else colors.surfaceRaised)
            .border(SonaraBorders.standard, if (selected) colors.accent else colors.border)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = SonaraSpacing.s12),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = SonaraTheme.typography.microLabel,
            color = if (selected) colors.onAccent else colors.foreground,
            maxLines = 1,
        )
    }
}

/**
 * One band: gain on top, a vertical slider (top = loudest) with a mark at 0 dB, frequency below. The thumb and
 * bar are drawn from [levelMb], which the manager owns; dragging asks for a level and the screen shows whatever the
 * manager then reports. While the equalizer is off the slider still works (the curve is saved) but is drawn muted.
 */
@Composable
private fun BandSlider(
    frequency: String,
    gain: String,
    levelMb: Int,
    minMb: Int,
    maxMb: Int,
    active: Boolean,
    onLevel: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    val hapticsNow by rememberUpdatedState(LocalSonaraHaptics.current)
    val currentLevel by rememberUpdatedState(levelMb)
    val onLevelNow by rememberUpdatedState(onLevel)
    // A light tick each time the band moves to a new step, so dragging can be felt without looking.
    val request: (Int) -> Unit = remember {
        { level ->
            if (level != currentLevel) hapticsNow.tick()
            onLevelNow(level)
        }
    }
    val barColor = if (active) colors.accent else colors.foregroundMuted
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s8)) {
        Text(
            text = gain,
            style = typography.microLabel,
            color = colors.foreground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
        Box(
            modifier = Modifier
                .width(SLIDER_WIDTH)
                .height(SLIDER_HEIGHT)
                .semantics {
                    contentDescription = "$frequency band"
                    stateDescription = gain
                    progressBarRangeInfo = ProgressBarRangeInfo(levelMb.toFloat(), minMb.toFloat()..maxMb.toFloat())
                    setProgress { target ->
                        request(EqualizerLogic.snap(target.toInt(), minMb, maxMb))
                        true
                    }
                }
                .pointerInput(minMb, maxMb) {
                    detectTapGestures { offset ->
                        request(EqualizerLogic.levelFromFraction(1f - offset.y / size.height, minMb, maxMb))
                    }
                }
                .pointerInput(minMb, maxMb) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            request(EqualizerLogic.levelFromFraction(1f - offset.y / size.height, minMb, maxMb))
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            request(EqualizerLogic.levelFromFraction(1f - change.position.y / size.height, minMb, maxMb))
                        },
                    )
                }
                .drawBehind {
                    val trackWidth = 2.dp.toPx()
                    val barWidth = 6.dp.toPx()
                    val centreX = size.width / 2f
                    val zeroY = size.height * (1f - EqualizerLogic.fractionOf(0, minMb, maxMb))
                    val thumbY = size.height * (1f - EqualizerLogic.fractionOf(levelMb, minMb, maxMb))
                    drawRect(colors.borderMuted, Offset(centreX - trackWidth / 2f, 0f), Size(trackWidth, size.height))
                    drawRect(colors.border, Offset(0f, zeroY - 1.dp.toPx()), Size(size.width, 2.dp.toPx()))
                    drawRect(
                        barColor,
                        Offset(centreX - barWidth / 2f, minOf(zeroY, thumbY)),
                        Size(barWidth, kotlin.math.abs(zeroY - thumbY)),
                    )
                    val thumbW = 30.dp.toPx()
                    val thumbH = 12.dp.toPx()
                    drawRect(barColor, Offset(centreX - thumbW / 2f, thumbY - thumbH / 2f), Size(thumbW, thumbH))
                },
        )
        Text(
            text = frequency,
            style = typography.microLabel,
            color = colors.foregroundMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

private val SLIDER_WIDTH = 44.dp
private val SLIDER_HEIGHT = 240.dp
