package com.logiclabs.core.data.persistence

internal actual fun webLocalStorageGet(key: String): String? = try {
    kotlinx.browser.window.localStorage.getItem(key)
} catch (_: Throwable) {
    null
}

internal actual fun webLocalStorageSet(key: String, value: String): Boolean = try {
    kotlinx.browser.window.localStorage.setItem(key, value)
    true
} catch (_: Throwable) {
    false
}

internal actual fun webLocalStorageRemove(key: String): Boolean = try {
    val had = kotlinx.browser.window.localStorage.getItem(key) != null
    kotlinx.browser.window.localStorage.removeItem(key)
    had
} catch (_: Throwable) {
    false
}

internal actual fun webLocalStorageLength(): Int = try {
    kotlinx.browser.window.localStorage.length
} catch (_: Throwable) {
    0
}

internal actual fun webLocalStorageKeyAt(index: Int): String? = try {
    kotlinx.browser.window.localStorage.key(index)
} catch (_: Throwable) {
    null
}
