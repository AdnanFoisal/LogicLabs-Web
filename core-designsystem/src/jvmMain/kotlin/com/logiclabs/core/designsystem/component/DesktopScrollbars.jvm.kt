package com.logiclabs.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.ChassisBase
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.SurfaceCardBorder
import com.logiclabs.core.designsystem.theme.SurfaceRaised
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextSecondary

@Composable
private fun rememberDesktopScrollbarStyle(isHorizontal: Boolean = false): ScrollbarStyle {
    val unhover = TextSecondary.copy(alpha = 0.48f)
    val hover = AccentCyan.copy(alpha = 0.88f)
    return remember(unhover, hover, isHorizontal) {
        ScrollbarStyle(
            minimalHeight = 32.dp,
            thickness = if (isHorizontal) 7.dp else 9.dp,
            shape = RoundedCornerShape(5.dp),
            hoverDurationMillis = 120,
            unhoverColor = unhover,
            hoverColor = hover
        )
    }
}

@Composable
private fun DesktopVerticalScrollbarAdapter(adapter: ScrollbarAdapter, modifier: Modifier) {
    val canScroll = adapter.contentSize > adapter.viewportSize + 1.0
    if (!canScroll) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceCardBorder.copy(alpha = 0.22f))
            .padding(horizontal = 1.dp, vertical = 2.dp)
            .desktopPointerHand()
    ) {
        VerticalScrollbar(
            adapter = adapter,
            style = rememberDesktopScrollbarStyle(isHorizontal = false)
        )
    }
}

@Composable
actual fun DesktopVerticalScrollbar(scrollState: ScrollState, modifier: Modifier) =
    DesktopVerticalScrollbarAdapter(rememberScrollbarAdapter(scrollState), modifier)

@Composable
actual fun DesktopVerticalScrollbar(listState: LazyListState, modifier: Modifier) =
    DesktopVerticalScrollbarAdapter(rememberScrollbarAdapter(listState), modifier)

@Composable
private fun DesktopHorizontalScrollbarAdapter(adapter: ScrollbarAdapter, modifier: Modifier) {
    val canScroll = adapter.contentSize > adapter.viewportSize + 1.0
    if (!canScroll) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(SurfaceCardBorder.copy(alpha = 0.22f))
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .desktopPointerHand()
    ) {
        HorizontalScrollbar(
            adapter = adapter,
            style = rememberDesktopScrollbarStyle(isHorizontal = true)
        )
    }
}

@Composable
actual fun DesktopHorizontalScrollbar(scrollState: ScrollState, modifier: Modifier) =
    DesktopHorizontalScrollbarAdapter(rememberScrollbarAdapter(scrollState), modifier)

@Composable
actual fun DesktopHorizontalScrollbar(listState: LazyListState, modifier: Modifier) =
    DesktopHorizontalScrollbarAdapter(rememberScrollbarAdapter(listState), modifier)

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun DesktopTooltip(
    text: String,
    shortcut: String?,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    TooltipArea(
        tooltip = {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .background(SurfaceRaised)
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(Dimens.RadiusSm))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = text,
                    style = LogicLabsType.TechnicalXs,
                    color = TextPrimary
                )
                if (!shortcut.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(ChassisBase)
                            .border(Dimens.Hairline, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = shortcut,
                            style = LogicLabsType.TechnicalXs.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = AccentCyan
                        )
                    }
                }
            }
        },
        modifier = modifier,
        delayMillis = 360,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 18.dp)),
        content = content
    )
}
