package com.logiclabs.core.bridge.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

/** Wall-clock UTC epoch milliseconds, safe across JVM, WasmGC, and JS. */
expect fun currentTimeMillis(): Long

/** Off-UI I/O dispatcher (`Dispatchers.IO` on JVM, `Dispatchers.Default` on single-threaded Web). */
expect val ioDispatcher: CoroutineDispatcher

/** Pure-Kotlin RFC 4122 v4 UUID string generator (no `java.util.UUID` dependency). */
fun randomUuid(): String {
    val bytes = ByteArray(16)
    Random.nextBytes(bytes)
    bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte() // version 4
    bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte() // variant RFC 4122
    val hex = "0123456789abcdef"
    val sb = StringBuilder(36)
    for (i in bytes.indices) {
        if (i == 4 || i == 6 || i == 8 || i == 10) sb.append('-')
        val v = bytes[i].toInt() and 0xFF
        sb.append(hex[v ushr 4])
        sb.append(hex[v and 0x0F])
    }
    return sb.toString()
}

/** Multiplatform replacement for `"%02d".format(n)`. */
fun padZero2(value: Int): String = if (value in 0..9) "0$value" else value.toString()

/** Multiplatform replacement for `"%04d".format(n)`. */
fun padZero4(value: Int): String = value.toString().padStart(4, '0')

/** Multiplatform replacement for `"%02x".format(byte)`. */
fun toHexByte(value: Byte): String {
    val hex = "0123456789abcdef"
    val v = value.toInt() and 0xFF
    return "${hex[v ushr 4]}${hex[v and 0x0F]}"
}

/** Multiplatform replacement for `String.format(Locale.US, "%.<decimals>f", value)`. */
fun formatFixed(value: Double, decimals: Int): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value < 0) "-Infinity" else "Infinity"
    val factor = 10.0.pow(decimals)
    val scaled = (abs(value) * factor).roundToLong()
    val factorLong = factor.roundToLong().coerceAtLeast(1L)
    val intPart = scaled / factorLong
    val fracPart = scaled % factorLong
    val sign = if (value < 0.0 && scaled != 0L) "-" else ""
    return if (decimals <= 0) {
        "$sign$intPart"
    } else {
        "$sign$intPart.${fracPart.toString().padStart(decimals, '0')}"
    }
}

fun formatFixed1(value: Double): String = formatFixed(value, 1)
fun formatFixed1(value: Float): String = formatFixed(value.toDouble(), 1)
fun formatFixed2(value: Double): String = formatFixed(value, 2)
fun formatFixed2(value: Float): String = formatFixed(value.toDouble(), 2)
