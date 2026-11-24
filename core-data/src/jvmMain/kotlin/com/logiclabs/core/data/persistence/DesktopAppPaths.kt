package com.logiclabs.core.data.persistence

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Resolves the desktop storage directory for saved projects, settings, progress, and session state.
 *
 * Resolution order:
 * 1. Explicit `logiclabs.data.dir` system property (for testing or custom launchers).
 * 2. Portable mode: if `portable.txt` exists beside the executable/JAR (`logiclabs.app.dir`
 *    or current working directory), stores everything in `<appDir>/data/`.
 * 3. Standard OS location:
 *    - Windows: `%APPDATA%\LogicLabs` (fallback: `%USERPROFILE%\AppData\Roaming\LogicLabs`)
 *    - macOS: `~/Library/Application Support/LogicLabs`
 *    - Linux/other: `$XDG_DATA_HOME/LogicLabs` or `~/.local/share/LogicLabs`
 */
object DesktopAppPaths {

    const val PORTABLE_MARKER = "portable.txt"

    fun resolveDataDir(
        appDir: File = detectAppDir(),
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty()
    ): File {
        val explicit = System.getProperty("logiclabs.data.dir")?.trim()
        if (!explicit.isNullOrEmpty()) {
            return File(explicit).apply { mkdirs() }
        }

        val marker = File(appDir, PORTABLE_MARKER)
        if (marker.exists()) {
            return File(appDir, "data").apply { mkdirs() }
        }

        val lowerOs = osName.lowercase()
        val baseDir = when {
            lowerOs.contains("win") -> {
                val appData = env["APPDATA"]?.takeIf { it.isNotBlank() }
                if (appData != null) File(appData, "LogicLabs")
                else File(File(File(userHome, "AppData"), "Roaming"), "LogicLabs")
            }
            lowerOs.contains("mac") -> {
                File(File(File(userHome, "Library"), "Application Support"), "LogicLabs")
            }
            else -> {
                val xdg = env["XDG_DATA_HOME"]?.takeIf { it.isNotBlank() }
                if (xdg != null) File(xdg, "LogicLabs")
                else File(File(File(userHome, ".local"), "share"), "LogicLabs")
            }
        }
        return baseDir.apply { mkdirs() }
    }

    fun isPortableMode(appDir: File = detectAppDir()): Boolean =
        File(appDir, PORTABLE_MARKER).exists()

    fun detectAppDir(): File {
        val fromProp = System.getProperty("logiclabs.app.dir")?.trim()
        if (!fromProp.isNullOrEmpty()) {
            return File(fromProp)
        }
        val codeSource = runCatching {
            File(DesktopAppPaths::class.java.protectionDomain.codeSource.location.toURI())
        }.getOrNull()
        if (codeSource != null && codeSource.isFile) {
            val parent = codeSource.parentFile
            if (parent != null) {
                // If JAR sits inside `<root>/app/LogicLabs.jar`, check `<root>` for portable.txt too.
                val grandParent = parent.parentFile
                if (grandParent != null && File(grandParent, PORTABLE_MARKER).exists()) {
                    return grandParent
                }
                return parent
            }
        }
        return File(System.getProperty("user.dir", "."))
    }

    /** Atomically writes UTF-8 text to [target], protecting against partial writes on crash. */
    fun atomicWriteText(target: File, text: String) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(text, Charsets.UTF_8)
        try {
            Files.move(
                tmp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
