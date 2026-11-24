package com.logiclabs.app

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.logiclabs.core.designsystem.theme.BenchPaletteState
import com.logiclabs.core.designsystem.theme.CleanroomPalette
import com.logiclabs.core.designsystem.theme.ObsidianPalette
import com.logiclabs.feature.breadboard.canvas.BoardCache
import com.logiclabs.feature.breadboard.canvas.BreadboardGeometryMapper
import com.logiclabs.feature.breadboard.canvas.FaceplateRenderer
import com.logiclabs.feature.tools.feedback.ConsoleSfx
import org.jetbrains.skia.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the v1.1.3 performance architecture upgrades targeting 4 GB RAM Linux Mint / Windows desktops:
 * 1. Non-blocking off-thread ConsoleSfx click dispatch (< 0.2 ms/click on UI thread).
 * 2. RAM-capped rasterized offscreen BoardCache (zero re-bake across zoom/loupe, <= 12 MB native RAM,
 *    and significantly faster per-frame blit than replaying ~5,000 vector holes).
 */
class PerformanceArchitectureTest {

    @Test
    fun `ConsoleSfx startup and rapid clicks never block caller UI thread`() {
        val t0 = System.nanoTime()
        val sfx = ConsoleSfx()
        val initMs = (System.nanoTime() - t0) / 1_000_000.0

        val t1 = System.nanoTime()
        repeat(100) { idx ->
            when (idx % 3) {
                0 -> sfx.tick()
                1 -> sfx.click()
                else -> sfx.pop()
            }
        }
        val dispatchMs = (System.nanoTime() - t1) / 1_000_000.0
        sfx.release()

        println(
            "PERF_BENCHMARK ConsoleSfx: init=${"%.2f".format(initMs)}ms, " +
                "100_clicks_dispatch=${"%.2f".format(dispatchMs)}ms (${"%.4f".format(dispatchMs / 100.0)}ms/click)"
        )
        assertTrue("ConsoleSfx lazy init must not block UI thread (was ${initMs}ms)", initMs < 150.0)
        assertTrue("100 rapid clicks must dispatch non-blocking in < 25ms (was ${dispatchMs}ms)", dispatchMs < 25.0)
    }

    @Test
    fun `BoardCache rasterizes once within RAM cap and blits dramatically faster than vector redraw`() {
        val previousPalette = BenchPaletteState.value
        BenchPaletteState.value = ObsidianPalette
        val mapper = BreadboardGeometryMapper()
        val cache = BoardCache()
        val surface = Surface.makeRasterN32Premul(1024, 600)

        try {
            val drawScope = CanvasDrawScope()
            val composeCanvas = surface.canvas.asComposeCanvas()
            val canvasSize = Size(1024f, 600f)

            // 1. Measure baseline full static vector redraw (20 frames)
            val vectorStart = System.nanoTime()
            repeat(20) {
                drawScope.draw(Density(1f), LayoutDirection.Ltr, composeCanvas, canvasSize) {
                    FaceplateRenderer.drawStatic(
                        scope = this,
                        mapper = mapper,
                        density = 1f,
                        zoomScale = 1f,
                        viewport = mapper.boardBounds()
                    )
                }
            }
            val vectorAvgMs = (System.nanoTime() - vectorStart) / 20_000_000.0

            // 2. Warmup 1 raster bake
            drawScope.draw(Density(1f), LayoutDirection.Ltr, composeCanvas, canvasSize) {
                cache.drawStatic(this, mapper, 1f, 0.75f)
            }
            assertEquals("First draw should bake static bitmap once", 1, cache.recordings)

            val ramMb = cache.cachedByteSize / (1024.0 * 1024.0)
            assertTrue("Cached faceplate bitmap must be non-empty", cache.cachedByteSize > 100_000L)
            assertTrue("Cached faceplate bitmap must stay under 12 MB RAM cap (was ${"%.2f".format(ramMb)} MB)", ramMb <= 12.0)

            // 3. Verify zoom changes & 2.2x loupe magnification cause ZERO re-bakes
            val scales = floatArrayOf(0.5f, 0.75f, 1.0f, 1.5f, 2.2f, 3.0f, 0.85f)
            val blitStart = System.nanoTime()
            repeat(50) { i ->
                val s = scales[i % scales.size]
                drawScope.draw(Density(1f), LayoutDirection.Ltr, composeCanvas, canvasSize) {
                    cache.drawStatic(this, mapper, 1f, s)
                }
            }
            val blitAvgMs = (System.nanoTime() - blitStart) / 50_000_000.0

            assertEquals("Zooming and loupe magnification must not thrash BoardCache", 1, cache.recordings)

            // 4. Verify theme switch cleanly invalidates and re-bakes once
            BenchPaletteState.value = CleanroomPalette
            drawScope.draw(Density(1f), LayoutDirection.Ltr, composeCanvas, canvasSize) {
                cache.drawStatic(this, mapper, 1f, 1.0f)
            }
            assertEquals("Theme switch must invalidate and re-bake bitmap once", 2, cache.recordings)

            val speedup = if (blitAvgMs > 0.001) vectorAvgMs / blitAvgMs else 999.0
            println(
                "PERF_BENCHMARK BoardCache: vector_avg=${"%.2f".format(vectorAvgMs)}ms/frame, " +
                    "raster_blit_avg=${"%.2f".format(blitAvgMs)}ms/frame (${"%.1f".format(speedup)}x faster), " +
                    "native_ram=${"%.2f".format(ramMb)}MB"
            )
            assertTrue(
                "Rasterized BoardCache blit (${"%.2f".format(blitAvgMs)}ms) must be faster than vector redraw (${"%.2f".format(vectorAvgMs)}ms)",
                blitAvgMs < vectorAvgMs
            )
        } finally {
            cache.release()
            surface.close()
            BenchPaletteState.value = previousPalette
        }
    }
}
