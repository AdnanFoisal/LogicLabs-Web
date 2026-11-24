package com.logiclabs.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.logiclabs.core.designsystem.canvas.DesktopTypefaces
import com.logiclabs.core.designsystem.theme.LogicLabsFontStore
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.fetch.Response
import kotlin.js.Promise

private fun jsHideSplash(): Unit = js("(() => { if (window.__hideLogicLabsSplash) window.__hideLogicLabsSplash(); })()")

private fun jsFetchArrayBuffer(url: String): Promise<org.khronos.webgl.ArrayBuffer> =
    js("fetch(url).then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.arrayBuffer(); })")

private suspend fun fetchFontBytes(url: String): ByteArray? = runCatching {
    val buf: org.khronos.webgl.ArrayBuffer = jsFetchArrayBuffer(url).await()
    val arr = Int8Array(buf)
    ByteArray(arr.length) { i -> arr[i] }
}.getOrNull()

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val launchParams = WebLaunchParams.parse(window.location.search, window.location.hash)
    MainScope().launch {
        val interBytes = fetchFontBytes("./font/inter_semibold.ttf")
        val monoBytes = fetchFontBytes("./font/jetbrains_mono_medium.ttf")
        LogicLabsFontStore.initialize(interBytes, monoBytes)
        DesktopTypefaces.resetCache()

        val root = document.getElementById("logiclabs-root") ?: document.body!!
        jsHideSplash()
        ComposeViewport(root) {
            LogicLabsWebRoot(launchParams = launchParams)
        }
    }
}
