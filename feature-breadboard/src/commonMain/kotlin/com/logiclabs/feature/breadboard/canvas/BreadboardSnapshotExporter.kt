package com.logiclabs.feature.breadboard.canvas

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import kotlin.math.ceil

/**
 * Renders a crisp, High-DPI PNG snapshot of the entire wired AD-200 breadboard
 * (static faceplate, live terminal LEDs, placed 74xx DIP ICs, and jumper wires)
 * using an offscreen Skia raster [Surface].
 *
 * Works identically across Web (`wasmJs` + `js`) and Desktop (`jvm`) with zero
 * WebGL framebuffer capture issues.
 */
object BreadboardSnapshotExporter {

    fun renderPngBytes(
        circuit: BreadboardCircuit,
        renderScale: Float = 1.25f
    ): ByteArray? = runCatching {
        val mapper = BreadboardGeometryMapper()
        val left = mapper.boardLeft
        val top = mapper.boardTop
        val right = mapper.boardRight
        val bottom = mapper.boardBottom
        val boardW = (right - left).coerceAtLeast(1f)
        val boardH = (bottom - top).coerceAtLeast(1f)

        val pad = 28f
        val totalW = boardW + pad * 2f
        val totalH = boardH + pad * 2f
        val pixelW = ceil(totalW * renderScale).toInt().coerceIn(320, 2600)
        val pixelH = ceil(totalH * renderScale).toInt().coerceIn(200, 1800)
        val effScale = pixelW.toFloat() / totalW

        val surface = Surface.makeRasterN32Premul(pixelW, pixelH)
        try {
            val skiaCanvas = surface.canvas
            skiaCanvas.clear(0xFF090D14.toInt())
            skiaCanvas.save()
            skiaCanvas.scale(effScale, effScale)
            skiaCanvas.translate(pad - left, pad - top)

            val bounds = mapper.boardBounds()
            CanvasDrawScope().draw(
                density = Density(1f),
                layoutDirection = LayoutDirection.Ltr,
                canvas = skiaCanvas.asComposeCanvas(),
                size = Size(right + pad, bottom + pad)
            ) {
                FaceplateRenderer.drawStatic(
                    scope = this,
                    mapper = mapper,
                    density = 1f,
                    zoomScale = effScale.coerceAtLeast(1.25f),
                    viewport = bounds
                )
                FaceplateRenderer.drawDynamic(
                    scope = this,
                    mapper = mapper,
                    circuit = circuit,
                    density = 1f,
                    zoomScale = effScale.coerceAtLeast(1.25f),
                    highlightSockets = emptySet(),
                    viewport = bounds
                )
                for (chip in circuit.placedChips) {
                    DipPainter.draw(
                        scope = this,
                        mapper = mapper,
                        chip = chip,
                        density = 1f,
                        zoomScale = effScale.coerceAtLeast(1.25f),
                        isBurned = chip.placedIc.id in circuit.burnedChipIds ||
                            (circuit.isReversePolarityBurned && circuit.burnedChipId == chip.placedIc.id),
                        isSelected = false,
                        explosionTimestamp = 0L
                    )
                }
                WirePainter.draw(this, mapper, circuit, null, bounds)
            }
            skiaCanvas.restore()

            val img = surface.makeImageSnapshot()
            try {
                img.encodeToData(EncodedImageFormat.PNG)?.bytes
            } finally {
                img.close()
            }
        } finally {
            surface.close()
        }
    }.getOrNull()
}
