package com.logiclabs.feature.tools.feedback

internal expect fun webAudioPlayKind(kind: Int)
internal expect fun webAudioRelease()

/**
 * Web Audio API procedural console sound effects shared between WasmGC and JS builds.
 * Synthesizes exact click/pop/tick PCM waveforms directly in the browser AudioContext.
 */
actual class ConsoleSfx actual constructor() {
    actual var muted: Boolean = false

    actual fun click() {
        if (!muted) webAudioPlayKind(1)
    }

    actual fun pop() {
        if (!muted) webAudioPlayKind(2)
    }

    actual fun tick() {
        if (!muted) webAudioPlayKind(3)
    }

    actual fun release() {
        webAudioRelease()
    }
}
