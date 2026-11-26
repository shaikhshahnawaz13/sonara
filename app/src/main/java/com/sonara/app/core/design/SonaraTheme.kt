package com.sonara.app.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.sonara.app.core.animation.LocalSonaraMotion
import com.sonara.app.core.animation.MotionLevel
import com.sonara.app.core.animation.SonaraMotion

/** Strength of the flat veil shown over a pressed row. */
private const val PRESS_VEIL_ALPHA = 0.10f

private val LocalSonaraColors = staticCompositionLocalOf { SonaraColors() }
private val LocalSonaraTypography = staticCompositionLocalOf { SonaraTypography() }
private val LocalSonaraShapes = staticCompositionLocalOf { SonaraShapes() }

/** Accessor for SONARA design tokens inside composables: `SonaraTheme.colors.accent`. */
object SonaraTheme {
    val colors: SonaraColors
        @Composable @ReadOnlyComposable get() = LocalSonaraColors.current

    val typography: SonaraTypography
        @Composable @ReadOnlyComposable get() = LocalSonaraTypography.current

    val shapes: SonaraShapes
        @Composable @ReadOnlyComposable get() = LocalSonaraShapes.current

    val motion: SonaraMotion
        @Composable @ReadOnlyComposable get() = LocalSonaraMotion.current
}

/**
 * SONARA V2 is a light, warm theme and does not follow the system dark mode: the ivory and ink palette is the identity.
 * Material 3 is mapped onto the tokens so stock components stay on-brand.
 */
@Composable
fun SonaraTheme(
    motionLevel: MotionLevel = MotionLevel.Full,
    accent: AccentChoice = AccentChoice.default,
    content: @Composable () -> Unit,
) {
    val motion = remember(motionLevel) { SonaraMotion(motionLevel) }
    // Tokens change only when the accent does, so they are built once per accent. Rebuilding them (and the
    // Material mapping below) on every recomposition of the root would hand every screen new instances to
    // compare for no reason.
    val colors = remember(accent) { SonaraColors().withAccent(accent) }
    val typography = remember { SonaraTypography() }
    val shapes = remember { SonaraShapes() }

    val materialColors = remember(colors) { lightColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        secondary = colors.accent,
        onSecondary = colors.onAccent,
        tertiary = colors.highlight,
        onTertiary = colors.onHighlight,
        background = colors.background,
        onBackground = colors.foreground,
        surface = colors.surface,
        onSurface = colors.foreground,
        surfaceVariant = colors.surfaceRaised,
        onSurfaceVariant = colors.foregroundMuted,
        outline = colors.outline,
        outlineVariant = colors.divider,
        error = colors.danger,
        onError = colors.background,
    ) }

    val materialTypography = remember(typography) { Typography(
        displayLarge = typography.display,
        displayMedium = typography.hero,
        headlineMedium = typography.screenTitle,
        titleLarge = typography.section,
        titleMedium = typography.trackTitle,
        bodyMedium = typography.body,
        labelLarge = typography.button,
        labelMedium = typography.metadata,
        labelSmall = typography.microLabel,
    ) }

    val materialShapes = remember(shapes) { Shapes(
        extraSmall = shapes.none,
        small = shapes.small,
        medium = shapes.medium,
        large = shapes.large,
        extraLarge = shapes.large,
    ) }

    val indication = remember(colors, motion.isOff) {
        SonaraPressIndication(veil = colors.foreground.copy(alpha = PRESS_VEIL_ALPHA), instant = motion.isOff)
    }

    CompositionLocalProvider(
        LocalSonaraColors provides colors,
        LocalSonaraTypography provides typography,
        LocalSonaraShapes provides shapes,
        LocalSonaraMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = materialTypography,
            shapes = materialShapes,
        ) {
            // Provided inside MaterialTheme, which installs its own ripple; this replaces it for the whole app.
            CompositionLocalProvider(LocalIndication provides indication) {
                Surface(
                    color = colors.background,
                    contentColor = colors.foreground,
                    content = content,
                )
            }
        }
    }
}
