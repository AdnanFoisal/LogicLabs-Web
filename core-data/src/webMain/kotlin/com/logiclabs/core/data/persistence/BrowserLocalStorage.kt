package com.logiclabs.core.data.persistence

internal expect fun webLocalStorageGet(key: String): String?
internal expect fun webLocalStorageSet(key: String, value: String): Boolean
internal expect fun webLocalStorageRemove(key: String): Boolean
internal expect fun webLocalStorageLength(): Int
internal expect fun webLocalStorageKeyAt(index: Int): String?

/**
 * Browser `window.localStorage` implementation with automatic in-memory fallback when running
 * inside strict private browsing contexts or when browser storage quota is exceeded.
 */
class BrowserLocalStorage : LogicLabsStorage {
    private val fallback = InMemoryLogicLabsStorage("Browser Session (Memory)")
    private val keyPrefix = "logiclabs.v1:"

    override val locationDisplay: String = "Browser Local Storage (Offline PWA)"
    override val isPortableMode: Boolean = false

    override fun readText(relativePath: String): String? {
        return webLocalStorageGet(keyPrefix + relativePath) ?: fallback.readText(relativePath)
    }

    override fun writeText(relativePath: String, text: String) {
        if (!webLocalStorageSet(keyPrefix + relativePath, text)) {
            fallback.writeText(relativePath, text)
        } else {
            fallback.writeText(relativePath, text)
        }
    }

    override fun delete(relativePath: String): Boolean {
        val removedWeb = webLocalStorageRemove(keyPrefix + relativePath)
        val removedMem = fallback.delete(relativePath)
        return removedWeb || removedMem
    }

    override fun listNames(dir: String): List<String> {
        val dirPrefix = keyPrefix + (if (dir.endsWith("/")) dir else "$dir/")
        val names = LinkedHashSet<String>()
        val len = webLocalStorageLength()
        for (i in 0 until len) {
            val k = webLocalStorageKeyAt(i) ?: continue
            if (k.startsWith(dirPrefix)) {
                val tail = k.removePrefix(dirPrefix)
                if (!tail.contains('/')) names.add(tail)
            }
        }
        names.addAll(fallback.listNames(dir))
        return names.toList()
    }
}

actual fun createDefaultStorage(): LogicLabsStorage = BrowserLocalStorage()
