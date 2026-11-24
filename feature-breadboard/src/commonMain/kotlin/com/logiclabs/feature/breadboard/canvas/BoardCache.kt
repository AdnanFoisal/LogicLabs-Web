package com.logiclabs.feature.breadboard.canvas

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.logiclabs.core.designsystem.theme.BenchPalette
import com.logiclabs.core.designsystem.theme.BenchPaletteState
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect as SkiaRect
import org.jetbrains.skia.Surface
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Rasterizes the static half of the AD-200 faceplate (~5,000 vector holes, clip gradients,
 * phenolic slab, rails, and silkscreen legends) ONCE into a RAM-capped offscreen Skia [Image]
 * and replays it as a single blit per frame.
 *
 * Compared to replaying a Skia [org.jetbrains.skia.Picture] vector display list, rasterizing once
 * reduces static faceplate paint time from thousands of gradient draw ops per frame to a single
 * `drawImageRect` blit (< 0.3 ms even on CPU software rendering), eliminates zoom-bucket cache
 * thrashing (including under the 2.2x optical loupe), and caps off-heap native RAM to <= ~7 MB.
 */
class BoardCache {

    private var cachedImage: Image? = null
    private var keyDensity = 0f
    private var keyLeft = Float.NaN
    private var keyTop = Float.NaN
    private var keyRight = Float.NaN
    private var keyBottom = Float.NaN
    private var keyPalette: BenchPalette? = null

    var recordings: Int = 0
        private set

    /** Approximate native RGBA byte footprint of the cached static faceplate bitmap. */
    val cachedByteSize: Long
        get() = cachedImage?.let { it.width.toLong() * it.height.toLong() * 4L } ?: 0L

    fun drawStatic(
        scope: DrawScope,
        mapper: BreadboardGeometryMapper,
        density: Float,
        scale: Float
    ) {
        val left = mapper.boardLeft
        val top = mapper.boardTop
        val right = mapper.boardRight
        val bottom = mapper.boardBottom
        val palette = BenchPaletteState.value

        val stale = cachedImage == null ||
            density != keyDensity ||
            left != keyLeft ||
            top != keyTop ||
            right != keyRight ||
            bottom != keyBottom ||
            palette !== keyPalette

        if (stale) bakeBitmap(mapper, density, left, top, right, bottom, palette)

        val img = cachedImage ?: return
        val srcRect = SkiaRect.makeWH(img.width.toFloat(), img.height.toFloat())
        val dstRect = SkiaRect.makeLTRB(left, top, right, bottom)
        scope.drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawImageRect(
                img,
                srcRect,
                dstRect,
                SAMPLING_MODE,
                null,
                true
            )
        }
    }

    private fun bakeBitmap(
        mapper: BreadboardGeometryMapper,
        density: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        palette: BenchPalette
    ) {
        cachedImage?.close()
        cachedImage = null

        val boardW = (right - left).coerceAtLeast(1f)
        val boardH = (bottom - top).coerceAtLeast(1f)
        val maxEdge = max(boardW, boardH)
        // Supersample gently up to MAX_RASTER_EDGE (2048px) so silkscreen stays razor-sharp
        // both at fit-to-screen and inside the 2.2x loupe while capping RAM to <= ~7 MB.
        val rasterScale = min(MAX_RASTER_SCALE, MAX_RASTER_EDGE / maxEdge).coerceAtLeast(0.5f)
        val pixelW = ceil(boardW * rasterScale).toInt().coerceIn(1, MAX_RASTER_EDGE)
        val pixelH = ceil(boardH * rasterScale).toInt().coerceIn(1, MAX_RASTER_EDGE)

        val surface = Surface.makeRasterN32Premul(pixelW, pixelH)
        val img: Image = try {
            val skiaCanvas = surface.canvas
            skiaCanvas.clear(0x00000000)
            skiaCanvas.save()
            skiaCanvas.scale(rasterScale, rasterScale)
            skiaCanvas.translate(-left, -top)

            CanvasDrawScope().draw(
                density = Density(density),
                layoutDirection = LayoutDirection.Ltr,
                canvas = skiaCanvas.asComposeCanvas(),
                size = Size(right, bottom)
            ) {
                FaceplateRenderer.drawStatic(
                    scope = this,
                    mapper = mapper,
                    density = density,
                    zoomScale = max(1.25f, rasterScale),
                    viewport = mapper.boardBounds()
                )
            }
            skiaCanvas.restore()
            surface.makeImageSnapshot()
        } finally {
            surface.close()
        }

        cachedImage = img
        keyDensity = density
        keyLeft = left
        keyTop = top
        keyRight = right
        keyBottom = bottom
        keyPalette = palette
        recordings++
    }

    fun release() {
        cachedImage?.close()
        cachedImage = null
        keyLeft = Float.NaN
    }

    companion object {
        const val BUCKET_STEP = 0.25f
        const val MAX_RASTER_EDGE = 2048
        private const val MAX_RASTER_SCALE = 1.5f
        private val SAMPLING_MODE = FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR)

        fun bucketOf(scale: Float): Float =
            (kotlin.math.floor(scale / BUCKET_STEP) * BUCKET_STEP).coerceIn(BUCKET_STEP, 4.0f)
    }
}
