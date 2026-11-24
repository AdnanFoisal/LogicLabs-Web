package com.logiclabs.feature.breadboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.bridge.model.WireColor
import com.logiclabs.core.designsystem.component.webNativeZoomTarget
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.WireSpecular
import com.logiclabs.core.designsystem.theme.wireColorOf
import com.logiclabs.feature.breadboard.canvas.BoardCache
import com.logiclabs.feature.breadboard.canvas.BreadboardGeometryMapper
import com.logiclabs.feature.breadboard.canvas.DipPainter
import com.logiclabs.feature.breadboard.canvas.FaceplateRenderer
import com.logiclabs.feature.breadboard.canvas.WirePainter
import com.logiclabs.feature.breadboard.interaction.LoupeRenderer
import com.logiclabs.feature.breadboard.interaction.SocketHitTester
import com.logiclabs.feature.breadboard.physics.BezierWireGeometry

/**
 * Desktop-ready breadboard viewport: pan/zoomable AD-200 faceplate with mouse wheel zoom,
 * hover targeting, live armed-wire preview, right-click context menus, toggleable 2.2x loupe,
 * and Skia Picture faceplate caching.
 */
@Composable
fun BreadboardCanvas(
    circuit: BreadboardCircuit,
    modifier: Modifier = Modifier,
    state: BreadboardCanvasState = remember { BreadboardCanvasState() },
    activeWireColor: WireColor = WireColor.RED,
    defaultWireManhattan: Boolean = false,
    isWireModeActive: Boolean = false,
    isWireSelectMode: Boolean = false,
    selectedWireId: String? = null,
    selectedChipId: String? = null,
    circuitVersion: Int = 0,
    renderVersion: Int = 0,
    resetFitTrigger: Int = 0,
    zoomCaptureEnabled: Boolean = true,
    onWireSelected: ((String?) -> Unit)? = null,
    onChipSelected: ((String?) -> Unit)? = null,
    onChipDoubleClicked: ((String) -> Unit)? = null,
    onContextMenu: ((BreadboardContextMenuRequest) -> Unit)? = null,
    onBeforeTopologyMutation: (() -> Unit)? = null,
    onDeleteEquipment: (() -> Unit)? = null,
    onToggleWireMode: (() -> Unit)? = null,
    onSocketSelected: ((Int) -> Unit)? = null,
    onCircuitChanged: (() -> Unit)? = null
) {
    val density = LocalDensity.current.density
    val mapper = remember { BreadboardGeometryMapper() }
    val hitTester = remember(mapper) { SocketHitTester(mapper) }
    val boardCache = remember { BoardCache() }
    val guidePath = remember { Path() }

    val onWire by rememberUpdatedState(onWireSelected)
    val onChip by rememberUpdatedState(onChipSelected)
    val onChipDbl by rememberUpdatedState(onChipDoubleClicked)
    val onCtxMenu by rememberUpdatedState(onContextMenu)
    val onBeforeMut by rememberUpdatedState(onBeforeTopologyMutation)
    val onChanged by rememberUpdatedState(onCircuitChanged)
    val onSocket by rememberUpdatedState(onSocketSelected)

    val callbacks = remember {
        BreadboardGestureCallbacks(
            onWireSelected = { onWire?.invoke(it) },
            onChipSelected = { onChip?.invoke(it) },
            onCircuitChanged = { onChanged?.invoke() },
            onSocketSelected = { onSocket?.invoke(it) },
            onBeforeTopologyMutation = { onBeforeMut?.invoke() },
            onChipDoubleClicked = { onChipDbl?.invoke(it) },
            onContextMenu = { onCtxMenu?.invoke(it) }
        )
    }

    DisposableEffect(Unit) { onDispose { boardCache.release() } }

    LaunchedEffect(selectedWireId, circuitVersion) {
        state.recomputeHighlights(mapper, circuit, selectedWireId)
    }
    LaunchedEffect(isWireSelectMode, isWireModeActive) {
        state.clearDraft()
        state.recomputeHighlights(mapper, circuit, selectedWireId)
    }
    LaunchedEffect(resetFitTrigger) {
        if (resetFitTrigger > 0) {
            state.resetFit(circuit)
        }
    }

    state.renderVersion.value = renderVersion
    state.circuitVersion.value = circuitVersion

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val vw = with(LocalDensity.current) { maxWidth.toPx() }
        val vh = with(LocalDensity.current) { maxHeight.toPx() }
        LaunchedEffect(vw, vh) { state.onViewportMeasured(Size(vw, vh), mapper, circuit) }

        val hasActiveExplosion = remember(circuit.chipExplosionTimestamps.size, state.renderVersion.value) {
            val now = com.logiclabs.core.bridge.platform.currentTimeMillis()
            circuit.chipExplosionTimestamps.values.any { now - it < 1600L }
        }
        var explosionTick by remember { mutableStateOf(0L) }
        if (hasActiveExplosion) {
            LaunchedEffect(hasActiveExplosion, circuit.chipExplosionTimestamps.size) {
                val start = com.logiclabs.core.bridge.platform.currentTimeMillis()
                while (com.logiclabs.core.bridge.platform.currentTimeMillis() - start < 1600L) {
                    withFrameMillis { frameTime -> explosionTick = frameTime }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .webNativeZoomTarget(enabled = zoomCaptureEnabled) { zoom, panDx, panDy, lx, ly ->
                    state.applyPinch(zoom, Offset(panDx, panDy), Offset(lx, ly))
                }
                .breadboardGestures(
                    state = state,
                    mapper = mapper,
                    hitTester = hitTester,
                    circuit = circuit,
                    density = density,
                    activeWireColor = activeWireColor,
                    defaultWireManhattan = defaultWireManhattan,
                    isWireModeActive = isWireModeActive,
                    isWireSelectMode = isWireSelectMode,
                    selectedWireId = selectedWireId,
                    selectedChipId = selectedChipId,
                    callbacks = callbacks
                )
        ) {
            // Layer 1: Static AD-200 faceplate bitmap blit — only redraws on pan/zoom/theme change.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scale = state.scale.value
                val pan = state.pan.value
                withTransform({
                    translate(pan.x, pan.y)
                    scale(scale, scale, Offset.Zero)
                }) {
                    boardCache.drawStatic(this, mapper, density, scale)
                }
            }

            // Layer 2: Dynamic circuit layer (terminal LEDs, highlighted nets, DIP ICs, jumper wires).
            // Never invalidated by mouse-hover socket tracking or rubber-band draft wire motion.
            Canvas(modifier = Modifier.fillMaxSize()) {
                @Suppress("UNUSED_EXPRESSION") state.circuitVersion.value
                @Suppress("UNUSED_EXPRESSION") state.renderVersion.value
                @Suppress("UNUSED_EXPRESSION") explosionTick
                val scale = state.scale.value
                val pan = state.pan.value
                val viewport = visibleBoardRect(size, pan, scale)

                withTransform({
                    translate(pan.x, pan.y)
                    scale(scale, scale, Offset.Zero)
                }) {
                    FaceplateRenderer.drawDynamic(
                        scope = this,
                        mapper = mapper,
                        circuit = circuit,
                        density = density,
                        zoomScale = scale,
                        highlightSockets = state.highlights.value,
                        viewport = viewport
                    )

                    for (chip in circuit.placedChips) {
                        DipPainter.draw(
                            scope = this,
                            mapper = mapper,
                            chip = chip,
                            density = density,
                            zoomScale = scale,
                            isBurned = chip.placedIc.id in circuit.burnedChipIds ||
                                (circuit.isReversePolarityBurned && circuit.burnedChipId == chip.placedIc.id),
                            isSelected = chip.placedIc.id == selectedChipId,
                            explosionTimestamp = circuit.chipExplosionTimestamps[chip.placedIc.id] ?: 0L
                        )
                    }

                    WirePainter.draw(this, mapper, circuit, selectedWireId, viewport)
                }
            }

            // Layer 3: Ultra-lightweight interactive pointer overlay (hover ring, draft wire, optional loupe).
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scale = state.scale.value
                val pan = state.pan.value
                val hovered = state.hoveredSocket.value
                val loupeState = state.loupe.value
                val hasDraft = state.draftAnchor.value != null

                if (hovered != null || hasDraft) {
                    withTransform({
                        translate(pan.x, pan.y)
                        scale(scale, scale, Offset.Zero)
                    }) {
                        if (hovered != null) {
                            val pos = mapper.getSocketPosition(hovered)
                            drawCircle(
                                color = AccentCyan.copy(alpha = 0.7f),
                                radius = 9.5f,
                                center = pos,
                                style = Stroke(width = 1.8f)
                            )
                        }
                        if (hasDraft) {
                            drawDraftWire(mapper, state, guidePath, activeWireColor)
                        }
                    }
                }

                if (loupeState.isVisible) {
                    LoupeRenderer.draw(
                        scope = this,
                        state = loupeState,
                        mapper = mapper,
                        circuit = circuit,
                        boardCache = boardCache,
                        pan = pan,
                        scale = scale,
                        density = density,
                        selectedChipId = selectedChipId
                    )
                }
            }

            TapToConnectHint(
                visible = isWireModeActive && !isWireSelectMode &&
                    state.draftAnchor.value != null &&
                    selectedWireId == null && selectedChipId == null,
                armedLabel = LoupeRenderer.coordinateLabel(state.draftAnchor.value)
            )

            EmptyBenchHint(
                visible = circuit.placedChips.isEmpty() && circuit.wires.isEmpty() &&
                    circuit.passives.isEmpty()
            )

            HoverReadoutPill(
                hoveredSocket = state.hoveredSocket.value,
                circuit = circuit
            )

            BreadboardControls(
                state = state,
                mapper = mapper,
                circuit = circuit,
                isWireModeActive = isWireModeActive,
                hasSelection = selectedWireId != null || selectedChipId != null,
                onDeleteEquipment = onDeleteEquipment,
                onToggleWireMode = onToggleWireMode
            )
        }
    }
}

