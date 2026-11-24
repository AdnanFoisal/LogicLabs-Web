package com.logiclabs.feature.instruments.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.bridge.model.SimulationMode
import com.logiclabs.core.designsystem.component.DesktopVerticalScrollbar
import com.logiclabs.core.designsystem.component.desktopPointerHand
import com.logiclabs.core.designsystem.component.keyboardPageScroll
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.AmberCore
import com.logiclabs.core.designsystem.theme.ChassisSilkscreen
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.PhosphorBloom
import com.logiclabs.core.designsystem.theme.ShortCircuitAlert
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.SurfaceCardBorder
import com.logiclabs.core.designsystem.theme.SwitchWell
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextSecondary
import com.logiclabs.core.designsystem.theme.TextTertiary
import com.logiclabs.feature.instruments.ui.hardware.ChassisSurface
import com.logiclabs.feature.instruments.ui.hardware.ClockDial
import com.logiclabs.feature.instruments.ui.hardware.CornerScrews
import com.logiclabs.feature.instruments.ui.hardware.DiagnosticsStrip
import com.logiclabs.feature.instruments.ui.hardware.LedLens
import com.logiclabs.feature.instruments.ui.hardware.MasterPowerRocker
import com.logiclabs.feature.instruments.ui.hardware.MomentaryPulser
import com.logiclabs.feature.instruments.ui.hardware.PanelHaptics
import com.logiclabs.feature.instruments.ui.hardware.RockerSwitch
import com.logiclabs.feature.instruments.ui.hardware.SectionBlock
import com.logiclabs.feature.instruments.ui.hardware.SevenSegModule
import com.logiclabs.feature.instruments.ui.hardware.recessedWell
import kotlinx.coroutines.delay

private class ConsoleSnapshot(circuit: BreadboardCircuit) {
    var masterPower by mutableStateOf(circuit.masterPower)
    val switches: SnapshotStateList<Boolean> = circuit.switches.toList().toMutableStateList()
    var pulserA by mutableStateOf(circuit.pulserAPressed)
    var pulserB by mutableStateOf(circuit.pulserBPressed)
    var clockFrequencyHz by mutableStateOf(circuit.clockFrequencyHz)
    var clockRunning by mutableStateOf(circuit.clockRunning)
    var clockState by mutableStateOf(circuit.clockState)
    val leds: SnapshotStateList<Boolean> = circuit.ledValues.toList().toMutableStateList()
    val ledDuty: SnapshotStateList<Float> = circuit.ledPovDutyCycles.toList().toMutableStateList()
    val seg: SnapshotStateList<Int> = circuit.seg7Digits.toList().toMutableStateList()
    var contention by mutableStateOf(circuit.contentionDetected)
    var reversePolarity by mutableStateOf(circuit.isReversePolarityBurned)
    var oscillationClamped by mutableStateOf(circuit.isOscillationClamped)
    var hasBurnedChips by mutableStateOf(circuit.burnedChipIds.isNotEmpty() || circuit.isReversePolarityBurned)
    var simulationMode by mutableStateOf(circuit.simulationMode)

    fun sync(circuit: BreadboardCircuit) {
        masterPower = circuit.masterPower
        pulserA = circuit.pulserAPressed
        pulserB = circuit.pulserBPressed
        clockFrequencyHz = circuit.clockFrequencyHz
        clockRunning = circuit.clockRunning
        clockState = circuit.clockState
        for (i in 0 until 8) {
            if (switches[i] != circuit.switches[i]) switches[i] = circuit.switches[i]
            if (leds[i] != circuit.ledValues[i]) leds[i] = circuit.ledValues[i]
            if (ledDuty[i] != circuit.ledPovDutyCycles[i]) ledDuty[i] = circuit.ledPovDutyCycles[i]
        }
        for (i in 0 until 2) {
            if (seg[i] != circuit.seg7Digits[i]) seg[i] = circuit.seg7Digits[i]
        }
        contention = circuit.contentionDetected
        reversePolarity = circuit.isReversePolarityBurned
        oscillationClamped = circuit.isOscillationClamped
        hasBurnedChips = circuit.burnedChipIds.isNotEmpty() || circuit.isReversePolarityBurned
        simulationMode = circuit.simulationMode
    }
}

private const val MaxUiTickHz = 60.0

/**
 * Desktop K&H IDL-800A Trainer Console Rack.
 * Displays all hardware I/O controls (Power, Pulsers A/B, LEDs L7..L0, Switches SW7..SW0,
 * Clock Generator with single-step, Dual 7-Segment Hex Displays, and Diagnostics) in a clean
 * resizable right-hand workstation column with keyboard shortcut badges.
 */
