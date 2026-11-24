package com.logiclabs.app.platform

import androidx.compose.ui.input.pointer.PointerIcon
import com.logiclabs.app.desktop.DesktopPlatform
import com.logiclabs.feature.tools.report.LabCertificatePdf
import com.logiclabs.feature.tools.report.exportToFile
import com.logiclabs.feature.tools.testbench.TestBenchReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.awt.Cursor
import java.io.File

actual object AppPlatform {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    actual val isWeb: Boolean = false
    actual val platformEditionLabel: String get() = DesktopPlatform.platformEditionLabel
    actual val standardStorageLabel: String get() = DesktopPlatform.standardStorageLabel
    actual val standardStoragePathHint: String get() = DesktopPlatform.standardStoragePathHint
    actual val executableName: String get() = DesktopPlatform.executableName
    actual val canOpenStorageFolder: Boolean = true
    actual val supportsFullscreen: Boolean = false
    actual val resizeHorizontalPointerIcon: PointerIcon =
        PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR))

    actual fun copyToClipboard(text: String): Boolean = DesktopPlatform.copyToClipboard(text)

    actual fun readFromClipboardAsync(onResult: (String?) -> Unit) {
        onResult(DesktopPlatform.readFromClipboard())
    }

    actual fun exportJsonText(
        suggestedFileName: String,
        jsonText: String,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        ioScope.launch {
            val target = DesktopPlatform.chooseSaveFile(
                title = "Export Logic Labs Circuit (.json)",
                defaultFileName = suggestedFileName,
                extension = "json",
                description = "Logic Labs Circuit (*.json)"
            ) ?: return@launch

            val ok = runCatching { target.writeText(jsonText); true }.getOrDefault(false)
            if (ok) onComplete(true, "Exported: ${target.name}")
            else onComplete(false, "Could not write ${target.name}")
        }
    }

    actual fun importJsonText(onLoaded: (fileName: String, content: String?) -> Unit) {
        ioScope.launch {
            val file = DesktopPlatform.chooseOpenFile(
                title = "Import Logic Labs Project (.json)",
                extension = "json",
                description = "Logic Labs Project (*.json)"
            ) ?: return@launch
            val text = runCatching { file.readText() }.getOrNull()
            onLoaded(file.name, text)
        }
    }

    actual fun exportPdfCertificate(
        report: TestBenchReport,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        ioScope.launch {
            val chosen = DesktopPlatform.chooseSaveFile(
                title = "Export Verification Certificate (A4 PDF)",
                defaultFileName = LabCertificatePdf.suggestedFileName(),
                extension = "pdf",
                description = "PDF Document (*.pdf)"
            ) ?: return@launch

            val ok = LabCertificatePdf.exportToFile(chosen, report)
            if (ok) {
                DesktopPlatform.openFileInSystem(chosen)
                onComplete(true, "Saved PDF certificate: ${chosen.name}")
            } else {
                onComplete(false, "Could not write PDF report.")
            }
        }
    }

    actual fun exportPngSnapshot(
        suggestedFileName: String,
        pngBytes: ByteArray,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        ioScope.launch {
            val chosen = DesktopPlatform.chooseSaveFile(
                title = "Export Breadboard Diagram Snapshot (PNG)",
                defaultFileName = suggestedFileName,
                extension = "png",
                description = "PNG Image (*.png)"
            ) ?: return@launch

            val ok = runCatching { chosen.writeBytes(pngBytes); true }.getOrDefault(false)
            if (ok) {
                DesktopPlatform.openFileInSystem(chosen)
                onComplete(true, "Saved PNG snapshot: ${chosen.name}")
            } else {
                onComplete(false, "Could not write PNG image.")
            }
        }
    }

    actual fun openStorageFolder(locationDisplay: String): Boolean =
        DesktopPlatform.openFolderInExplorer(File(locationDisplay))

    actual fun buildShareUrl(shareCode: String): String? = null

    actual fun isFullscreen(): Boolean = false

    actual fun toggleFullscreen(): Boolean = false

    actual fun canInstallPwa(): Boolean = false

    actual fun promptInstallPwa(onResult: (Boolean) -> Unit) = onResult(false)

    actual fun canNativeShare(): Boolean = false

    actual fun nativeShareUrl(title: String, text: String, url: String): Boolean = false
}
