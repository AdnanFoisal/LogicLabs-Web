package com.logiclabs.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * Cross-platform font byte store for bundled Inter & JetBrains Mono fonts.
 *
 * - On JVM Desktop: lazily reads `font/inter_semibold.ttf` and `font/jetbrains_mono_medium.ttf`
 *   from classpath resources on first access.
 * - On Web (WasmGC / JS): populated asynchronously via `initialize(...)` before `ComposeViewport`
 *   mounts so both Compose UI text and Skia Canvas text render with authentic hardware typography.
 */
object LogicLabsFontStore {
    private var preloadedInter: ByteArray? = null
    private var preloadedMono: ByteArray? = null

    private var cachedInterFamily: FontFamily? = null
    private var cachedMonoFamily: FontFamily? = null

    fun initialize(interBytes: ByteArray?, monoBytes: ByteArray?) {
        preloadedInter = interBytes
        preloadedMono = monoBytes
        cachedInterFamily = null
        cachedMonoFamily = null
    }

    fun interBytes(): ByteArray? = preloadedInter ?: loadBundledFontBytes("font/inter_semibold.ttf")

    fun monoBytes(): ByteArray? = preloadedMono ?: loadBundledFontBytes("font/jetbrains_mono_medium.ttf")

    fun interFamily(): FontFamily {
        cachedInterFamily?.let { return it }
        return buildFontFamily("Inter", interBytes(), FontFamily.SansSerif).also {
            cachedInterFamily = it
        }
    }

    fun monoFamily(): FontFamily {
        cachedMonoFamily?.let { return it }
        return buildFontFamily("JetBrainsMono", monoBytes(), FontFamily.Monospace).also {
            cachedMonoFamily = it
        }
    }
}

internal expect fun loadBundledFontBytes(resourcePath: String): ByteArray?

internal expect fun buildFontFamily(identity: String, data: ByteArray?, fallback: FontFamily): FontFamily
