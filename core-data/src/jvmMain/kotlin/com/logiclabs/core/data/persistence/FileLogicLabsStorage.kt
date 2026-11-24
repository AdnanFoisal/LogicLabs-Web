package com.logiclabs.core.data.persistence

import com.logiclabs.core.data.progress.ProgressRepository
import com.logiclabs.core.data.session.SessionRepository
import com.logiclabs.core.data.settings.SettingsRepository
import java.io.File

/**
 * Atomic file-backed [LogicLabsStorage] for Linux, Windows, and macOS desktop builds.
 */
class FileLogicLabsStorage(
    val dataDir: File = DesktopAppPaths.resolveDataDir()
) : LogicLabsStorage {

    init {
        dataDir.mkdirs()
    }

    override val locationDisplay: String
        get() = dataDir.absolutePath

    override val isPortableMode: Boolean
        get() = DesktopAppPaths.isPortableMode()

    override fun readText(relativePath: String): String? {
        val f = File(dataDir, relativePath)
        if (!f.isFile) return null
        return runCatching { f.readText(Charsets.UTF_8) }.getOrNull()
    }

    override fun writeText(relativePath: String, text: String) {
        DesktopAppPaths.atomicWriteText(File(dataDir, relativePath), text)
    }

    override fun delete(relativePath: String): Boolean {
        return File(dataDir, relativePath).delete()
    }

    override fun listNames(dir: String): List<String> {
        val target = File(dataDir, dir)
        if (!target.isDirectory) return emptyList()
        return target.listFiles { f -> f.isFile }?.map { it.name } ?: emptyList()
    }
}

actual fun createDefaultStorage(): LogicLabsStorage = FileLogicLabsStorage()

// Convenience JVM factories keeping 100% source compatibility with existing desktop code & tests
fun ProjectsRepository(dataDir: File): ProjectsRepository = ProjectsRepository(FileLogicLabsStorage(dataDir))
fun SettingsRepository(dataDir: File): SettingsRepository = SettingsRepository(FileLogicLabsStorage(dataDir))
fun ProgressRepository(dataDir: File): ProgressRepository = ProgressRepository(FileLogicLabsStorage(dataDir))
fun SessionRepository(dataDir: File): SessionRepository = SessionRepository(FileLogicLabsStorage(dataDir))
