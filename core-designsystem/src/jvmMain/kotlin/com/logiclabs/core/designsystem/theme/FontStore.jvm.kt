package com.logiclabs.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

internal actual fun loadBundledFontBytes(resourcePath: String): ByteArray? = runCatching {
    LogicLabsFontStore::class.java.classLoader?.getResourceAsStream(resourcePath)?.use { it.readBytes() }
}.getOrNull()

internal actual fun buildFontFamily(identity: String, data: ByteArray?, fallback: FontFamily): FontFamily {
    if (data == null || data.isEmpty()) return fallback
    return FontFamily(
        Font(identity = "$identity-normal", data = data, weight = FontWeight.Normal),
        Font(identity = "$identity-medium", data = data, weight = FontWeight.Medium),
        Font(identity = "$identity-semibold", data = data, weight = FontWeight.SemiBold),
        Font(identity = "$identity-bold", data = data, weight = FontWeight.Bold)
    )
}
