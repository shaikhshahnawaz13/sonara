package com.sonara.app.core.design.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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

/** Size the Full Player asks for. One request size for every track, so it is decoded once and cached. */
const val PLAYER_ARTWORK_PX = 768

/** Artwork requested for tiles that fill their cell, whatever its exact width. */
private val FILL_REQUEST_SIZE = 192.dp

/** Sizes other tiles of the same track are drawn at, tried (largest first) as a stand-in on a cache miss. */
private val STAND_IN_SIZES = listOf(192.dp, 112.dp, 56.dp, 48.dp, 40.dp)

/** The bitmap size [SonaraArtwork] requests for a tile of [size]; callers that warm the cache must use the same. */
fun artworkRequestPx(size: Dp, density: Density): Int = with(density) { size.roundToPx() }.coerceIn(1, MAX_ARTWORK_PX)

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
    val sizePx = artworkRequestPx(size, LocalDensity.current)
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
    // Opening a detail from a card: the card's smaller picture of the same track is usually in memory already.
    // It stands in (same crop, same square) until the sharp one arrives, so the header is never an empty tile.
    val density = LocalDensity.current
    val standIn: ImageBitmap? = if (trackId != null && cached == null) {
        remember(loader, trackId, sizePx) {
            STAND_IN_SIZES.firstNotNullOfOrNull { dp ->
                val px = artworkRequestPx(dp, density)
                if (px == sizePx) null else loader.peek(trackId, px)
            }?.asImageBitmap()
        }
    } else {
        null
    }
    val bitmap by produceState(initialValue = cached ?: standIn, loader, trackId, sizePx) {
        value = cached ?: standIn
        if (cached == null && trackId != null) value = loader.load(trackId, sizePx)?.asImageBitmap() ?: standIn
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

/**
 * Where the two pictures sit while the player artwork changes track. The new picture arrives from the side the
 * queue moved towards (next: from the right, previous: from the left) while the old one leaves the other way;
 * they stay exactly one width apart, so the crop and geometry of both are untouched.
 */
object ArtworkSlide {
    /** +1 when the queue moved forward (or the position is unknown), -1 when it moved back. */
    fun direction(previousIndex: Int, newIndex: Int): Int = if (previousIndex >= 0 && newIndex >= 0 && newIndex < previousIndex) -1 else 1

    fun incomingOffset(progress: Float, direction: Int, widthPx: Float): Float =
        direction * widthPx * (1f - progress.coerceIn(0f, 1f))

    fun outgoingOffset(progress: Float, direction: Int, widthPx: Float): Float =
        -direction * widthPx * progress.coerceIn(0f, 1f)
}

/**
 * Artwork for the Full Player. It never shows an empty tile while a better picture is on the way: when the
 * large bitmap is not in memory yet, the small one the mini-player already shows stands in (same crop, so
 * nothing moves) until the large one arrives. A track change keeps the previous picture on screen until the
 * new one is ready, instead of flashing the empty tile, and then slides it out in the direction the queue
 * moved ([queueIndex] tells which way). Swapping a stand-in for the sharp version of the same track never
 * slides, and with motion Off the picture is replaced at once.
 */
@Composable
fun SonaraPlayerArtwork(
    trackId: Long?,
    modifier: Modifier = Modifier,
    queueIndex: Int = -1,
) {
    val loader = LocalArtworkLoader.current
    val miniPx = with(LocalDensity.current) { 40.dp.roundToPx() }.coerceIn(1, MAX_ARTWORK_PX)
    val ready: ImageBitmap? = if (trackId != null) {
        remember(loader, trackId) {
            (loader.peek(trackId, PLAYER_ARTWORK_PX) ?: loader.peek(trackId, miniPx))?.asImageBitmap()
        }
    } else {
        null
    }
    // The last picture shown, so a track change replaces it only when the next one is available.
    var shown by remember { mutableStateOf<ImageBitmap?>(null) }
    // Which track the picture in [shown] belongs to.
    var shownTrack by remember { mutableStateOf<Long?>(null) }
    var settledFor by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(loader, trackId) {
        if (trackId == null) {
            shown = null
            shownTrack = null
            settledFor = null
            return@LaunchedEffect
        }
        if (ready != null) {
            shown = ready
            shownTrack = trackId
        }
        val large = loader.peek(trackId, PLAYER_ARTWORK_PX)?.asImageBitmap()
            ?: loader.load(trackId, PLAYER_ARTWORK_PX)?.asImageBitmap()
        if (large != null) {
            shown = large
            shownTrack = trackId
        } else if (ready == null) {
            shown = null
            shownTrack = null
        }
        settledFor = trackId
    }
    val colors = SonaraTheme.colors
    val motion = SonaraTheme.motion
    val art = shown ?: ready
    val artTrack = if (shown != null) shownTrack else trackId

    // What is drawn, and what is leaving. Kept apart from [art] so a track change can slide.
    var displayed by remember { mutableStateOf(art) }
    var displayedTrack by remember { mutableStateOf(artTrack) }
    var displayedIndex by remember { mutableIntStateOf(queueIndex) }
    var outgoing by remember { mutableStateOf<ImageBitmap?>(null) }
    var direction by remember { mutableIntStateOf(1) }
    val slide = remember { Animatable(1f) }
    var slideToken by remember { mutableIntStateOf(0) }
    // Bookkeeping only (it never suspends), so a sharper version of the same picture arriving mid-slide
    // changes what is drawn without touching the slide itself.
    LaunchedEffect(art, artTrack) {
        if (art === displayed && artTrack == displayedTrack) return@LaunchedEffect
        val old = displayed
        val trackChanged = artTrack != displayedTrack
        val previousIndex = displayedIndex
        displayed = art
        displayedTrack = artTrack
        displayedIndex = queueIndex
        if (trackChanged) {
            if (old != null && art != null && !motion.isOff) {
                direction = ArtworkSlide.direction(previousIndex, queueIndex)
                outgoing = old
                slideToken++
            } else {
                outgoing = null
            }
        }
    }
    LaunchedEffect(slideToken) {
        if (slideToken == 0) return@LaunchedEffect
        slide.snapTo(0f)
        slide.animateTo(1f, motion.artworkChangeSpec())
        outgoing = null
    }

    Box(
        modifier = modifier
            .background(colors.surfaceRaised)
            .border(SonaraBorders.standard, colors.borderMuted, SonaraTheme.shapes.small)
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        val shownNow = displayed
        val leaving = outgoing
        if (shownNow != null) {
            Image(
                bitmap = shownNow,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = if (leaving != null) ArtworkSlide.incomingOffset(slide.value, direction, size.width) else 0f
                    },
            )
            if (leaving != null) {
                Image(
                    bitmap = leaving,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = ArtworkSlide.outgoingOffset(slide.value, direction, size.width) },
                )
            }
        } else if (trackId == null || settledFor == trackId) {
            Text("NO ART", style = SonaraTheme.typography.microLabel, color = colors.foregroundMuted)
        }
    }
}
