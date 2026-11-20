package com.sonara.app.core.design.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
interface ArtworkLoader {
    /** The bitmap if it is already in memory. Never blocks, never decodes. */
    fun peek(trackId: Long, sizePx: Int): Bitmap? = null

    suspend fun load(trackId: Long, sizePx: Int): Bitmap?
}

val LocalArtworkLoader = compositionLocalOf<ArtworkLoader> {
    object : ArtworkLoader {
        override suspend fun load(trackId: Long, sizePx: Int): Bitmap? = null
    }
}

/** Never request more than this per side; large headers are still sharp enough and memory stays bounded. */
private const val MAX_ARTWORK_PX = 512

/** Artwork requested for tiles that fill their cell, whatever its exact width. */
private val FILL_REQUEST_SIZE = 192.dp

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
    val sizePx = with(LocalDensity.current) { size.roundToPx() }.coerceIn(1, MAX_ARTWORK_PX)
    ArtworkTile(trackId, sizePx, modifier.size(size), showLabel = size >= 96.dp)
}

/**
 * Square artwork as wide as its parent. It needs no measuring pass of its own (no BoxWithConstraints),
 * which keeps grid cells cheap; the bitmap is requested at one fixed size so every cell shares cache entries.
 */
@Composable
fun SonaraArtworkFill(
    trackId: Long?,
    modifier: Modifier = Modifier,
) {
    val sizePx = with(LocalDensity.current) { FILL_REQUEST_SIZE.roundToPx() }.coerceIn(1, MAX_ARTWORK_PX)
    ArtworkTile(trackId, sizePx, modifier.fillMaxWidth().aspectRatio(1f), showLabel = true)
}

@Composable
private fun ArtworkTile(
    trackId: Long?,
    sizePx: Int,
    modifier: Modifier,
    showLabel: Boolean,
) {
    val loader = LocalArtworkLoader.current
    // A cache hit is available in the very first frame; only a miss goes through the asynchronous load.
    val cached: ImageBitmap? = if (trackId != null) {
        remember(loader, trackId, sizePx) { loader.peek(trackId, sizePx)?.asImageBitmap() }
    } else {
        null
    }
    val bitmap by produceState(initialValue = cached, loader, trackId, sizePx) {
        value = cached
        if (cached == null && trackId != null) value = loader.load(trackId, sizePx)?.asImageBitmap()
    }
    val colors = SonaraTheme.colors
    Box(
        modifier = modifier
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
        } else if (showLabel) {
            Text("NO ART", style = SonaraTheme.typography.microLabel, color = colors.foregroundMuted)
        }
    }
}
