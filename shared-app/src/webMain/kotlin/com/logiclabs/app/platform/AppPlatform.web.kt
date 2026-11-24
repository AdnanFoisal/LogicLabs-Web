package com.logiclabs.app.platform

import androidx.compose.ui.input.pointer.PointerIcon
import com.logiclabs.feature.tools.report.LabCertificatePdf
import com.logiclabs.feature.tools.testbench.TestBenchReport
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal expect fun webPlatformCopyText(text: String): Boolean
internal expect fun webPlatformReadClipboard(onResult: (String?) -> Unit)
internal expect fun webPlatformDownloadText(fileName: String, mimeType: String, content: String): Boolean
internal expect fun webPlatformDownloadBase64Bytes(fileName: String, mimeType: String, base64Data: String): Boolean
internal expect fun webPlatformPickJsonFile(onLoaded: (fileName: String, content: String?) -> Unit)
internal expect fun webPlatformBasePageUrl(): String
internal expect fun webPlatformEngineLabel(): String
internal expect fun webPlatformSupportsFullscreen(): Boolean
internal expect fun webPlatformIsFullscreen(): Boolean
internal expect fun webPlatformToggleFullscreen(): Boolean
internal expect fun webPlatformCanInstallPwa(): Boolean
internal expect fun webPlatformPromptInstallPwa(onResult: (Boolean) -> Unit)
internal expect fun webPlatformCanNativeShare(): Boolean
internal expect fun webPlatformNativeShareUrl(title: String, text: String, url: String): Boolean

@OptIn(ExperimentalEncodingApi::class)
actual object AppPlatform {
    actual val isWeb: Boolean = true
    actual val platformEditionLabel: String get() = "WEB (${webPlatformEngineLabel()})"
    actual val standardStorageLabel: String = "BROWSER LOCAL"
    actual val standardStoragePathHint: String = "Browser localStorage (Offline PWA)"
    actual val executableName: String = "Logic Labs Web PWA"
    actual val canOpenStorageFolder: Boolean = false
    actual val supportsFullscreen: Boolean get() = webPlatformSupportsFullscreen()
    actual val resizeHorizontalPointerIcon: PointerIcon = PointerIcon.Hand

    actual fun copyToClipboard(text: String): Boolean = webPlatformCopyText(text)

    actual fun readFromClipboardAsync(onResult: (String?) -> Unit) = webPlatformReadClipboard(onResult)

    actual fun exportJsonText(
        suggestedFileName: String,
        jsonText: String,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        val ok = webPlatformDownloadText(suggestedFileName, "application/json;charset=utf-8", jsonText)
        if (ok) onComplete(true, "Downloaded: $suggestedFileName")
        else onComplete(false, "Browser blocked JSON download.")
    }

    actual fun importJsonText(onLoaded: (fileName: String, content: String?) -> Unit) {
        webPlatformPickJsonFile(onLoaded)
    }

    actual fun exportPdfCertificate(
        report: TestBenchReport,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        val bytes = LabCertificatePdf.renderPdfBytes(report)
        if (bytes == null) {
            onComplete(false, "Could not generate PDF certificate.")
            return
        }
        val fileName = LabCertificatePdf.suggestedFileName()
        val ok = webPlatformDownloadBase64Bytes(fileName, "application/pdf", Base64.encode(bytes))
        if (ok) onComplete(true, "Downloaded A4 PDF certificate: $fileName")
        else onComplete(false, "Browser blocked PDF download.")
    }

    actual fun exportPngSnapshot(
        suggestedFileName: String,
        pngBytes: ByteArray,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        val ok = webPlatformDownloadBase64Bytes(suggestedFileName, "image/png", Base64.encode(pngBytes))
        if (ok) onComplete(true, "Downloaded breadboard PNG: $suggestedFileName")
        else onComplete(false, "Browser blocked PNG download.")
    }

    actual fun openStorageFolder(locationDisplay: String): Boolean = false

    actual fun buildShareUrl(shareCode: String): String? {
        val base = webPlatformBasePageUrl()
        if (base.isBlank()) return null
        return "$base?circuit=$shareCode"
    }

    actual fun isFullscreen(): Boolean = webPlatformIsFullscreen()

    actual fun toggleFullscreen(): Boolean = webPlatformToggleFullscreen()

    actual fun canInstallPwa(): Boolean = webPlatformCanInstallPwa()

    actual fun promptInstallPwa(onResult: (Boolean) -> Unit) = webPlatformPromptInstallPwa(onResult)

    actual fun canNativeShare(): Boolean = webPlatformCanNativeShare()

    actual fun nativeShareUrl(title: String, text: String, url: String): Boolean =
        webPlatformNativeShareUrl(title, text, url)
}
