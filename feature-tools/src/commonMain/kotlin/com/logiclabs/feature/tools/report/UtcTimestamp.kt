package com.logiclabs.feature.tools.report

import com.logiclabs.core.bridge.platform.padZero2
import com.logiclabs.core.bridge.platform.padZero4

/**
 * Pure-Kotlin UTC timestamp formatter (Howard Hinnant's civil-from-days algorithm)
 * replacing `java.text.SimpleDateFormat` across JVM, WasmGC, and JS.
 */
object UtcTimestamp {

    private data class CivilDateTime(
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minute: Int,
        val second: Int
    )

    private fun fromEpochMillis(epochMillis: Long): CivilDateTime {
        val totalSeconds = epochMillis / 1000L
        val days = floorDiv(totalSeconds, 86_400L)
        val secOfDay = (totalSeconds - days * 86_400L).toInt()
        val hour = secOfDay / 3600
        val minute = (secOfDay % 3600) / 60
        val second = secOfDay % 60

        val z = days + 719_468L
        val era = (if (z >= 0) z else z - 146_096L) / 146_097L
        val doe = z - era * 146_097L
        val yoe = (doe - doe / 1460L + doe / 36_524L - doe / 146_096L) / 365L
        var y = yoe + era * 400L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val d = doy - (153L * mp + 2L) / 5L + 1L
        val m = mp + (if (mp < 10L) 3L else -9L)
        if (m <= 2L) y += 1L
        return CivilDateTime(y.toInt(), m.toInt(), d.toInt(), hour, minute, second)
    }

    private fun floorDiv(a: Long, b: Long): Long {
        var q = a / b
        if ((a xor b) < 0L && q * b != a) q--
        return q
    }

    /** Formats as `"YYYY-MM-DD HH:MM:SS"`. */
    fun formatDisplay(epochMillis: Long): String {
        val t = fromEpochMillis(epochMillis)
        return "${padZero4(t.year)}-${padZero2(t.month)}-${padZero2(t.day)} " +
            "${padZero2(t.hour)}:${padZero2(t.minute)}:${padZero2(t.second)}"
    }

    /** Formats as `"YYYYMMDD-HHMMSS"` for safe filenames. */
    fun formatFileStamp(epochMillis: Long): String {
        val t = fromEpochMillis(epochMillis)
        return "${padZero4(t.year)}${padZero2(t.month)}${padZero2(t.day)}-" +
            "${padZero2(t.hour)}${padZero2(t.minute)}${padZero2(t.second)}"
    }
}
