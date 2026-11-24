package com.logiclabs.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.logiclabs.core.designsystem.canvas.DesktopTypefaces
import com.logiclabs.core.designsystem.component.ViewportZoomRegistry
import com.logiclabs.core.designsystem.theme.LogicLabsFontStore
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import kotlin.js.Promise

private fun jsSetSplashStatus(msg: String): Unit =
    js("(() => { if (window.__setLogicLabsSplashStatus) window.__setLogicLabsSplashStatus(msg); })()")

private fun jsHideSplash(): Unit =
    js("(() => { if (window.__hideLogicLabsSplash) window.__hideLogicLabsSplash(); })()")

private fun jsFetchArrayBuffer(url: String): Promise<org.khronos.webgl.ArrayBuffer> =
    js("fetch(url).then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.arrayBuffer(); })")

private fun jsInstallZoomBridge(
    handler: (Float, Float, Float, Float, Float) -> Boolean
): Unit = js("""
(() => {
  window.__logicLabsDispatchZoom = (z, dx, dy, rx, ry) => Boolean(handler(z, dx, dy, rx, ry));
})()
""")

private fun jsInstallBrowserBridges(
    onDroppedJson: (String, String) -> Unit,
    onBrowserBack: () -> Boolean
): Unit = js("""
(() => {
  window.__logicLabsOnDroppedJson = (name, content) => { onDroppedJson(name || 'circuit.json', content || ''); };
  window.__logicLabsOnBrowserBack = () => Boolean(onBrowserBack());
})()
""")

private fun jsSyncBrowserState(
    pageTitle: String,
    querySuffix: String,
    pushHistory: Boolean,
    themeColorHex: String
): Unit = js("""
(() => {
  if (window.__logicLabsSyncBrowserState) {
    window.__logicLabsSyncBrowserState(pageTitle, querySuffix, pushHistory, themeColorHex);
  }
})()
""")

internal actual fun webRegisterBrowserBridges(
    onDroppedJson: (fileName: String, content: String) -> Unit,
    onBrowserBack: () -> Boolean
) {
    jsInstallBrowserBridges(onDroppedJson, onBrowserBack)
}

internal actual fun webSyncBrowserState(
    pageTitle: String,
    querySuffix: String,
    pushHistory: Boolean,
    themeColorHex: String
) {
    jsSyncBrowserState(pageTitle, querySuffix, pushHistory, themeColorHex)
}

private suspend fun fetchFontBytes(url: String): ByteArray? = runCatching {
    val buf: org.khronos.webgl.ArrayBuffer = jsFetchArrayBuffer(url).await()
    val arr = Int8Array(buf)
    ByteArray(arr.length) { i -> arr[i] }
}.getOrNull()

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    jsInstallZoomBridge { z, dx, dy, rx, ry ->
        ViewportZoomRegistry.dispatchFromRoot(z, dx, dy, rx, ry)
    }

    val launchParams = WebLaunchParams.parse(window.location.search, window.location.hash)
    MainScope().launch {
        jsSetSplashStatus("Loading bundled Inter & JetBrains Mono typography…")
        val interBytes = fetchFontBytes("./font/inter_semibold.ttf")
        val monoBytes = fetchFontBytes("./font/jetbrains_mono_medium.ttf")
        LogicLabsFontStore.initialize(interBytes, monoBytes)
        DesktopTypefaces.resetCache()

        jsSetSplashStatus("Starting K&H IDL-800A Skia WebGL Workstation…")
        val root = document.getElementById("logiclabs-root") ?: document.body!!
        jsHideSplash()
        ComposeViewport(root) {
            LogicLabsWebRoot(launchParams = launchParams)
        }
    }
}
