package com.logiclabs.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logiclabs.app.ui.component.DipPackageDiagram
import com.logiclabs.app.ui.component.dipPinRoleInfo
import com.logiclabs.app.ui.screens.home.buildIcChipSpec
import com.logiclabs.core.bridge.catalog.TTLChipCatalog
import com.logiclabs.core.bridge.model.PinDefinition
import com.logiclabs.core.bridge.model.PinRole
import com.logiclabs.core.designsystem.component.DesktopModalDialog
import com.logiclabs.core.designsystem.component.DesktopVerticalScrollbar
import com.logiclabs.core.designsystem.component.LogicIcons
import com.logiclabs.core.designsystem.component.SheetScaffold
import com.logiclabs.core.designsystem.component.keyboardPageScroll
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.BusGnd
import com.logiclabs.core.designsystem.theme.BusVcc
import com.logiclabs.core.designsystem.theme.ChassisBase
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.GlassHairline
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.PhosphorCore
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.SurfaceCardBorder
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextSecondary
import com.logiclabs.core.designsystem.theme.TextTertiary

@Composable
fun IcPinoutInspectorSheet(
    partNumber: String,
    onDismiss: () -> Unit
) {
    val model = TTLChipCatalog.getInfo(partNumber) ?: return
    val listState = rememberLazyListState()

    DesktopModalDialog(
        onDismissRequest = onDismiss,
        maxWidth = 840.dp,
        maxHeight = 700.dp
    ) {
        val displayPart = if (partNumber.startsWith("74")) partNumber else "74$partNumber"
        SheetScaffold(
            title = "IC $displayPart PINOUT & SCHEMATIC",
            subtitle = "${model.description} · ${model.pinCount}-Pin Dual In-Line Package (DIP)",
            titleIcon = LogicIcons.Chip,
            onDismiss = onDismiss
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .keyboardPageScroll(listState)
            ) {
                val twoColumns = maxWidth >= 600.dp
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = Dimens.Space3),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
                ) {
                    item {
                        PhysicalDIPGraphic(
                            partNumber = model.partNumber,
                            pinCount = model.pinCount,
                            pins = model.pins
                        )
                    }

                    item {
                        InternalSchematicCard(
                            partNumber = model.partNumber,
                            pins = model.pins,
                            twoColumns = twoColumns
                        )
                    }

                    item {
                        PinoutTableCard(
                            pinCount = model.pinCount,
                            pins = model.pins,
                            twoColumns = twoColumns
                        )
                    }
                }

                DesktopVerticalScrollbar(
                    listState = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun PhysicalDIPGraphic(
    partNumber: String,
    pinCount: Int,
    pins: Map<Int, PinDefinition>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusLg))
            .background(SurfaceCard)
            .border(Dimens.Hairline, GlassHairline, RoundedCornerShape(Dimens.RadiusLg))
            .padding(Dimens.Space3),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PHYSICAL DIP-$pinCount PINOUT ORIENTATION",
            style = LogicLabsType.SwitchPlate,
            color = AccentCyan
        )
        Spacer(Modifier.height(Dimens.Space2))

        DipPackageDiagram(
            partNumber = partNumber,
            pinCount = pinCount,
            pinLabels = (1..pinCount).map { p -> pins[p]?.name ?: "$p" },
            pinRoles = (1..pinCount).map { p -> pins[p]?.role ?: PinRole.INPUT },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun InternalSchematicCard(
    partNumber: String,
    pins: Map<Int, PinDefinition>,
    twoColumns: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusLg))
            .background(SurfaceCard)
            .border(Dimens.Hairline, GlassHairline, RoundedCornerShape(Dimens.RadiusLg))
            .padding(Dimens.Space3)
    ) {
        Text(
            text = "INTERNAL LOGIC GATE SCHEMATIC",
            style = LogicLabsType.SwitchPlate,
            color = AccentCyan
        )
        Spacer(Modifier.height(Dimens.Space2))

        val gates = remember(partNumber) {
            buildIcChipSpec(partNumber)?.gateMappings ?: emptyList()
        }

        if (gates.isEmpty()) {
            Text(
                text = "Consult manufacturer datasheet for custom pin mapping",
                style = LogicLabsType.TechnicalXs,
                color = TextSecondary
            )
        } else {
            val rows = if (twoColumns) gates.chunked(2) else gates.map { listOf(it) }
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
                rows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
                    ) {
                        pair.forEach { mapping ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                                    .background(ChassisBase.copy(alpha = 0.65f))
                                    .border(Dimens.Hairline, SurfaceCardBorder, RoundedCornerShape(Dimens.RadiusSm))
                                    .padding(horizontal = Dimens.Space2, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mapping.title,
                                        style = LogicLabsType.TechnicalSm.copy(fontWeight = FontWeight.Bold),
                                        color = PhosphorCore,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (mapping.expression.isNotEmpty()) {
                                        Text(
                                            text = mapping.expression,
                                            style = LogicLabsType.TechnicalXs,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                if (mapping.inputPins.isNotEmpty()) {
                                    Text(
                                        text = "IN  " + mapping.inputPins.joinToString(" · ") { "${it.name} (P${it.pinNumber})" },
                                        style = LogicLabsType.TechnicalXs,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (mapping.outputPins.isNotEmpty()) {
                                    Text(
                                        text = "OUT " + mapping.outputPins.joinToString(" · ") { "${it.name} (P${it.pinNumber})" },
                                        style = LogicLabsType.TechnicalXs,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (twoColumns && pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Physical-Side Pinout Breakdown Table (2-column on desktop >=600dp, stacked 1-column on mobile). */
@Composable
private fun PinoutTableCard(
    pinCount: Int,
    pins: Map<Int, PinDefinition>,
    twoColumns: Boolean = true
) {
    val half = (pinCount + 1) / 2
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusLg))
            .background(SurfaceCard)
            .border(Dimens.Hairline, GlassHairline, RoundedCornerShape(Dimens.RadiusLg))
            .padding(Dimens.Space3)
    ) {
        Text(
            text = "PINOUT SPECIFICATION BREAKDOWN (PINS 1–$half  ·  PINS ${half + 1}–$pinCount)",
            style = LogicLabsType.SwitchPlate,
            color = AccentCyan
        )
        Spacer(Modifier.height(Dimens.Space2))

        if (twoColumns) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                PinSideColumn(
                    pinNumbers = (1..half).toList(),
                    pins = pins,
                    modifier = Modifier.weight(1f)
                )
                PinSideColumn(
                    pinNumbers = ((half + 1)..pinCount).toList(),
                    pins = pins,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                PinSideColumn(
                    pinNumbers = (1..half).toList(),
                    pins = pins,
                    modifier = Modifier.fillMaxWidth()
                )
                PinSideColumn(
                    pinNumbers = ((half + 1)..pinCount).toList(),
                    pins = pins,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PinSideColumn(
    pinNumbers: List<Int>,
    pins: Map<Int, PinDefinition>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChassisBase)
                .padding(horizontal = Dimens.Space2, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PIN", style = LogicLabsType.TechnicalXs, color = TextTertiary, modifier = Modifier.width(42.dp))
            Text("SIG", style = LogicLabsType.TechnicalXs, color = TextTertiary, modifier = Modifier.width(48.dp))
            Text("ROLE", style = LogicLabsType.TechnicalXs, color = TextTertiary, modifier = Modifier.width(68.dp))
            Text("FUNCTION", style = LogicLabsType.TechnicalXs, color = TextTertiary, modifier = Modifier.weight(1f))
        }

        pinNumbers.forEach { p ->
            val def = pins[p]
            val roleLabel = when (def?.role) {
                PinRole.INPUT -> "INPUT"
                PinRole.OUTPUT -> "OUTPUT"
                PinRole.POWER_VCC -> "+5V VCC"
                PinRole.POWER_GND -> "0V GND"
                PinRole.NO_CONNECT -> "NC"
                else -> "NC"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(Dimens.Hairline, SurfaceCardBorder)
                    .padding(horizontal = Dimens.Space2, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "P$p",
                    style = LogicLabsType.TechnicalXs.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.width(42.dp)
                )
                Text(
                    text = def?.name ?: "-",
                    style = LogicLabsType.TechnicalSm.copy(fontWeight = FontWeight.Bold),
                    color = dipPinRoleInfo(def?.role ?: PinRole.INPUT).second,
                    maxLines = 1,
                    modifier = Modifier.width(48.dp)
                )
                Text(
                    text = roleLabel,
                    style = LogicLabsType.TechnicalXs,
                    color = when (def?.role) {
                        PinRole.INPUT -> AccentCyan
                        PinRole.OUTPUT -> PhosphorCore
                        PinRole.POWER_VCC -> BusVcc
                        PinRole.POWER_GND -> BusGnd
                        else -> TextTertiary
                    },
                    maxLines = 1,
                    modifier = Modifier.width(68.dp)
                )
                Text(
                    text = when {
                        def?.role == PinRole.POWER_VCC -> "+5V DC supply"
                        def?.role == PinRole.POWER_GND -> "0V reference"
                        def?.isClock == true -> "Edge clock input"
                        def?.isInverted == true -> "Active-LOW input"
                        def?.role == PinRole.OUTPUT -> "Logic output"
                        def?.role == PinRole.INPUT -> "Logic input"
                        else -> "No connect"
                    },
                    style = LogicLabsType.TechnicalXs,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