private fun visibleBoardRect(canvas: Size, pan: Offset, scale: Float): Rect {
    val s = if (scale <= 0f) 1f else scale
    return Rect(
        left = -pan.x / s,
        top = -pan.y / s,
        right = (canvas.width - pan.x) / s,
        bottom = (canvas.height - pan.y) / s
    )
}

private fun DrawScope.drawDraftWire(
    mapper: BreadboardGeometryMapper,
    state: BreadboardCanvasState,
    guidePath: Path,
    color: WireColor
) {
    val anchor = state.draftAnchor.value ?: return
    val anchorPos = mapper.getSocketPosition(anchor)

    drawCircle(color = AccentCyan, radius = 11f, center = anchorPos, style = Stroke(width = 2.5f))
    drawCircle(color = Color.White, radius = 4f, center = anchorPos)

    val finger = state.draftFinger.value ?: return
    val locked = state.draftLocked.value
    val lockPos = if (locked != null) mapper.getSocketPosition(locked) else finger
    val strength = if (locked == null) 0f else state.draftLockStrength.value

    guidePath.reset()
    BezierWireGeometry.appendMagneticGuide(
        path = guidePath,
        sx = anchorPos.x, sy = anchorPos.y,
        fx = finger.x, fy = finger.y,
        tx = lockPos.x, ty = lockPos.y,
        lockStrength = strength
    )

    drawPath(guidePath, color = wireColorOf(color.hexArgb), style = Stroke(5f, cap = StrokeCap.Round))
    drawPath(guidePath, color = WireSpecular, style = Stroke(1.4f, cap = StrokeCap.Round))

    if (locked != null) {
        drawCircle(
            color = AccentCyan.copy(alpha = 0.35f + 0.65f * strength),
            radius = 10f + 4f * strength,
            center = lockPos,
            style = Stroke(width = 2f)
        )
    }
}
