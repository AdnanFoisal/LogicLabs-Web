package com.logiclabs.feature.tools.feedback

/**
 * Desktop feedback bridge replacing Android's Vibrator/View haptics.
 * Routes tactile events to synthesized [ConsoleSfx] clicks and relay thuds when sound is enabled.
 */
object Haptics {

    @kotlin.concurrent.Volatile
    var enabled: Boolean = true

    @kotlin.concurrent.Volatile
    var sfx: ConsoleSfx? = null

    fun tick() {
        if (!enabled) return
        sfx?.tick()
    }

    fun thud() {
        if (!enabled) return
        sfx?.pop()
    }

    fun success() {
        if (!enabled) return
        sfx?.click()
    }

    fun reject() {
        if (!enabled) return
        sfx?.pop()
    }
}
