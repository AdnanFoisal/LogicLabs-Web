package com.logiclabs.app.desktop

import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Native Desktop OS helpers (Windows 10/11 & Linux Mint / Ubuntu / Debian):
 * File Open & Save dialogs (Win32 native, GTK Zenity on Linux Mint with Swing fallback),
 * system clipboard, and opening exported PDFs or the Projects folder in Explorer / Nemo.
 */
object DesktopPlatform {

    private val osLower: String = System.getProperty("os.name").orEmpty().lowercase()

    val isWindows: Boolean = osLower.contains("win")
    val isLinux: Boolean = osLower.contains("nux") || osLower.contains("nix")
    val isMac: Boolean = osLower.contains("mac")

    val platformEditionLabel: String = when {
        isWindows -> "WINDOWS DESKTOP"
        isLinux -> "LINUX DESKTOP"
        isMac -> "MACOS DESKTOP"
        else -> "DESKTOP"
    }

    val standardStorageLabel: String = when {
        isWindows -> "APPDATA"
        isLinux -> "XDG DATA"
        isMac -> "APP SUPPORT"
        else -> "LOCAL DATA"
    }

    val standardStoragePathHint: String = when {
        isWindows -> "%APPDATA%\\LogicLabs"
        isLinux -> "~/.local/share/LogicLabs"
        isMac -> "~/Library/Application Support/LogicLabs"
        else -> "user data folder"
    }

    val executableName: String = if (isWindows) "LogicLabs.exe" else "LogicLabs"

    fun copyToClipboard(text: String): Boolean = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        true
    }.getOrDefault(false)

    fun readFromClipboard(): String? = runCatching {
        val cb = Toolkit.getDefaultToolkit().systemClipboard
        if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
            cb.getData(DataFlavor.stringFlavor) as? String
        } else null
    }.getOrNull()

    fun openFileInSystem(file: File): Boolean {
        if (isLinux) {
            // xdg-open works reliably across Linux Mint Cinnamon (Nemo), MATE (Caja), and Xfce (Thunar)
            val launched = runCatching {
                ProcessBuilder("xdg-open", file.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                true
            }.getOrDefault(false)
            if (launched) return true
        }
        return runCatching {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file)
                true
            } else false
        }.getOrDefault(false)
    }

    fun openFolderInExplorer(dir: File): Boolean {
        dir.mkdirs()
        return openFileInSystem(dir)
    }

    /**
     * Opens a native OS Open File dialog filtered to [extension] (e.g. "json").
     * Uses Win32 FileDialog on Windows, native GTK Zenity on Linux Mint when available,
     * and falls back cleanly to Swing JFileChooser.
     */
    fun chooseOpenFile(
        title: String = "Open Logic Labs Project",
        extension: String = "json",
        description: String = "Logic Labs Circuit (*.json)"
    ): File? {
        return try {
            when {
                isWindows -> {
                    val dlg = FileDialog(null as Frame?, title, FileDialog.LOAD)
                    dlg.file = "*.$extension"
                    dlg.isVisible = true
                    val dir = dlg.directory
                    val name = dlg.file
                    dlg.dispose()
                    if (dir != null && name != null) File(dir, name) else null
                }
                isLinux -> {
                    val zenityResult = tryZenityFileDialog(
                        save = false,
                        title = title,
                        defaultFileName = null,
                        extension = extension,
                        description = description
                    )
                    when (zenityResult) {
                        is ZenityResult.Selected -> zenityResult.file
                        ZenityResult.Cancelled -> null
                        ZenityResult.Unavailable -> swingOpenFile(title, extension, description)
                    }
                }
                else -> swingOpenFile(title, extension, description)
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Opens a native OS Save File dialog with [defaultFileName] and guarantees [extension] suffix.
     */
    fun chooseSaveFile(
        title: String,
        defaultFileName: String,
        extension: String,
        description: String
    ): File? {
        val chosen = try {
            when {
                isWindows -> {
                    val dlg = FileDialog(null as Frame?, title, FileDialog.SAVE)
                    dlg.file = defaultFileName
                    dlg.isVisible = true
                    val dir = dlg.directory
                    val name = dlg.file
                    dlg.dispose()
                    if (dir != null && name != null) File(dir, name) else null
                }
                isLinux -> {
                    val zenityResult = tryZenityFileDialog(
                        save = true,
                        title = title,
                        defaultFileName = defaultFileName,
                        extension = extension,
                        description = description
                    )
                    when (zenityResult) {
                        is ZenityResult.Selected -> zenityResult.file
                        ZenityResult.Cancelled -> null
                        ZenityResult.Unavailable -> swingSaveFile(title, defaultFileName, extension, description)
                    }
                }
                else -> swingSaveFile(title, defaultFileName, extension, description)
            }
        } catch (_: Throwable) {
            null
        } ?: return null

        val suffix = ".$extension"
        return if (chosen.name.lowercase().endsWith(suffix.lowercase())) {
            chosen
        } else {
            File(chosen.parentFile, chosen.name + suffix)
        }
    }

    private fun swingOpenFile(title: String, extension: String, description: String): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileFilter = FileNameExtensionFilter(description, extension)
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }

    private fun swingSaveFile(
        title: String,
        defaultFileName: String,
        extension: String,
        description: String
    ): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            selectedFile = File(defaultFileName)
            fileFilter = FileNameExtensionFilter(description, extension)
        }
        return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }

    private sealed interface ZenityResult {
        data class Selected(val file: File) : ZenityResult
        data object Cancelled : ZenityResult
        data object Unavailable : ZenityResult
    }

    private fun tryZenityFileDialog(
        save: Boolean,
        title: String,
        defaultFileName: String?,
        extension: String,
        description: String
    ): ZenityResult {
        return try {
            val cmd = mutableListOf(
                "zenity",
                "--file-selection",
                "--title=$title",
                "--file-filter=$description | *.$extension",
                "--file-filter=All files | *"
            )
            if (save) {
                cmd.add("--save")
                cmd.add("--confirm-overwrite")
                if (!defaultFileName.isNullOrBlank()) {
                    cmd.add("--filename=$defaultFileName")
                }
            }
            val proc = ProcessBuilder(cmd).start()
            val output = proc.inputStream.bufferedReader().readText().trim()
            val code = proc.waitFor()
            when {
                code == 0 && output.isNotEmpty() -> ZenityResult.Selected(File(output))
                code == 1 -> ZenityResult.Cancelled
                else -> ZenityResult.Unavailable
            }
        } catch (_: Throwable) {
            ZenityResult.Unavailable
        }
    }
}
