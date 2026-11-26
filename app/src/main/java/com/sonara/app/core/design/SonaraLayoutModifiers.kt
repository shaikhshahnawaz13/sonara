package com.sonara.app.core.design

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

/** The side margins every screen uses ([SonaraLayout.screenMargin]). Apply once, to the screen's content column. */
fun Modifier.screenMargin(): Modifier = padding(horizontal = SonaraLayout.screenMargin)

/** At least a 48dp touch target in both directions; the visible content may be smaller. */
fun Modifier.minTouchTarget(): Modifier =
    defaultMinSize(minWidth = SonaraSizes.minTouchTarget, minHeight = SonaraSizes.minTouchTarget)
