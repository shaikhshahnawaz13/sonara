package com.sonara.app.core.design.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * SONARA's icon family. Every icon is drawn on the same 24 x 24 grid inside a 4..20 live area, so they
 * share one optical size and alignment. Utility icons are 2-unit strokes with butt caps and mitred
 * joins (square, never rounded); transport icons are solid shapes of the same height. All are black
 * and are tinted by the caller, so there is a single colour system.
 *
 * The official logo is not part of this set; it lives in the drawable `ic_logo_mark`.
 */
object SonaraIcons {

    private const val STROKE = 2f

    private fun icon(name: String, autoMirror: Boolean = false, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror,
        ).apply(block).build()

    private fun ImageVector.Builder.line(block: PathBuilder.() -> Unit) = path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        pathBuilder = block,
    )

    private fun ImageVector.Builder.solid(block: PathBuilder.() -> Unit) = path(
        fill = SolidColor(Color.Black),
        pathBuilder = block,
    )

    val Search: ImageVector by lazy {
        icon("Search") {
            line {
                moveTo(16.5f, 10.5f)
                arcTo(6f, 6f, 0f, true, true, 4.5f, 10.5f)
                arcTo(6f, 6f, 0f, true, true, 16.5f, 10.5f)
                close()
                moveTo(15f, 15f)
                lineTo(20f, 20f)
            }
        }
    }

    val Close: ImageVector by lazy {
        icon("Close") {
            line {
                moveTo(5f, 5f); lineTo(19f, 19f)
                moveTo(19f, 5f); lineTo(5f, 19f)
            }
        }
    }

    val Add: ImageVector by lazy {
        icon("Add") {
            line {
                moveTo(12f, 4f); lineTo(12f, 20f)
                moveTo(4f, 12f); lineTo(20f, 12f)
            }
        }
    }

    /** Mirrors automatically in right-to-left layouts. */
    val Back: ImageVector by lazy {
        icon("Back", autoMirror = true) {
            line {
                moveTo(20f, 12f); lineTo(4f, 12f)
                moveTo(11f, 5f); lineTo(4f, 12f); lineTo(11f, 19f)
            }
        }
    }

    /** Vertical overflow: three square dots. */
    val More: ImageVector by lazy {
        icon("More") {
            solid {
                moveTo(10.5f, 4f); horizontalLineToRelative(3f); verticalLineToRelative(3f); horizontalLineToRelative(-3f); close()
                moveTo(10.5f, 10.5f); horizontalLineToRelative(3f); verticalLineToRelative(3f); horizontalLineToRelative(-3f); close()
                moveTo(10.5f, 17f); horizontalLineToRelative(3f); verticalLineToRelative(3f); horizontalLineToRelative(-3f); close()
            }
        }
    }

    val Refresh: ImageVector by lazy {
        icon("Refresh") {
            line {
                moveTo(18.11f, 9.78f)
                arcTo(6.5f, 6.5f, 0f, true, true, 10.87f, 5.60f)
            }
            solid {
                moveTo(10.32f, 2.45f)
                lineTo(15.01f, 4.87f)
                lineTo(11.43f, 8.75f)
                close()
            }
        }
    }

    /** An eight-tooth square gear with a round hub. */
    val Settings: ImageVector by lazy {
        icon("Settings") {
            line {
            moveTo(10.50f, 5.98f)
            lineTo(10.51f, 3.53f)
            lineTo(13.49f, 3.53f)
            lineTo(13.50f, 5.98f)
            lineTo(15.19f, 6.69f)
            lineTo(16.93f, 4.96f)
            lineTo(19.04f, 7.07f)
            lineTo(17.31f, 8.81f)
            lineTo(18.02f, 10.50f)
            lineTo(20.47f, 10.51f)
            lineTo(20.47f, 13.49f)
            lineTo(18.02f, 13.50f)
            lineTo(17.31f, 15.19f)
            lineTo(19.04f, 16.93f)
            lineTo(16.93f, 19.04f)
            lineTo(15.19f, 17.31f)
            lineTo(13.50f, 18.02f)
            lineTo(13.49f, 20.47f)
            lineTo(10.51f, 20.47f)
            lineTo(10.50f, 18.02f)
            lineTo(8.81f, 17.31f)
            lineTo(7.07f, 19.04f)
            lineTo(4.96f, 16.93f)
            lineTo(6.69f, 15.19f)
            lineTo(5.98f, 13.50f)
            lineTo(3.53f, 13.49f)
            lineTo(3.53f, 10.51f)
            lineTo(5.98f, 10.50f)
            lineTo(6.69f, 8.81f)
            lineTo(4.96f, 7.07f)
            lineTo(7.07f, 4.96f)
            lineTo(8.81f, 6.69f)
            close()
                moveTo(14.5f, 12f)
                arcTo(2.5f, 2.5f, 0f, true, true, 9.5f, 12f)
                arcTo(2.5f, 2.5f, 0f, true, true, 14.5f, 12f)
                close()
            }
        }
    }

    val Play: ImageVector by lazy {
        icon("Play") {
            solid {
                moveTo(7f, 5f); lineTo(19f, 12f); lineTo(7f, 19f); close()
            }
        }
    }

    val Pause: ImageVector by lazy {
        icon("Pause") {
            solid {
                moveTo(6f, 5f); horizontalLineToRelative(4f); verticalLineToRelative(14f); horizontalLineToRelative(-4f); close()
                moveTo(14f, 5f); horizontalLineToRelative(4f); verticalLineToRelative(14f); horizontalLineToRelative(-4f); close()
            }
        }
    }

    val SkipNext: ImageVector by lazy {
        icon("SkipNext") {
            solid {
                moveTo(5f, 5f); lineTo(16f, 12f); lineTo(5f, 19f); close()
                moveTo(16.5f, 5f); horizontalLineToRelative(2.5f); verticalLineToRelative(14f); horizontalLineToRelative(-2.5f); close()
            }
        }
    }

    /** Reorder handle: three bars. */
    val Drag: ImageVector by lazy {
        icon("Drag") {
            line {
                moveTo(5f, 8f); lineTo(19f, 8f)
                moveTo(5f, 12f); lineTo(19f, 12f)
                moveTo(5f, 16f); lineTo(19f, 16f)
            }
        }
    }

    /** Mirror of [SkipNext]: bar on the left, triangle pointing left. */
    val SkipPrevious: ImageVector by lazy {
        icon("SkipPrevious") {
            solid {
                moveTo(19f, 5f); lineTo(8f, 12f); lineTo(19f, 19f); close()
                moveTo(7.5f, 5f); horizontalLineToRelative(-2.5f); verticalLineToRelative(14f); horizontalLineToRelative(2.5f); close()
            }
        }
    }

    /** Two crossing arrows. */
    val Shuffle: ImageVector by lazy {
        icon("Shuffle") {
            line {
                moveTo(3f, 7f); lineTo(8f, 7f); lineTo(16f, 17f); lineTo(20f, 17f)
                moveTo(3f, 17f); lineTo(8f, 17f); lineTo(10.5f, 13.8f)
                moveTo(13.5f, 10.2f); lineTo(16f, 7f); lineTo(20f, 7f)
                moveTo(17.5f, 4.5f); lineTo(20.5f, 7f); lineTo(17.5f, 9.5f)
                moveTo(17.5f, 14.5f); lineTo(20.5f, 17f); lineTo(17.5f, 19.5f)
            }
        }
    }

    /** A loop with an arrow head. */
    val Repeat: ImageVector by lazy {
        icon("Repeat") {
            line {
                moveTo(4f, 11f); lineTo(4f, 8f); lineTo(18f, 8f)
                moveTo(15f, 5f); lineTo(18.5f, 8f); lineTo(15f, 11f)
                moveTo(20f, 13f); lineTo(20f, 16f); lineTo(6f, 16f)
                moveTo(9f, 13f); lineTo(5.5f, 16f); lineTo(9f, 19f)
            }
        }
    }

    /** [Repeat] with a "1" in the middle. */
    val RepeatOne: ImageVector by lazy {
        icon("RepeatOne") {
            line {
                moveTo(4f, 11f); lineTo(4f, 8f); lineTo(18f, 8f)
                moveTo(15f, 5f); lineTo(18.5f, 8f); lineTo(15f, 11f)
                moveTo(20f, 13f); lineTo(20f, 16f); lineTo(6f, 16f)
                moveTo(9f, 13f); lineTo(5.5f, 16f); lineTo(9f, 19f)
                moveTo(11f, 10.5f); lineTo(12.5f, 9.5f); lineTo(12.5f, 14.5f)
            }
        }
    }

    /** A list: three bars with markers. */
    val Queue: ImageVector by lazy {
        icon("Queue") {
            line {
                moveTo(9f, 7f); lineTo(20f, 7f)
                moveTo(9f, 12f); lineTo(20f, 12f)
                moveTo(9f, 17f); lineTo(20f, 17f)
                moveTo(4f, 7f); lineTo(5.5f, 7f)
                moveTo(4f, 12f); lineTo(5.5f, 12f)
                moveTo(4f, 17f); lineTo(5.5f, 17f)
            }
        }
    }

    /** Collapse the player. */
    val ChevronDown: ImageVector by lazy {
        icon("ChevronDown") {
            line {
                moveTo(5f, 9f); lineTo(12f, 16f); lineTo(19f, 9f)
            }
        }
    }

    /** Favourite, outline. */
    val Heart: ImageVector by lazy {
        icon("Heart") {
            line {
                moveTo(12f, 20f); lineTo(4.5f, 12.5f)
                lineTo(4.5f, 8.5f); lineTo(7.5f, 5.5f); lineTo(10f, 5.5f); lineTo(12f, 7.5f)
                lineTo(14f, 5.5f); lineTo(16.5f, 5.5f); lineTo(19.5f, 8.5f)
                lineTo(19.5f, 12.5f); close()
            }
        }
    }

    /** Favourite, filled. */
    val HeartFilled: ImageVector by lazy {
        icon("HeartFilled") {
            solid {
                moveTo(12f, 20f); lineTo(4.5f, 12.5f)
                lineTo(4.5f, 8.5f); lineTo(7.5f, 5.5f); lineTo(10f, 5.5f); lineTo(12f, 7.5f)
                lineTo(14f, 5.5f); lineTo(16.5f, 5.5f); lineTo(19.5f, 8.5f)
                lineTo(19.5f, 12.5f); close()
            }
        }
    }
}
