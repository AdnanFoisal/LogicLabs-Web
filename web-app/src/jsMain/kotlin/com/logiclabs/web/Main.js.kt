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
import org.jetbrains.skiko.wasm.onWasmReady
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get

internal actual fun webRegisterBrowserBridges(
    onDroppedJson: (fileName: String, content: String) -> Unit,
    onBrowserBack: () -> Boolean
) {
    val dropCb: (String, String) -> Unit = onDroppedJson
    val backCb: () -> Boolean = onBrowserBack
    js("""
    (function(dCb, bCb) {
      window.__logicLabsOnDroppedJson = function(name, content) { dCb(name || 'circuit.json', content || ''); };
      window.__logicLabsOnBrowserBack = function() { return Boolean(bCb()); };
    })(dropCb, backCb)
    """)
}

internal actual fun webSyncBrowserState(
    pageTitle: String,
    querySuffix: String,
    pushHistory: Boolean,
    themeColorHex: String
) {
    js("""
    (function(t, q, p, hex) {
      if (window.__logicLabsSyncBrowserState) {
        window.__logicLabsSyncBrowserState(t, q, p, hex);
      }
    })(pageTitle, querySuffix, pushHistory, themeColorHex)
    """)
}

private suspend fun fetchFontBytesJs(url: String): ByteArray? = runCatching {
    val resp = window.fetch(url).await()
    if (!resp.ok) return null
    val buf: ArrayBuffer = resp.arrayBuffer().await()
    val arr = Int8Array(buf)
    ByteArray(arr.length) { i -> arr[i] }
}.getOrNull()

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val zoomCb: (Float, Float, Float, Float, Float, Boolean) -> Boolean = { z, dx, dy, rx, ry, allowFallback ->
        ViewportZoomRegistry.dispatchFromRoot(z, dx, dy, rx, ry, allowFallback)
    }
    js("""
    (function(cb) {
      window.__logicLabsDispatchZoom = function(z, dx, dy, rx, ry, allowFallback) {
        return Boolean(cb(z, dx, dy, rx, ry, Boolean(allowFallback)));
      };
    })(zoomCb)
    """)

    val launchParams = WebLaunchParams.parse(window.location.search, window.location.hash)
    onWasmReady {
        MainScope().launch {
            js("if (window.__setLogicLabsSplashStatus) window.__setLogicLabsSplashStatus('Loading bundled Inter & JetBrains Mono typography…');")
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
