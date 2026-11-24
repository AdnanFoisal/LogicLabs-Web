package com.logiclabs.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Coordinates native browser multi-touch pinch-to-zoom, Safari trackpad gestures,
 * laptop `Ctrl + Wheel` trackpad pinch, and smooth wheel zoom directly into active
 * Skia viewport canvases (`BreadboardCanvas` and `BooleanDiagramView`).
 *
 * Compose Multiplatform 1.7.0 Web forwards `TouchEvent.changedTouches` rather than
 * `TouchEvent.touches`, which drops the stationary finger during two-finger pinches
 * on Android Chrome and iOS Safari. Registering exact root-space composable bounds here
 * allows `index.html` to compute 60-120 Hz two-finger pinch & wheel transforms in physical
 * canvas pixels and route them only when the pointer is inside an uncovered zoomable canvas.
 */
object ViewportZoomRegistry {

    class Entry(
        var boundsInRoot: Rect = Rect.Zero,
        var enabled: Boolean = true,
        val onZoomPan: (
            zoomFactor: Float,
            panDxPx: Float,
            panDyPx: Float,
            localXpx: Float,
            localYpx: Float
        ) -> Unit
    )

    private val entries = mutableListOf<Entry>()

    fun register(entry: Entry) {
        if (entry !in entries) {
            entries.add(entry)
        }
    }

    fun unregister(entry: Entry) {
        entries.remove(entry)
    }

    /**
     * Dispatches a zoom/pan delta located at `(rootXpx, rootYpx)` in root Compose canvas pixels.
     * Returns `true` if an active, uncovered viewport handled the gesture.
     */
    fun dispatchFromRoot(
        zoomFactor: Float,
        panDxPx: Float,
        panDyPx: Float,
        rootXpx: Float,
        rootYpx: Float
    ): Boolean {
        val pt = Offset(rootXpx, rootYpx)
        for (i in entries.lastIndex downTo 0) {
            val e = entries[i]
            if (!e.enabled) continue
            val b = e.boundsInRoot
            if (b.width > 1f && b.height > 1f && b.contains(pt)) {
                e.onZoomPan(
                    zoomFactor,
                    panDxPx,
                    panDyPx,
                    rootXpx - b.left,
                    rootYpx - b.top
                )
                return true
            }
        }
        return false
    }
}

/**
 * Marks a composable viewport (`BreadboardCanvas` or `BooleanDiagramView`) as a native
 * pinch-to-zoom and trackpad/wheel zoom target when [enabled] is `true`.
 */
fun Modifier.webNativeZoomTarget(
    enabled: Boolean = true,
    onZoomPan: (
        zoomFactor: Float,
        panDxPx: Float,
        panDyPx: Float,
        localXpx: Float,
        localYpx: Float
    ) -> Unit
): Modifier = composed {
    val currentHandler = rememberUpdatedState(onZoomPan)
    val entry = remember {
        ViewportZoomRegistry.Entry(
            enabled = enabled,
            onZoomPan = { z, dx, dy, lx, ly -> currentHandler.value(z, dx, dy, lx, ly) }
        )
    }
    SideEffect {
        entry.enabled = enabled
    }
    DisposableEffect(entry) {
        ViewportZoomRegistry.register(entry)
        onDispose {
            ViewportZoomRegistry.unregister(entry)
        }
    }
    this.onGloballyPositioned { coords ->
        entry.boundsInRoot = if (coords.isAttached) coords.boundsInRoot() else Rect.Zero
    }
}
