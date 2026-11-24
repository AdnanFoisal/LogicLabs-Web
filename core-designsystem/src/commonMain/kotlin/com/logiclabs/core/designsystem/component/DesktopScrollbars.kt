package com.logiclabs.core.designsystem.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.SurfaceCardBorder
import com.logiclabs.core.designsystem.theme.SurfaceRaised
import com.logiclabs.core.designsystem.theme.TextTertiary
import kotlinx.coroutines.launch

/** Shows the native hand pointer when hovering interactive controls on Desktop and Web browsers. */
fun Modifier.desktopPointerHand(): Modifier =
    this.pointerHoverIcon(PointerIcon.Hand)

/** Always-discoverable, theme-matched vertical scrollbar for scrollable columns. */
@Composable
expect fun DesktopVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
)

@Composable
expect fun DesktopVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier
)

/** Always-discoverable, theme-matched horizontal scrollbar for overflowing strips. */
@Composable
expect fun DesktopHorizontalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
)

@Composable
expect fun DesktopHorizontalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier
)

/** Hardware-styled hover tooltip with optional keyboard shortcut pill (no-op passthrough on touch Web). */
@Composable
expect fun DesktopTooltip(
    text: String,
    shortcut: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)

/**
 * Translates standard vertical mouse-wheel scrolling into horizontal scrolling when hovering
 * a horizontal strip (`ToolRail`, `CommandBar`, `ProbePicker`, `VerifierDialog` target strip).
 */
fun Modifier.horizontalWheelScroll(scrollState: ScrollState, stepPx: Float = 64f): Modifier = composed {
    val scope = rememberCoroutineScope()
    this.pointerInput(scrollState) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll && scrollState.maxValue > 0) {
                    val delta = event.changes.firstOrNull()?.scrollDelta ?: continue
                    if (delta.y != 0f && delta.x == 0f) {
                        val canGoRight = delta.y > 0f && scrollState.value < scrollState.maxValue
                        val canGoLeft = delta.y < 0f && scrollState.value > 0
                        if (canGoRight || canGoLeft) {
                            scope.launch { scrollState.scrollBy(delta.y * stepPx) }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
        }
    }
}

/** Vertical mouse-wheel → horizontal scroll bridge for horizontal `LazyRow` strips. */
fun Modifier.horizontalWheelScroll(listState: LazyListState, stepPx: Float = 72f): Modifier = composed {
    val scope = rememberCoroutineScope()
    this.pointerInput(listState) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val delta = event.changes.firstOrNull()?.scrollDelta ?: continue
                    if (delta.y != 0f && delta.x == 0f) {
                        val canGoRight = delta.y > 0f && listState.canScrollForward
                        val canGoLeft = delta.y < 0f && listState.canScrollBackward
                        if (canGoRight || canGoLeft) {
                            scope.launch { listState.scrollBy(delta.y * stepPx) }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
        }
    }
}

/** Enables PageUp / PageDown / Home / End keyboard scrolling on a `ScrollState` column. */
fun Modifier.keyboardPageScroll(scrollState: ScrollState, pageStepPx: Float = 360f): Modifier = composed {
    val scope = rememberCoroutineScope()
    this.onPreviewKeyEvent { ev ->
        if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (ev.key) {
            Key.PageDown -> {
                scope.launch { scrollState.scrollBy(pageStepPx) }
                true
            }
            Key.PageUp -> {
                scope.launch { scrollState.scrollBy(-pageStepPx) }
                true
            }
            Key.MoveHome -> {
                scope.launch { scrollState.scrollTo(0) }
                true
            }
            Key.MoveEnd -> {
                scope.launch { scrollState.scrollTo(scrollState.maxValue) }
                true
            }
            else -> false
        }
    }
}

/** Enables PageUp / PageDown / Home / End keyboard scrolling on a `LazyListState` list. */
fun Modifier.keyboardPageScroll(listState: LazyListState, pageStepPx: Float = 360f): Modifier = composed {
    val scope = rememberCoroutineScope()
    this.onPreviewKeyEvent { ev ->
        if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (ev.key) {
            Key.PageDown -> {
                scope.launch { listState.scrollBy(pageStepPx) }
                true
            }
            Key.PageUp -> {
                scope.launch { listState.scrollBy(-pageStepPx) }
                true
            }
            Key.MoveHome -> {
                scope.launch { listState.scrollToItem(0) }
                true
            }
            Key.MoveEnd -> {
                val last = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                scope.launch { listState.scrollToItem(last) }
                true
            }
            else -> false
        }
    }
}

/** Compact `<` / `>` nudge button for overflowing horizontal strips (`ProbePicker`, `ToolRail`, etc.). */
@Composable
fun DesktopScrollArrowButton(
    isLeft: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .background(if (enabled) SurfaceRaised else SurfaceCard.copy(alpha = 0.45f))
            .border(
                width = Dimens.Hairline,
                color = if (enabled) AccentCyan.copy(alpha = 0.55f) else SurfaceCardBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(Dimens.RadiusSm)
            )
            .then(if (enabled) Modifier.desktopPointerHand().clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isLeft) "‹" else "›",
            style = LogicLabsType.TitleSm.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = if (enabled) AccentCyan else TextTertiary.copy(alpha = 0.45f)
        )
    }
}
