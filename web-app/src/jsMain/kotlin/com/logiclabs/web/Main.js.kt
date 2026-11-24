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
import org.jetbrains.skiko.wasm.onWasmReady
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get

private suspend fun fetchFontBytesJs(url: String): ByteArray? = runCatching {
    val resp = window.fetch(url).await()
    if (!resp.ok) return null
    val buf: ArrayBuffer = resp.arrayBuffer().await()
    val arr = Int8Array(buf)
    ByteArray(arr.length) { i -> arr[i] }
}.getOrNull()

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val launchParams = WebLaunchParams.parse(window.location.search, window.location.hash)
    onWasmReady {
        MainScope().launch {
            val interBytes = fetchFontBytesJs("./font/inter_semibold.ttf")
            val monoBytes = fetchFontBytesJs("./font/jetbrains_mono_medium.ttf")
            LogicLabsFontStore.initialize(interBytes, monoBytes)
            DesktopTypefaces.resetCache()

            val root = document.getElementById("logiclabs-root") ?: document.body!!
            js("if (window.__hideLogicLabsSplash) window.__hideLogicLabsSplash();")
            ComposeViewport(root) {
                LogicLabsWebRoot(launchParams = launchParams)
            }
        }
    }
}
