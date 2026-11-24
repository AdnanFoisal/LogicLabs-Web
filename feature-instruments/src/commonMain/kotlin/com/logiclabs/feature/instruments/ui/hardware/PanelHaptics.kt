package com.logiclabs.feature.instruments.ui.hardware

/**
 * Desktop audio-feedback hook replacing Android vibration haptics on the trainer console.
 * The desktop app registers its synthesized ConsoleSfx click/pop callbacks here at startup.
 */
object PanelHaptics {

    @kotlin.concurrent.Volatile
    var onTickSound: (() -> Unit)? = null

    @kotlin.concurrent.Volatile
    var onHeavySound: (() -> Unit)? = null

    fun tick() {
        runCatching { onTickSound?.invoke() }
    }

    fun heavy() {
        runCatching { onHeavySound?.invoke() }
    }
}
