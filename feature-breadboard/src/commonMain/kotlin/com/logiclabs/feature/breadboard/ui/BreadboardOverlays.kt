package com.logiclabs.feature.breadboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.bridge.model.ElectricalLevel
import com.logiclabs.core.designsystem.component.DesktopTooltip
import com.logiclabs.core.designsystem.component.GlassSurface
import com.logiclabs.core.designsystem.component.LogicIcons
import com.logiclabs.core.designsystem.component.desktopPointerHand
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.AmberCore
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.GlassHairline
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.PhosphorCore
import com.logiclabs.core.designsystem.theme.ShortCircuitAlert
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextSecondary
import com.logiclabs.core.designsystem.theme.TextTertiary
import com.logiclabs.feature.breadboard.canvas.BreadboardGeometryMapper
import com.logiclabs.feature.breadboard.interaction.LoupeRenderer

/**
 * Compact glass pill zoom & loupe controls (+ / - / FIT / LOUPE) positioned in the top-right corner.
 */
@Composable
internal fun BoxScope.BreadboardControls(
    state: BreadboardCanvasState,
    mapper: BreadboardGeometryMapper,
    circuit: BreadboardCircuit? = null,
    isWireModeActive: Boolean = false,
    hasSelection: Boolean = false,
    onDeleteEquipment: (() -> Unit)? = null,
    onToggleWireMode: (() -> Unit)? = null
) {
    val loupeOn = state.loupeEnabled.value
    GlassSurface(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(end = Dimens.Space3, top = Dimens.Space3),
        shape = RoundedCornerShape(Dimens.RadiusPill)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DesktopTooltip(text = "Zoom out", shortcut = "-") {
                ZoomPillButton(label = "−", contentDescription = "Zoom out (-)") {
                    state.nudgeZoom(1f / 1.25f)
                }
            }
            DesktopTooltip(text = "Zoom in", shortcut = "+") {
                ZoomPillButton(label = "+", contentDescription = "Zoom in (+)") {
                    state.nudgeZoom(1.25f)
                }
            }
            DesktopTooltip(text = "Fit circuit to screen", shortcut = "F") {
                ZoomPillButton("FIT", isLabel = true) { state.applyFit(mapper, circuit) }
            }
            DesktopTooltip(text = "Toggle 2.2x optical loupe", shortcut = "L") {
                ZoomPillButton(
                    label = "2.2x",
                    isLabel = true,
                    highlighted = loupeOn
                ) {
                    val next = !state.loupeEnabled.value
                    state.loupeEnabled.value = next
                    if (!next) state.hideLoupe()
                }
            }
        }
    }
}

@Composable
private fun ZoomPillButton(
    label: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    contentDescription: String? = null,
    isLabel: Boolean = false,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(if (isLabel) 42.dp else 30.dp, 28.dp)
            .clip(RoundedCornerShape(Dimens.RadiusPill))
            .background(if (highlighted) AccentCyan.copy(alpha = 0.25f) else SurfaceCard.copy(alpha = 0.78f))
            .desktopPointerHand()
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = AccentCyan,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = label.orEmpty(),
                style = if (isLabel) LogicLabsType.SwitchPlate else LogicLabsType.TitleMd,
                color = if (highlighted) AccentCyan else if (isLabel) TextPrimary else AccentCyan
            )
        }
    }
}

/** Subtle live coordinate badge in the bottom-left while hovering any breadboard hole or terminal. */
@Composable
internal fun BoxScope.HoverReadoutPill(
    hoveredSocket: Int?,
    circuit: BreadboardCircuit
) {
    if (hoveredSocket == null) return
    val coord = LoupeRenderer.coordinateLabel(hoveredSocket) ?: return
    val lvl = circuit.getSocketLevel(hoveredSocket)
    val lvlText = when (lvl) {
        ElectricalLevel.HIGH -> "HIGH (5V)"
        ElectricalLevel.LOW -> "LOW (0V)"
        ElectricalLevel.CONTENTION -> "SHORT!"
        ElectricalLevel.HIGH_Z -> "FLOAT"
        ElectricalLevel.UNPOWERED -> "OFF"
        ElectricalLevel.BURNOUT -> "BURNOUT!"
    }
    val lvlColor: Color = when (lvl) {
        ElectricalLevel.HIGH -> PhosphorCore
        ElectricalLevel.LOW -> TextSecondary
        ElectricalLevel.CONTENTION -> ShortCircuitAlert
        ElectricalLevel.HIGH_Z -> AmberCore
        ElectricalLevel.UNPOWERED -> TextTertiary
        ElectricalLevel.BURNOUT -> ShortCircuitAlert
    }

    // Check if an IC pin sits on this column
    val chipKey = circuit.placedChips.map { it.placedIc }
    val pinNote = remember(hoveredSocket, chipKey) {
        var note: String? = null
        for (chip in circuit.placedChips) {
            for (p in 1..chip.model.pinCount) {
                if (circuit.dsu.areConnected(hoveredSocket, chip.getPinSocket(p))) {
                    val def = chip.model.pins[p]
                    if (def != null) {
                        note = "${chip.placedIc.partNumber} Pin $p (${def.name})"
                        break
                    }
                }
            }
            if (note != null) break
        }
        note
    }

    Card(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = Dimens.Space3, bottom = Dimens.Space3),
        shape = RoundedCornerShape(Dimens.RadiusPill),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.92f)),
        border = BorderStroke(Dimens.Hairline, GlassHairline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.Space3, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = coord, style = LogicLabsType.TechnicalSm, color = AccentCyan)
            if (pinNote != null) {
                Text(text = "·  $pinNote", style = LogicLabsType.TechnicalSm, color = TextPrimary)
            }
            Text(text = "·  $lvlText", style = LogicLabsType.TechnicalSm, color = lvlColor)
        }
    }
}

/** Hint strip shown while a click-to-connect wire is half-drawn. */
@Composable
internal fun BoxScope.TapToConnectHint(
    visible: Boolean,
    armedLabel: String? = null
) {
    if (!visible) return
    Card(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = Dimens.Space2),
        shape = RoundedCornerShape(Dimens.RadiusPill),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(Dimens.Hairline, AmberCore)
    ) {
        Text(
            text = if (armedLabel == null) {
                "Click a second hole to finish wire · Right-click or Esc to cancel"
            } else {
                "Armed $armedLabel — click a second hole to connect · Esc/Right-click to cancel"
            },
            color = AmberCore,
            style = LogicLabsType.BodySm,
            modifier = Modifier.padding(horizontal = Dimens.Space4, vertical = Dimens.Space1)
        )
    }
}

@Composable
internal fun BoxScope.EmptyBenchHint(visible: Boolean) {
    if (!visible) return
    Card(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = Dimens.Space4),
        shape = RoundedCornerShape(Dimens.RadiusPill),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.88f)),
        border = BorderStroke(Dimens.Hairline, GlassHairline)
    ) {
        Text(
            text = "Empty bench — press C to place an IC, W to wire, B for labs, or F1 for controls",
            color = TextSecondary,
            style = LogicLabsType.BodySm,
            modifier = Modifier.padding(horizontal = Dimens.Space4, vertical = Dimens.Space2)
        )
    }
}
