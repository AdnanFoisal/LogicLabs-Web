package com.logiclabs.feature.tools.feedback

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Synthesized console sound effects over standard Java Sound (`javax.sound.sampled.Clip`).
 *
 * Uses the exact same procedural PCM16 waveforms as Logic Labs Android (switch click, relay
 * power thud, and socket tick) with zero external audio assets and zero native dependencies.
 *
 * All audio device initialization and Clip playback runs asynchronously on a dedicated single
 * daemon thread with atomic event coalescing so PulseAudio/ALSA/WASAPI mixer locks can NEVER
 * stall the Compose UI thread during clicks, tab switches, or breadboard selection.
 */
actual class ConsoleSfx actual constructor() {

    @Volatile
    actual var muted: Boolean = false

    private val released = AtomicBoolean(false)
    private val pendingKind = AtomicInteger(KIND_NONE)
    private val workerScheduled = AtomicBoolean(false)

    // Lazily opened on the audio worker thread so startup and first Compose frame never block.
    private var initialized = false
    private var clickClip: Clip? = null
    private var popClip: Clip? = null
    private var tickClip: Clip? = null

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "logiclabs-sfx").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }
    }

    /** Toggle switch: ~5 ms broadband noise burst plus a 1.2 kHz exponentially decaying tone. */
    actual fun click() = enqueue(KIND_CLICK)

    /** Relay / power contactor: ~12 ms low-frequency thud. */
    actual fun pop() = enqueue(KIND_POP)

    /** Tie-point selection: very short, soft tick. */
    actual fun tick() = enqueue(KIND_TICK)

    actual fun release() {
        if (!released.compareAndSet(false, true)) return
        pendingKind.set(KIND_NONE)
        runCatching {
            executor.execute {
                releaseClip(clickClip)
                releaseClip(popClip)
                releaseClip(tickClip)
                clickClip = null
                popClip = null
                tickClip = null
            }
        }
        executor.shutdown()
    }

    private fun enqueue(kind: Int) {
        if (muted || released.get()) return
        // Coalesce rapid clicks so queued audio never builds lag behind user input.
        pendingKind.set(kind)
        if (workerScheduled.compareAndSet(false, true)) {
            runCatching {
                executor.execute(::drainQueue)
            }.onFailure {
                workerScheduled.set(false)
            }
        }
    }

    private fun drainQueue() {
        try {
            while (!released.get()) {
                val kind = pendingKind.getAndSet(KIND_NONE)
                if (kind == KIND_NONE) break
                if (muted) continue
                ensureInitialized()
                val clip = when (kind) {
                    KIND_CLICK -> clickClip
                    KIND_POP -> popClip
                    KIND_TICK -> tickClip
                    else -> null
                }
                playOnAudioThread(clip)
            }
        } finally {
            workerScheduled.set(false)
            // Guard against the tiny race where another click arrived right before flag reset.
            if (!released.get() && !muted && pendingKind.get() != KIND_NONE) {
                if (workerScheduled.compareAndSet(false, true)) {
                    runCatching { executor.execute(::drainQueue) }
                        .onFailure { workerScheduled.set(false) }
                }
            }
        }
    }

    private fun ensureInitialized() {
        if (initialized || released.get()) return
        initialized = true
        clickClip = buildClip(synthClick())
        popClip = buildClip(synthPop())
        tickClip = buildClip(synthTick())
    }

    private fun playOnAudioThread(clip: Clip?) {
        if (muted || clip == null || released.get()) return
        try {
            if (clip.isRunning) clip.stop()
            clip.framePosition = 0
            clip.start()
        } catch (_: Throwable) {
        }
    }

    private fun releaseClip(clip: Clip?) {
        try {
            clip?.stop()
        } catch (_: Throwable) {
        }
        try {
            clip?.close()
        } catch (_: Throwable) {
        }
    }

    private fun buildClip(pcm: ShortArray): Clip? {
        if (pcm.isEmpty()) return null
        return try {
            val format = AudioFormat(
                SAMPLE_RATE.toFloat(),
                16,
                1,
                true,
                false // little-endian PCM16
            )
            val bytes = ByteArray(pcm.size * 2)
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcm) bb.putShort(s)

            val clip = AudioSystem.getClip()
            clip.open(format, bytes, 0, bytes.size)
            runCatching {
                val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl
                if (gain != null) {
                    val targetDb = (-5.0f).coerceIn(gain.minimum, gain.maximum)
                    gain.value = targetDb
                }
            }
            clip
        } catch (_: Throwable) {
            null
        }
    }

    companion object {
        const val SAMPLE_RATE = 22050
        private const val TWO_PI = (2.0 * PI).toFloat()

        private const val KIND_NONE = 0
        private const val KIND_CLICK = 1
        private const val KIND_POP = 2
        private const val KIND_TICK = 3

        fun synthClick(): ShortArray {
            val burst = ms(5)
            val total = ms(38)
            val out = ShortArray(total)
            val rng = Random(0x5EED)
            for (i in 0 until total) {
                val t = i.toFloat() / SAMPLE_RATE
                val toneEnv = exp(-t * 95f)
                val tone = sin(TWO_PI * 1200f * t) * toneEnv * 0.55f
                val noise = if (i < burst) {
                    val noiseEnv = 1f - (i.toFloat() / burst)
                    (rng.nextFloat() * 2f - 1f) * noiseEnv * 0.45f
                } else {
                    0f
                }
                out[i] = pcm((tone + noise) * fade(i, total))
            }
            return out
        }

        fun synthPop(): ShortArray {
            val body = ms(12)
            val total = ms(70)
            val out = ShortArray(total)
            for (i in 0 until total) {
                val t = i.toFloat() / SAMPLE_RATE
                val env = exp(-t * 42f)
                val low = sin(TWO_PI * 90f * t) * 0.7f
                val thump = if (i < body) sin(TWO_PI * 150f * t) * 0.35f else 0f
                out[i] = pcm((low + thump) * env * fade(i, total))
            }
            return out
        }

        fun synthTick(): ShortArray {
            val total = ms(16)
            val out = ShortArray(total)
            for (i in 0 until total) {
                val t = i.toFloat() / SAMPLE_RATE
                val env = exp(-t * 260f)
                val tone = sin(TWO_PI * 2400f * t) * 0.3f
                out[i] = pcm(tone * env * fade(i, total))
            }
            return out
        }

        private fun ms(milliseconds: Int): Int = (SAMPLE_RATE * milliseconds) / 1000

        private fun fade(index: Int, total: Int): Float {
            val ramp = ms(1).coerceAtLeast(1)
            if (total <= ramp * 2) return 1f
            return when {
                index < ramp -> 0.5f - 0.5f * cos(PI.toFloat() * index / ramp)
                index > total - ramp -> 0.5f - 0.5f * cos(PI.toFloat() * (total - index) / ramp)
                else -> 1f
            }
        }

        private fun pcm(value: Float): Short {
            val clamped = value.coerceIn(-1f, 1f)
            return (clamped * 32000f).toInt().toShort()
        }
    }
}

