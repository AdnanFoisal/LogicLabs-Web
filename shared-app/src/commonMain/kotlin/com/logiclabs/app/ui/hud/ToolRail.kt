package com.logiclabs.app.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.logiclabs.app.ui.state.BoardMode
import com.logiclabs.core.designsystem.component.DesktopScrollArrowButton
import com.logiclabs.core.designsystem.component.LogicIcons
import com.logiclabs.core.designsystem.component.desktopPointerHand
import com.logiclabs.core.designsystem.component.horizontalWheelScroll
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.AmberCore
import com.logiclabs.core.designsystem.theme.ChassisBase
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.SurfaceCardBorder
import com.logiclabs.core.designsystem.theme.TextSecondary
import com.logiclabs.core.designsystem.theme.TextTertiary
import kotlinx.coroutines.launch

/** The actions the rail exposes. */
enum class ToolAction {
    CHIPS,
    SCOPE,
    LABS,
    ROAM,
    FIT_VIEW,
    ROTATE,
    CLEAR,
    DIAGRAM
}

/**
 * Top Horizontal Bench Tool Ribbon + Telemetry Status Bar.
 *
 * Compact desktop pill sizing fits all 10 bench tools cleanly on 1280dp (1600x960 @ 125% DPI)
 * and 1366x768 screens. On narrower split widths, translates vertical mouse wheel to horizontal
 * scroll and surfaces subtle ‹ / › scroll chevron buttons automatically.
 */
@Composable
fun ToolRail(
    boardMode: BoardMode,
    onAction: (ToolAction) -> Unit,
    onModeChange: (BoardMode) -> Unit,
    deleteLabel: String,
    simulationModeLabel: String = "PRACTICAL",
    chipCount: Int = 0,
    wireCount: Int = 0,
    zoomPercent: Int = 100,
    diagramActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val statusScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val overflows = scrollState.maxValue > 0

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val isCompact = maxWidth < 680.dp
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Horizontal Tool Ribbon with automatic wheel-scroll & overflow chevrons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.CommandBarInset, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (overflows) {
                    DesktopScrollArrowButton(
                        isLeft = true,
                        enabled = scrollState.value > 0,
                        onClick = { scope.launch { scrollState.animateScrollTo((scrollState.value - 180).coerceAtLeast(0)) } }
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalWheelScroll(scrollState)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolRibbonPill(
                        icon = LogicIcons.Roam,
                        label = "ROAM",
                        shortcut = if (isCompact) null else "V",
                        isActive = boardMode == BoardMode.ROAM,
                        onClick = { onModeChange(BoardMode.ROAM) }
                    )

                    ToolRibbonPill(
                        icon = LogicIcons.WireDraw,
                        label = "WIRE",
                        shortcut = if (isCompact) null else "W",
                        isActive = boardMode == BoardMode.WIRE_DRAW,
                        onClick = {
                            onModeChange(
                                if (boardMode == BoardMode.WIRE_DRAW) BoardMode.ROAM else BoardMode.WIRE_DRAW
                            )
                        }
                    )

                    ToolRibbonPill(
                        icon = LogicIcons.WireSelect,
                        label = "SELECT",
                        shortcut = if (isCompact) null else "S",
                        isActive = boardMode == BoardMode.WIRE_SELECT,
                        onClick = {
                            onModeChange(
                                if (boardMode == BoardMode.WIRE_SELECT) BoardMode.ROAM else BoardMode.WIRE_SELECT
                            )
                        }
                    )

                    ToolRibbonPill(
                        icon = LogicIcons.Chip,
                        label = "ICs",
                        shortcut = if (isCompact) null else "C",
                        isActive = false,
                        onClick = { onAction(ToolAction.CHIPS) }
                    )

                    ToolRibbonPill(
                        icon = LogicIcons.Scope,
                        label = "SCOPE",
                        shortcut = if (isCompact) null else "O",
                        isActive = false,
                        onClick = { onAction(ToolAction.SCOPE) }
                    )

                    ToolRibbonPill(
                        icon = LogicIcons.Schematic,
                        label = "LOGIC",
                        shortcut = if (isCompact) null else "D",
                        isActive = diagramActive,
                        onClick = { onAction(ToolAction.DIAGRAM) }
                    )

                    ToolRibbonPill(
                        icon = LogicIcons.Labs,
                        label = "LABS",
                        shortcut = if (isCompact) null else "B",
                        isActive = false,
                        onClick = { onAction(ToolAction.LABS) }
                    )

                    ToolRibbonPill(
                        icon = LogicIcons.FitBoard,
                        label = "FIT",
                        shortcut = if (isCompact) null else "F",
                        isActive = false,
                        onClick = { onAction(ToolAction.FIT_VIEW) }
                    )

                    ToolRibbonPill(
                        icon = LogicIcons.Rotate,
                        label = "ROTATE",
                        shortcut = if (isCompact) null else "R",
                        isActive = false,
                        onClick = { onAction(ToolAction.ROTATE) }
                    )

                    ToolRibbonPill(
                        icon = LogicIcons.Trash,
                        label = deleteLabel,
                        shortcut = if (isCompact) null else "Del",
                        isActive = false,
                        onClick = { onAction(ToolAction.CLEAR) }
                    )
                }

                if (overflows) {
                    DesktopScrollArrowButton(
                        isLeft = false,
                        enabled = scrollState.value < scrollState.maxValue,
                        onClick = { scope.launch { scrollState.animateScrollTo((scrollState.value + 180).coerceAtMost(scrollState.maxValue)) } }
                    )
                }
            }

            // 2. Slim Telemetry Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChassisBase)
                    .horizontalWheelScroll(statusScrollState)
                    .horizontalScroll(statusScrollState)
                    .padding(horizontal = Dimens.CommandBarInset + 2.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ELEMENTS: $chipCount ICs, $wireCount WIRES",
                    style = LogicLabsType.SwitchPlate,
                    color = AccentCyan,
                    softWrap = false
                )
                Text(
                    text = "MODE: $simulationModeLabel",
                    style = LogicLabsType.SwitchPlate,
                    color = AmberCore,
                    softWrap = false
                )
                Text(
                    text = "ZOOM: $zoomPercent%",
                    style = LogicLabsType.SwitchPlate,
                    color = if (zoomPercent > 102) AccentCyan else TextSecondary,
                    softWrap = false
                )
                Text(
                    text = if (isCompact) {
                        "PINCH OR +/− TO ZOOM · DOUBLE-TAP IC FOR PINOUT"
                    } else {
                        "2-FINGER SCROLL / PINCH TO ZOOM · RIGHT-CLICK MENU · DOUBLE-CLICK IC · F1 KEYS"
                    },
                    style = LogicLabsType.SwitchPlate,
                    color = TextTertiary,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun ToolRibbonPill(
    icon: ImageVector,
    label: String,
    shortcut: String? = null,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(7.dp)
    val bgColor = if (isActive) AmberCore else SurfaceCard
    val contentColor = if (isActive) ChassisBase else TextSecondary
    val borderColor = if (isActive) AmberCore else SurfaceCardBorder

    Row(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .desktopPointerHand()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = contentColor
        )
        Text(
            text = label,
            style = LogicLabsType.SwitchPlate,
            color = contentColor,
            softWrap = false
        )
        if (shortcut != null) {
            Text(
                text = "[$shortcut]",
                style = LogicLabsType.TechnicalXs,
                color = if (isActive) ChassisBase.copy(alpha = 0.8f) else TextTertiary,
                softWrap = false
            )
        }
    }
}
