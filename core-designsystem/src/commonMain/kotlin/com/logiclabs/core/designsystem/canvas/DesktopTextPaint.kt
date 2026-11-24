package com.logiclabs.core.designsystem.canvas

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.logiclabs.core.designsystem.theme.LogicLabsFontStore
import org.jetbrains.skia.Canvas as SkiaCanvas
import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Typeface

enum class TextAlignMode { LEFT, CENTER, RIGHT }

/**
 * Shared Skia typeface loader backed by [LogicLabsFontStore] so Desktop (JVM) and Web (Skiko Wasm/JS)
 * render crisp, identical breadboard silkscreen, DIP legends, oscilloscope grids, and schematics.
 */
object DesktopTypefaces {

    private var cachedMono: Typeface? = null
    private var cachedSans: Typeface? = null

    val mono: Typeface?
        get() {
            cachedMono?.let { return it }
            val loaded = loadFromBytes(LogicLabsFontStore.monoBytes())
                ?: runCatching { FontMgr.default.legacyMakeTypeface("monospace", FontStyle.NORMAL) }.getOrNull()
            if (loaded != null) cachedMono = loaded
            return loaded
        }

    val sans: Typeface?
        get() {
            cachedSans?.let { return it }
            val loaded = loadFromBytes(LogicLabsFontStore.sansOrFallbackBytes())
                ?: runCatching { FontMgr.default.legacyMakeTypeface("sans-serif", FontStyle.NORMAL) }.getOrNull()
            if (loaded != null) cachedSans = loaded
            return loaded
        }

    fun resetCache() {
        cachedMono = null
        cachedSans = null
    }

    private fun LogicLabsFontStore.sansOrFallbackBytes(): ByteArray? = interBytes() ?: monoBytes()

    private fun loadFromBytes(bytes: ByteArray?): Typeface? {
        if (bytes == null || bytes.isEmpty()) return null
        return runCatching {
            FontMgr.default.makeFromData(Data.makeFromBytes(bytes))
        }.getOrNull()
    }
}

/**
 * Allocation-light Skia text painter shared across Desktop and Web Skiko renderers.
 */
class DesktopTextPaint(
    color: Color = Color.White,
    textSize: Float = 12f,
    bold: Boolean = false,
    monospace: Boolean = true,
    var align: TextAlignMode = TextAlignMode.LEFT
) {
    private val skiaPaint = Paint().apply {
        isAntiAlias = true
        mode = PaintMode.FILL
        this.color = color.toArgb()
    }

    private var currentSize: Float = textSize
    private var font: Font = createFont(textSize, bold, monospace)

    var color: Int
        get() = skiaPaint.color
        set(value) {
            skiaPaint.color = value
        }

    var textSize: Float
        get() = currentSize
        set(value) {
            if (value != currentSize) {
                currentSize = value
                font.size = value
            }
        }

    fun setColor(c: Color) {
        skiaPaint.color = c.toArgb()
    }

    private fun createFont(size: Float, bold: Boolean, monospace: Boolean): Font {
        val tf = if (monospace) DesktopTypefaces.mono else DesktopTypefaces.sans
        return Font(tf, size).apply {
            isSubpixel = true
            isEmboldened = bold
        }
    }

    fun measureText(text: String): Float {
        if (text.isEmpty()) return 0f
        return font.measureTextWidth(text, skiaPaint)
    }

    fun ascent(): Float = font.metrics.ascent

    fun descent(): Float = font.metrics.descent

    fun lineHeight(): Float {
        val m = font.metrics
        return (m.descent - m.ascent + m.leading).coerceAtLeast(currentSize * 1.15f)
    }

    fun drawText(canvas: SkiaCanvas, text: String, x: Float, y: Float) {
        if (text.isEmpty()) return
        val drawX = when (align) {
            TextAlignMode.LEFT -> x
            TextAlignMode.CENTER -> x - measureText(text) * 0.5f
            TextAlignMode.RIGHT -> x - measureText(text)
        }
        canvas.drawString(text, drawX, y, font, skiaPaint)
    }
}

/** Drop-in extension mirroring Android's `Canvas.drawText(text, x, y, paint)`. */
fun SkiaCanvas.drawText(text: String, x: Float, y: Float, paint: DesktopTextPaint) {
    paint.drawText(this, text, x, y)
}
