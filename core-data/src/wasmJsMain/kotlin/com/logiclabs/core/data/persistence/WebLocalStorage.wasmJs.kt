package com.logiclabs.core.data.persistence

private fun jsLsGet(key: String): String? =
    js("(() => { try { return window.localStorage.getItem(key); } catch (e) { return null; } })()")

private fun jsLsSet(key: String, value: String): Boolean =
    js("(() => { try { window.localStorage.setItem(key, value); return true; } catch (e) { return false; } })()")

private fun jsLsRemove(key: String): Boolean =
    js("(() => { try { const had = window.localStorage.getItem(key) !== null; window.localStorage.removeItem(key); return had; } catch (e) { return false; } })()")

private fun jsLsLength(): Int =
    js("(() => { try { return window.localStorage.length; } catch (e) { return 0; } })()")

private fun jsLsKey(index: Int): String? =
    js("(() => { try { return window.localStorage.key(index); } catch (e) { return null; } })()")

internal actual fun webLocalStorageGet(key: String): String? = jsLsGet(key)
internal actual fun webLocalStorageSet(key: String, value: String): Boolean = jsLsSet(key, value)
internal actual fun webLocalStorageRemove(key: String): Boolean = jsLsRemove(key)
internal actual fun webLocalStorageLength(): Int = jsLsLength()
internal actual fun webLocalStorageKeyAt(index: Int): String? = jsLsKey(index)
