package com.logiclabs.core.data.persistence

/**
 * Cross-platform key/path UTF-8 text persistence backend.
 *
 * - JVM Desktop (`FileLogicLabsStorage`): atomic file writes under `~/.local/share/LogicLabs`,
 *   `%APPDATA%\LogicLabs`, or `<appDir>/data` when `portable.txt` is present.
 * - Web (`BrowserLocalStorage`): browser `window.localStorage` (survives reloads & offline PWA).
 */
interface LogicLabsStorage {
    fun readText(relativePath: String): String?
    fun writeText(relativePath: String, text: String)
    fun delete(relativePath: String): Boolean
    /** Lists file names (e.g. `"abc.json"`) stored directly inside [dir] (e.g. `"projects"`). */
    fun listNames(dir: String): List<String>
    val locationDisplay: String
    val isPortableMode: Boolean
}

expect fun createDefaultStorage(): LogicLabsStorage

/** Simple in-memory backend useful for tests and private-mode fallback. */
class InMemoryLogicLabsStorage(
    override val locationDisplay: String = "In-Memory Session",
    override val isPortableMode: Boolean = false
) : LogicLabsStorage {
    private val store = LinkedHashMap<String, String>()

    override fun readText(relativePath: String): String? = store[relativePath]

    override fun writeText(relativePath: String, text: String) {
        store[relativePath] = text
    }

    override fun delete(relativePath: String): Boolean = store.remove(relativePath) != null

    override fun listNames(dir: String): List<String> {
        val prefix = if (dir.endsWith("/")) dir else "$dir/"
        return store.keys
            .filter { it.startsWith(prefix) && !it.removePrefix(prefix).contains('/') }
            .map { it.removePrefix(prefix) }
    }
}
