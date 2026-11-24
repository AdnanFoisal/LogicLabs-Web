package com.logiclabs.app.platform

import androidx.compose.ui.input.pointer.PointerIcon
import com.logiclabs.feature.tools.testbench.TestBenchReport

/**
 * Cross-platform OS / browser integration facade shared by Desktop (JVM) and Web (WasmGC / JS).
 */
expect object AppPlatform {
    val isWeb: Boolean
    val platformEditionLabel: String
    val standardStorageLabel: String
    val standardStoragePathHint: String
    val executableName: String
    val canOpenStorageFolder: Boolean
    val supportsFullscreen: Boolean
    val resizeHorizontalPointerIcon: PointerIcon

    fun copyToClipboard(text: String): Boolean

    fun readFromClipboardAsync(onResult: (String?) -> Unit)

    fun exportJsonText(
        suggestedFileName: String,
        jsonText: String,
        onComplete: (success: Boolean, message: String) -> Unit
    )

    fun importJsonText(
        onLoaded: (fileName: String, content: String?) -> Unit
    )

    fun exportPdfCertificate(
        report: TestBenchReport,
        onComplete: (success: Boolean, message: String) -> Unit
    )

    fun exportPngSnapshot(
        suggestedFileName: String,
        pngBytes: ByteArray,
        onComplete: (success: Boolean, message: String) -> Unit
    )

    fun openStorageFolder(locationDisplay: String): Boolean

    /** Returns a direct clickable Web share URL (`?circuit=LOGIC-...`) on Web, or `null` on Desktop. */
    fun buildShareUrl(shareCode: String): String?

    fun isFullscreen(): Boolean

    fun toggleFullscreen(): Boolean

    fun canInstallPwa(): Boolean

    fun promptInstallPwa(onResult: (Boolean) -> Unit = {})

    fun canNativeShare(): Boolean

    fun nativeShareUrl(title: String, text: String, url: String): Boolean
}
