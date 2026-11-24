package com.logiclabs.feature.tools.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * Synthesized procedural console sound effects (`click`, `pop`, `tick`).
 *
 * - JVM Desktop: non-blocking async `javax.sound.sampled.Clip` daemon queue.
 * - Web (WasmGC / JS): low-latency browser Web Audio API (`AudioContext`) unlocked on first gesture.
 */
expect class ConsoleSfx() {
    var muted: Boolean
    fun click()
    fun pop()
    fun tick()
    fun release()
}

val LocalConsoleSfx: ProvidableCompositionLocal<ConsoleSfx?> = compositionLocalOf { null }

@Composable
fun rememberConsoleSfx(enabled: Boolean = true): ConsoleSfx {
    val sfx = remember { ConsoleSfx() }
    sfx.muted = !enabled
    DisposableEffect(sfx) {
        onDispose { sfx.release() }
    }
    return sfx
}
