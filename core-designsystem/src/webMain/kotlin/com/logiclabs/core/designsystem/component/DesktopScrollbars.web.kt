package com.logiclabs.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.SurfaceCardBorder
import com.logiclabs.core.designsystem.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
private fun WebScrollbarThumb(
    fraction: Float,
    thumbRatio: Float,
    isHorizontal: Boolean,
    onDragDeltaRatio: (Float) -> Unit,
    modifier: Modifier
) {
    var hovered by remember { mutableStateOf(false) }
    val thumbColor = if (hovered) AccentCyan.copy(alpha = 0.88f) else TextSecondary.copy(alpha = 0.52f)
    val trackModifier = if (isHorizontal) {
        modifier
            .height(9.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(SurfaceCardBorder.copy(alpha = 0.22f))
            .padding(horizontal = 2.dp, vertical = 1.dp)
    } else {
        modifier
            .width(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceCardBorder.copy(alpha = 0.22f))
            .padding(horizontal = 1.dp, vertical = 2.dp)
    }

    Canvas(
        modifier = trackModifier
            .desktopPointerHand()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent()
                        when (ev.type) {
                            PointerEventType.Enter -> hovered = true
                            PointerEventType.Exit -> hovered = false
                            else -> Unit
                        }
                    }
                }
            }
            .pointerInput(isHorizontal) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val trackLen = if (isHorizontal) size.width.toFloat() else size.height.toFloat()
                    if (trackLen > 1f) {
                        val delta = if (isHorizontal) dragAmount.x else dragAmount.y
                        onDragDeltaRatio(delta / trackLen)
                    }
                }
            }
    ) {
        val clampedRatio = thumbRatio.coerceIn(0.12f, 1f)
        val clampedFrac = fraction.coerceIn(0f, 1f)
        if (isHorizontal) {
            val thumbW = (size.width * clampedRatio).coerceAtLeast(28f).coerceAtMost(size.width)
            val travel = (size.width - thumbW).coerceAtLeast(0f)
            val x = travel * clampedFrac
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(x, 0f),
                size = Size(thumbW, size.height),
                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
            )
        } else {
            val thumbH = (size.height * clampedRatio).coerceAtLeast(32f).coerceAtMost(size.height)
            val travel = (size.height - thumbH).coerceAtLeast(0f)
            val y = travel * clampedFrac
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(0f, y),
                size = Size(size.width, thumbH),
                cornerRadius = CornerRadius(size.width / 2f, size.width / 2f)
            )
        }
    }
}

@Composable
actual fun DesktopVerticalScrollbar(scrollState: ScrollState, modifier: Modifier) {
    val max = scrollState.maxValue
    if (max <= 0) return
    val scope = rememberCoroutineScope()
    val viewport = scrollState.viewportSize.toFloat().coerceAtLeast(1f)
    val total = viewport + max.toFloat()
    val fraction = scrollState.value.toFloat() / max.toFloat()
    WebScrollbarThumb(
        fraction = fraction,
        thumbRatio = viewport / total,
        isHorizontal = false,
        onDragDeltaRatio = { deltaRatio ->
            scope.launch { scrollState.scrollBy(deltaRatio * total) }
        },
        modifier = modifier.fillMaxHeight()
    )
}

@Composable
actual fun DesktopVerticalScrollbar(listState: LazyListState, modifier: Modifier) {
    val info = listState.layoutInfo
    val totalItems = info.totalItemsCount
    val visible = info.visibleItemsInfo.size
    if (totalItems <= 0 || (!listState.canScrollForward && !listState.canScrollBackward)) return
    val scope = rememberCoroutineScope()
    val maxIdx = (totalItems - visible).coerceAtLeast(1)
    val fraction = listState.firstVisibleItemIndex.toFloat() / maxIdx.toFloat()
    val thumbRatio = visible.toFloat() / totalItems.toFloat()
    val approxTotalPx = (info.viewportSize.height.toFloat() / thumbRatio.coerceAtLeast(0.1f))
    WebScrollbarThumb(
        fraction = fraction,
        thumbRatio = thumbRatio,
        isHorizontal = false,
        onDragDeltaRatio = { deltaRatio ->
            scope.launch { listState.scrollBy(deltaRatio * approxTotalPx) }
        },
        modifier = modifier.fillMaxHeight()
    )
}

@Composable
actual fun DesktopHorizontalScrollbar(scrollState: ScrollState, modifier: Modifier) {
    val max = scrollState.maxValue
    if (max <= 0) return
    val scope = rememberCoroutineScope()
    val viewport = scrollState.viewportSize.toFloat().coerceAtLeast(1f)
    val total = viewport + max.toFloat()
    val fraction = scrollState.value.toFloat() / max.toFloat()
    WebScrollbarThumb(
        fraction = fraction,
        thumbRatio = viewport / total,
        isHorizontal = true,
        onDragDeltaRatio = { deltaRatio ->
            scope.launch { scrollState.scrollBy(deltaRatio * total) }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
actual fun DesktopHorizontalScrollbar(listState: LazyListState, modifier: Modifier) {
    val info = listState.layoutInfo
    val totalItems = info.totalItemsCount
    val visible = info.visibleItemsInfo.size
    if (totalItems <= 0 || (!listState.canScrollForward && !listState.canScrollBackward)) return
    val scope = rememberCoroutineScope()
    val maxIdx = (totalItems - visible).coerceAtLeast(1)
    val fraction = listState.firstVisibleItemIndex.toFloat() / maxIdx.toFloat()
    val thumbRatio = visible.toFloat() / totalItems.toFloat()
    val approxTotalPx = (info.viewportSize.width.toFloat() / thumbRatio.coerceAtLeast(0.1f))
    WebScrollbarThumb(
        fraction = fraction,
        thumbRatio = thumbRatio,
        isHorizontal = true,
        onDragDeltaRatio = { deltaRatio ->
            scope.launch { listState.scrollBy(deltaRatio * approxTotalPx) }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
actual fun DesktopTooltip(
    text: String,
    shortcut: String?,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}