@Composable
fun TrainerConsoleDock(
    circuit: BreadboardCircuit,
    circuitVersion: Int = 0,
    renderVersion: Int = 0,
    onCircuitChanged: () -> Unit,
    onCollapseClick: (() -> Unit)? = null,
    showTitleBar: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snapshot = remember(circuit) { ConsoleSnapshot(circuit) }

    LaunchedEffect(circuitVersion, renderVersion) { snapshot.sync(circuit) }

    fun commit(mutate: () -> Unit) {
        mutate()
        circuit.step()
        snapshot.sync(circuit)
        onCircuitChanged()
    }

    // Clock driver coroutine (capped at 60 Hz UI refresh)
    LaunchedEffect(snapshot.clockRunning, snapshot.clockFrequencyHz, snapshot.masterPower) {
        val hz = snapshot.clockFrequencyHz
        if (!snapshot.clockRunning || !snapshot.masterPower || hz <= 0.0) return@LaunchedEffect
        val effectiveHz = if (hz > MaxUiTickHz) MaxUiTickHz else hz
        val halfPeriodMs = (1000.0 / (effectiveHz * 2.0)).toLong().coerceAtLeast(8L)
        while (true) {
            delay(halfPeriodMs)
            circuit.clockState = !circuit.clockState
            circuit.step()
            snapshot.sync(circuit)
            onCircuitChanged()
        }
    }

    ChassisSurface(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(min = 300.dp)
            .clip(
                if (showTitleBar) RoundedCornerShape(topStart = Dimens.RadiusLg, bottomStart = Dimens.RadiusLg)
                else RoundedCornerShape(0.dp)
            )
    ) {
        CornerScrews(modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            ConsoleHeader(
                isLive = snapshot.masterPower,
                hasFault = snapshot.contention || snapshot.reversePolarity || snapshot.hasBurnedChips,
                leds = snapshot.leds,
                ledDuty = snapshot.ledDuty,
                switches = snapshot.switches,
                onToggleSwitch = { i, on -> commit { circuit.switches[i] = on } },
                onCollapseClick = onCollapseClick,
                showTitleBar = showTitleBar
            )

            // Rack view filter tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.Space3, vertical = Dimens.Space1),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("ALL", "I/O", "CLOCK/HEX", "DIAG")
                tabs.forEachIndexed { i, title ->
                    val selected = selectedTab == i
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Dimens.RadiusPill))
                            .background(if (selected) AccentCyan.copy(alpha = 0.18f) else SurfaceCard)
                            .border(
                                width = if (selected) 1.dp else Dimens.Hairline,
                                color = if (selected) AccentCyan else SurfaceCardBorder,
                                shape = RoundedCornerShape(Dimens.RadiusPill)
                            )
                            .clickable(role = Role.Tab) {
                                if (selectedTab != i) {
                                    PanelHaptics.tick()
                                    selectedTab = i
                                }
                            }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = LogicLabsType.SwitchPlate,
                            color = if (selected) AccentCyan else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Scrollable Rack Body with visible hardware-themed desktop scrollbar
            val dockScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .keyboardPageScroll(dockScrollState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(dockScrollState)
                        .padding(start = Dimens.Space3, end = Dimens.Space4, top = Dimens.Space2, bottom = Dimens.Space3),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    if (selectedTab == 0 || selectedTab == 1) {
                        // Power + Pulsers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Space3),
                            verticalAlignment = Alignment.Top
                        ) {
                            SectionBlock(label = "Power [P]", modifier = Modifier.weight(0.95f)) {
                                MasterPowerRocker(
                                    isOn = snapshot.masterPower,
                                    onToggle = { on -> commit { circuit.masterPower = on } },
                                )
                            }
                            SectionBlock(label = "Pulsers [Q / E]", modifier = Modifier.weight(1.25f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
                                    MomentaryPulser(
                                        label = "PLS A (Q)",
                                        isPressed = snapshot.pulserA,
                                        onPressChanged = { p -> commit { circuit.pulserAPressed = p } },
                                    )
                                    MomentaryPulser(
                                        label = "PLS B (E)",
                                        isPressed = snapshot.pulserB,
                                        onPressChanged = { p -> commit { circuit.pulserBPressed = p } },
                                    )
                                }
                            }
                        }

                        // Logic Indicators L7..L0
                        SectionBlock(label = "Logic Indicators (L7..L0)") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .recessedWell(fill = SwitchWell)
                                    .padding(vertical = Dimens.Space1),
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                            ) {
                                for (i in 7 downTo 0) {
                                    LedLens(
                                        isOn = snapshot.leds[i],
                                        dutyCycle = snapshot.ledDuty[i],
                                        label = "L$i",
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }

                        // Data Switches SW7..SW0 with explicit [8]..[1] keycap hints beneath each switch
                        SectionBlock(label = "Data Switches (SW7..SW0 · Keys 8..1)") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .recessedWell(fill = SwitchWell)
                                    .padding(top = Dimens.Space1, bottom = Dimens.Space1),
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                            ) {
                                for (i in 7 downTo 0) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        RockerSwitch(
                                            isOn = snapshot.switches[i],
                                            label = "SW$i",
                                            onToggle = { on -> commit { circuit.switches[i] = on } },
                                            modifier = Modifier.fillMaxWidth().desktopPointerHand(),
                                        )
                                        Text(
                                            text = "[${i + 1}]",
                                            style = LogicLabsType.TechnicalXs,
                                            color = if (snapshot.switches[i]) AccentCyan else TextTertiary.copy(alpha = 0.72f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTab == 0 || selectedTab == 2) {
                        // Clock + Single Step + 7-Segment Displays
                        SectionBlock(label = "Clock Generator [K: Run · .: Step]") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ClockDial(
                                    frequencyHz = snapshot.clockFrequencyHz,
                                    isRunning = snapshot.clockRunning,
                                    clockState = snapshot.clockState,
                                    onFrequencyChange = { hz -> commit { circuit.clockFrequencyHz = hz } },
                                    onRunningChange = { run -> commit { circuit.clockRunning = run } },
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(Dimens.RadiusSm))
                                        .background(SurfaceCard)
                                        .border(Dimens.Hairline, AccentCyan.copy(alpha = 0.6f), RoundedCornerShape(Dimens.RadiusSm))
                                        .desktopPointerHand()
                                        .clickable(role = Role.Button) {
                                            PanelHaptics.tick()
                                            commit {
                                                circuit.clockRunning = false
                                                circuit.clockState = !circuit.clockState
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("STEP", style = LogicLabsType.SwitchPlate, color = AccentCyan)
                                        Text("[.]", style = LogicLabsType.TechnicalXs, color = TextTertiary)
                                    }
                                }
                            }
                        }

                        SectionBlock(label = "7-Segment BCD Displays") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.Space3, Alignment.CenterHorizontally)
                            ) {
                                SevenSegModule(value = snapshot.seg[0], label = "HEX A")
                                SevenSegModule(value = snapshot.seg[1], label = "HEX B")
                            }
                        }
                    }

                if (selectedTab == 0 || selectedTab == 3) {
                    // Diagnostics & Burned IC recovery
                    SectionBlock(label = "Trainer Diagnostic Monitor") {
                        DiagnosticsStrip(
                            contention = snapshot.contention,
                            reversePolarity = snapshot.reversePolarity,
                            oscillationClamped = snapshot.oscillationClamped,
                            burnedChip = snapshot.hasBurnedChips,
                            modifier = Modifier
                                .fillMaxWidth()
                                .recessedWell(fill = SwitchWell, cornerRadius = Dimens.RadiusSm)
                                .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
                        )
                    }

                    if (snapshot.hasBurnedChips) {
                        SectionBlock(label = "IC Silicon Damage Detected") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .recessedWell(fill = Color(0xFF1E0808), cornerRadius = Dimens.RadiusSm)
                                    .border(Dimens.Hairline, ShortCircuitAlert.copy(alpha = 0.6f), RoundedCornerShape(Dimens.RadiusSm))
                                    .padding(Dimens.Space3),
                                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                            ) {
                                Text(
                                    text = "FATAL: IC DESTROYED (PRACTICAL MODE)",
                                    style = LogicLabsType.TechnicalSm,
                                    color = ShortCircuitAlert,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Reverse polarity or active gate contention exceeded thermal limits. IC outputs are floating in High-Z.",
                                    style = LogicLabsType.TechnicalXs,
                                    color = TextSecondary
                                )
                                Button(
                                    onClick = { commit { circuit.restoreBurnedChips() } },
                                    colors = ButtonDefaults.buttonColors(containerColor = ShortCircuitAlert),
                                    shape = RoundedCornerShape(Dimens.RadiusSm),
                                    modifier = Modifier.fillMaxWidth().height(34.dp)
                                ) {
                                    Text(
                                        text = "REPLACE DESTROYED IC(S)",
                                        style = LogicLabsType.TechnicalSm,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    SectionBlock(label = "Supply & Physics Status") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .recessedWell(fill = SwitchWell, cornerRadius = Dimens.RadiusSm)
                                .padding(Dimens.Space2),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (snapshot.masterPower) "+5.0V DC ACTIVE" else "0.0V POWER OFF",
                                    style = LogicLabsType.TechnicalSm,
                                    color = if (snapshot.masterPower) PhosphorBloom else TextTertiary
                                )
                                Text(
                                    text = "MODE: ${snapshot.simulationMode.name}",
                                    style = LogicLabsType.TechnicalSm,
                                    color = if (snapshot.simulationMode == SimulationMode.PRACTICAL) AmberCore else AccentCyan
                                )
                            }
                        }
                    }
                }
                }
                DesktopVerticalScrollbar(
                    scrollState = dockScrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = Dimens.Space2, horizontal = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun ConsoleHeader(
    isLive: Boolean,
    hasFault: Boolean,
    leds: List<Boolean>,
    ledDuty: List<Float>,
    switches: List<Boolean>,
    onToggleSwitch: (Int, Boolean) -> Unit,
    onCollapseClick: (() -> Unit)?,
    showTitleBar: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (showTitleBar) Dimens.Space2 else 4.dp, bottom = Dimens.Space1),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showTitleBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.Space3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusDot(isLive = isLive, hasFault = hasFault)
                    Spacer(modifier = Modifier.width(Dimens.Space2))
                    Text(
                        text = "K&H IDL-800A TRAINER",
                        style = LogicLabsType.TechnicalSm,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onCollapseClick != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.RadiusPill))
                            .background(SurfaceCard)
                            .border(Dimens.Hairline, SurfaceCardBorder, RoundedCornerShape(Dimens.RadiusPill))
                            .clickable(role = Role.Button, onClick = onCollapseClick)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("HIDE", style = LogicLabsType.TechnicalXs, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        PersistentMicroIoStrip(
            leds = leds,
            ledDuty = ledDuty,
            switches = switches,
            onToggleSwitch = onToggleSwitch
        )
    }
}

