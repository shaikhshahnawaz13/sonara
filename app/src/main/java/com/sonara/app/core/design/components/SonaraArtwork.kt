package com.sonara.app.core.design.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraTheme

/** Supplies artwork bitmaps to [SonaraArtwork]. Provided once at the app root; null means no artwork. */
fun interface ArtworkLoader {
    suspend fun load(trackId: Long, sizePx: Int): Bitmap?
}

val LocalArtworkLoader = compositionLocalOf { ArtworkLoader { _, _ -> null } }

/** Never request more than this per side; large headers are still sharp enough and memory stays bounded. */
private const val MAX_ARTWORK_PX = 512

/**
 * Square artwork of a track (embedded art or album art). Without artwork it shows an honest empty tile,
 * never a stock image. The bitmap is requested at the size it is drawn at.
 */
@Composable
fun SonaraArtwork(
    trackId: Long?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val loader = LocalArtworkLoader.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }.coerceIn(1, MAX_ARTWORK_PX)
    val bitmap by produceState<ImageBitmap?>(initialValue = null, trackId, sizePx) {
        value = null
        if (trackId != null) value = loader.load(trackId, sizePx)?.asImageBitmap()
    }
    val colors = SonaraTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .background(colors.surfaceRaised)
            .border(SonaraBorders.standard, colors.borderMuted, SonaraTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        val art = bitmap
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (size >= 96.dp) {
            Text("NO ART", style = SonaraTheme.typography.microLabel, color = colors.foregroundMuted)
        }
    }
}