@Composable
private fun PersistentMicroIoStrip(
    leds: List<Boolean>,
    ledDuty: List<Float>,
    switches: List<Boolean>,
    onToggleSwitch: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space3, vertical = 2.dp)
            .recessedWell(fill = SwitchWell, cornerRadius = Dimens.RadiusSm)
            .padding(horizontal = Dimens.Space2, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 7 downTo 0) {
                MicroLedItem(
                    index = i,
                    isOn = leds.getOrElse(i) { false },
                    dutyCycle = ledDuty.getOrElse(i) { 0f },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 7 downTo 0) {
                val isOn = switches.getOrElse(i) { false }
                MicroSwitchItem(
                    index = i,
                    isOn = isOn,
                    onToggle = { nextState ->
                        if (!isOn && nextState) PanelHaptics.tick()
                        onToggleSwitch(i, nextState)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MicroLedItem(
    index: Int,
    isOn: Boolean,
    dutyCycle: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(7.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f
            if (isOn || dutyCycle > 0.05f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PhosphorBloom.copy(alpha = 0.5f), Color.Transparent),
                        center = c,
                        radius = r * 2.2f
                    ),
                    radius = r * 2.2f,
                    center = c
                )
                drawCircle(color = PhosphorBloom, radius = r, center = c)
            } else {
                drawCircle(color = Color(0xFF14241B), radius = r, center = c)
            }
            drawCircle(color = Color(0xFF2A2D35), radius = r, center = c, style = Stroke(0.6.dp.toPx()))
        }
        Text(
            text = "L$index",
            style = LogicLabsType.TechnicalXs.copy(fontSize = 7.sp, lineHeight = 8.sp),
            color = if (isOn) PhosphorBloom else ChassisSilkscreen
        )
    }
}

@Composable
private fun MicroSwitchItem(
    index: Int,
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isOn) AccentCyan.copy(alpha = 0.22f) else SurfaceCard)
            .border(
                width = 0.75.dp,
                color = if (isOn) AccentCyan else SurfaceCardBorder,
                shape = RoundedCornerShape(3.dp)
            )
            .clickable(role = Role.Switch) { onToggle(!isOn) }
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "S$index",
            style = LogicLabsType.TechnicalXs.copy(fontSize = 7.5.sp, lineHeight = 9.sp),
            color = if (isOn) AccentCyan else TextSecondary
        )
    }
}

@Composable
private fun StatusDot(isLive: Boolean, hasFault: Boolean) {
    val dotColor = when {
        hasFault -> ShortCircuitAlert
        isLive -> PhosphorBloom
        else -> TextTertiary
    }
    Canvas(modifier = Modifier.size(9.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        if (isLive || hasFault) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(dotColor.copy(alpha = 0.55f), Color.Transparent),
                    center = c,
                    radius = r * 2.1f
                ),
                radius = r * 2.1f,
                center = c
            )
        }
        drawCircle(color = dotColor, radius = r * 0.72f, center = c)
    }
}
