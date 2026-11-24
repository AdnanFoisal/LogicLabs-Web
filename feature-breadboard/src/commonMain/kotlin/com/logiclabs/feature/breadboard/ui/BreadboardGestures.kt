package com.logiclabs.feature.breadboard.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.bridge.model.JumperWire
import com.logiclabs.core.bridge.model.WireColor
import com.logiclabs.feature.breadboard.canvas.BreadboardGeometryMapper
import com.logiclabs.feature.breadboard.interaction.LoupeState
import com.logiclabs.feature.breadboard.interaction.SocketHitTester
import kotlin.math.roundToInt

/** Right-click target details surfaced to the desktop workbench context menu. */
data class BreadboardContextMenuRequest(
    val screenOffset: Offset,
    val wireId: String? = null,
    val chipId: String? = null,
    val socketId: Int? = null
)

/**
 * Callbacks the gesture layer raises. Held in one object so `pointerInput` key lists
 * stay short and hot paths never capture composable scopes.
 */
class BreadboardGestureCallbacks(
    val onWireSelected: (String?) -> Unit,
    val onChipSelected: (String?) -> Unit,
    val onCircuitChanged: () -> Unit,
    val onSocketSelected: (Int) -> Unit,
    val onBeforeTopologyMutation: () -> Unit = {},
    val onChipDoubleClicked: (String) -> Unit = {},
    val onContextMenu: (BreadboardContextMenuRequest) -> Unit = {}
)

/**
 * Installs desktop-grade mouse + touch pointer handling:
 * - Mouse wheel: smooth zoom anchored at mouse cursor.
 * - Mouse move/hover: socket hover ring, live armed-wire preview following cursor, toggleable/Alt loupe.
 * - Right-click: cancels armed draft wire, or opens context menu for wire/chip/board.
 * - Middle-click drag or Space+Left-click drag: pans smoothly in any mode.
 * - Double-click on a placed IC: opens live IC Pinout Inspector.
 * - Left-click / drag: click-to-connect & drag-to-connect wiring, wire endpoint drag, direct chip drag, pan/select.
 */
fun Modifier.breadboardGestures(
    state: BreadboardCanvasState,
    mapper: BreadboardGeometryMapper,
    hitTester: SocketHitTester,
    circuit: BreadboardCircuit,
    density: Float,
    activeWireColor: WireColor,
    defaultWireManhattan: Boolean,
    isWireModeActive: Boolean,
    isWireSelectMode: Boolean,
    selectedWireId: String?,
    selectedChipId: String?,
    callbacks: BreadboardGestureCallbacks
): Modifier = this
    // Pass 1: Continuous hover, scroll-wheel zoom, and live draft wire cursor tracking
    .pointerInput(isWireModeActive, isWireSelectMode, density) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull() ?: continue
                val pos = change.position

                when (event.type) {
                    PointerEventType.Scroll -> {
                        val dy = change.scrollDelta.y
                        if (dy != 0f) {
                            val factor = if (dy < 0f) 1.16f else 1f / 1.16f
                            state.applyPinch(factor, Offset.Zero, pos)
                            change.consume()
                        }
                    }

                    PointerEventType.Move, PointerEventType.Enter -> {
                        val board = state.toBoard(pos)
                        val hoverSock = mapper.findClosestSocket(board, mapper.socketPitch * 0.65f)
                        if (state.hoveredSocket.value != hoverSock) {
                            state.hoveredSocket.value = hoverSock
                        }

                        // Live rubber-band preview while a click-to-connect wire anchor is armed
                        if (state.draftAnchor.value != null && !change.pressed) {
                            state.draftFinger.value = board
                            state.draftLocked.value = hoverSock
                            state.draftLockStrength.value = if (hoverSock != null) 0.9f else 0f
                        }

                        // Show 2.2x optical loupe on desktop when toggled (L) or while holding Alt
                        val showLoupe = state.loupeEnabled.value || event.keyboardModifiers.isAltPressed
                        if (showLoupe) {
                            state.loupe.value = LoupeState(
                                isVisible = true,
                                touchPosition = pos,
                                snappedSocket = hoverSock
                            )
                        } else if (!change.pressed && state.loupe.value.isVisible) {
                            state.hideLoupe()
                        }
                    }

                    PointerEventType.Exit -> {
                        state.hoveredSocket.value = null
                        if (!change.pressed) {
                            state.hideLoupe()
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
    // Pass 2: Click, drag, right-click, middle-pan, double-click, and multi-touch gestures
    .pointerInput(
        activeWireColor, defaultWireManhattan, isWireModeActive, isWireSelectMode, selectedWireId, selectedChipId, density
    ) {
        var lastChipClickId: String? = null
        var lastChipClickTimeMs = 0L
        var lastEmptyTapTimeMs = 0L
        var lastEmptyTapPos = Offset.Zero

        awaitEachGesture {
            val downEvent = awaitPointerEvent()
            val down = downEvent.changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
            val scaleAtDown = state.scale.value
            val boardDown = state.toBoard(down.position)
            val dragSlop = (SocketHitTester.DRAG_SLOP_DP * density).coerceAtLeast(4f)

            hitTester.reset()

            val selWire = if (selectedWireId != null) {
                circuit.wires.firstOrNull { it.id == selectedWireId }
            } else null

            var handleIndex = -1
            if (selWire != null) {
                val s = mapper.getSocketPosition(selWire.startSocket)
                val e = mapper.getSocketPosition(selWire.endSocket)
                handleIndex = when {
                    hitTester.isOnHandle(boardDown, s, density, scaleAtDown) -> 0
                    hitTester.isOnHandle(boardDown, e, density, scaleAtDown) -> 1
                    else -> -1
                }
            }

            val hitWire = hitTester.resolveWire(boardDown, circuit.wires, density, scaleAtDown)
            val hitChip = mapper.findClosestChip(boardDown, circuit.placedChips)
            hitTester.reset()
            val hitSocket = hitTester.resolve(boardDown, density, scaleAtDown)
            hitTester.reset()

            // Right-click: cancel armed wire OR open desktop context menu
            if (downEvent.buttons.isSecondaryPressed) {
                down.consume()
                waitForRelease(down.id)
                if (state.draftAnchor.value != null) {
                    state.clearDraft()
                    state.recomputeHighlights(mapper, circuit, selectedWireId)
                } else {
                    if (hitWire != null) {
                        callbacks.onWireSelected(hitWire.id)
                    } else if (hitChip != null) {
                        callbacks.onChipSelected(hitChip.placedIc.id)
                    }
                    callbacks.onContextMenu(
                        BreadboardContextMenuRequest(
                            screenOffset = down.position,
                            wireId = hitWire?.id,
                            chipId = hitChip?.placedIc?.id,
                            socketId = hitSocket
                        )
                    )
                }
                return@awaitEachGesture
            }

            // Middle-click drag OR Spacebar + Left-click drag: always pan in any mode
            if (downEvent.buttons.isTertiaryPressed || state.spacePanActive.value) {
                down.consume()
                panOnly(state, down)
                return@awaitEachGesture
            }

            val wireDrawing = isWireModeActive && !isWireSelectMode

            when {
                wireDrawing && hitSocket != null ->
                    drawWire(
                        state, mapper, hitTester, circuit, density, down, hitSocket,
                        activeWireColor, defaultWireManhattan, dragSlop, selectedWireId, callbacks
                    )

                handleIndex >= 0 && selWire != null && !wireDrawing ->
                    dragEndpoint(state, mapper, hitTester, circuit, density, down, selWire, handleIndex, callbacks)

                // On desktop, clicking & dragging a chip in Roam mode moves it smoothly right away,
                // while double-clicking opens the live IC Pinout Inspector.
                hitChip != null && !wireDrawing && !isWireSelectMode -> {
                    val chipId = hitChip.placedIc.id
                    val dragged = dragChip(state, mapper, circuit, down, hitChip, dragSlop, callbacks)
                    if (!dragged) {
                        val now = com.logiclabs.core.bridge.platform.currentTimeMillis()
                        if (lastChipClickId == chipId && (now - lastChipClickTimeMs) <= 380L) {
                            lastChipClickId = null
                            lastChipClickTimeMs = 0L
                            callbacks.onChipSelected(chipId)
                            callbacks.onChipDoubleClicked(chipId)
                        } else {
                            lastChipClickId = chipId
                            lastChipClickTimeMs = now
                            callbacks.onChipSelected(if (selectedChipId == chipId) null else chipId)
                            callbacks.onWireSelected(null)
                        }
                    }
                }

                else -> panZoomOrSelect(
                    state, mapper, hitTester, circuit, density, down, boardDown,
                    hitWire, hitChip, isWireModeActive, isWireSelectMode, selectedWireId,
                    selectedChipId, dragSlop, activeWireColor, defaultWireManhattan, callbacks,
                    onEmptyTap = { tapPos ->
                        val now = com.logiclabs.core.bridge.platform.currentTimeMillis()
                        val closeInSpace = (tapPos - lastEmptyTapPos).getDistance() <= 36f * density
                        if (now - lastEmptyTapTimeMs <= 360L && closeInSpace) {
                            lastEmptyTapTimeMs = 0L
                            state.toggleQuickZoomAt(tapPos, mapper, circuit)
                        } else {
                            lastEmptyTapTimeMs = now
                            lastEmptyTapPos = tapPos
                        }
                    }
                )
            }
        }
    }

private suspend fun AwaitPointerEventScope.waitForRelease(pointerId: androidx.compose.ui.input.pointer.PointerId) {
    while (true) {
        val ev = awaitPointerEvent()
        val ch = ev.changes.firstOrNull { it.id == pointerId }
        if (ch == null || !ch.pressed) break
        ch.consume()
    }
}

private suspend fun AwaitPointerEventScope.panOnly(
    state: BreadboardCanvasState,
    down: PointerInputChange
) {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id }
        if (change == null || !change.pressed) break
        if (change.positionChanged()) {
            state.panBy(change.position - change.previousPosition)
            change.consume()
        }
    }
}

private fun PointerEvent.asPinch(state: BreadboardCanvasState): Boolean {
    if (changes.size <= 1) return false
    val centroid = calculateCentroid(useCurrent = false)
    if (centroid == Offset.Unspecified) return false
    state.applyPinch(calculateZoom(), calculatePan(), centroid)
    changes.forEach { it.consume() }
    return true
}

// ==============================================================================================
// Wire endpoint drag
// ==============================================================================================

private suspend fun AwaitPointerEventScope.dragEndpoint(
    state: BreadboardCanvasState,
    mapper: BreadboardGeometryMapper,
    hitTester: SocketHitTester,
    circuit: BreadboardCircuit,
    density: Float,
    down: PointerInputChange,
    wire: JumperWire,
    handleIndex: Int,
    callbacks: BreadboardGestureCallbacks
) {
    val anchorSocket = if (handleIndex == 0) wire.endSocket else wire.startSocket
    val movingSocket = if (handleIndex == 0) wire.startSocket else wire.endSocket
    state.draftAnchor.value = anchorSocket
    state.draftLocked.value = movingSocket
    if (state.loupeEnabled.value) {
        state.loupe.value = LoupeState(
            isVisible = true,
            touchPosition = down.position,
            snappedSocket = movingSocket
        )
    }

    var snapped: Int? = movingSocket
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id }
        if (change == null || !change.pressed) break
        change.consume()

        val board = state.toBoard(change.position)
        snapped = hitTester.resolve(board, density, state.scale.value) ?: snapped
        state.draftFinger.value = board
        state.draftLocked.value = snapped
        state.draftLockStrength.value = hitTester.lockStrength
        if (state.loupeEnabled.value || event.keyboardModifiers.isAltPressed) {
            state.loupe.value = LoupeState(
                isVisible = true,
                touchPosition = change.position,
                snappedSocket = snapped
            )
        }
    }

    if (!state.loupeEnabled.value) state.hideLoupe()
    state.clearDraft()
    hitTester.reset()

    val target = snapped
    if (target != null) {
        val newStart = if (handleIndex == 0) target else wire.startSocket
        val newEnd = if (handleIndex == 1) target else wire.endSocket
        if (newStart != newEnd && (newStart != wire.startSocket || newEnd != wire.endSocket)) {
            callbacks.onBeforeTopologyMutation()
            circuit.updateWireEndpoints(wire.id, newStart, newEnd)
            circuit.step()
            callbacks.onCircuitChanged()
        }
    }
    state.recomputeHighlights(mapper, circuit, wire.id)
}

// ==============================================================================================
// Chip drag
// ==============================================================================================

private suspend fun AwaitPointerEventScope.dragChip(
    state: BreadboardCanvasState,
    mapper: BreadboardGeometryMapper,
    circuit: BreadboardCircuit,
    down: PointerInputChange,
    chip: BreadboardCircuit.PlacedChipRuntime,
    dragSlop: Float,
    callbacks: BreadboardGestureCallbacks
): Boolean {
    var travelled = 0f
    var moved = false
    var recordedUndo = false
    while (true) {
        val event = awaitPointerEvent()
        if (event.asPinch(state)) break
        val change = event.changes.firstOrNull { it.id == down.id }
        if (change == null || !change.pressed) break
        change.consume()
        travelled += (change.position - change.previousPosition).getDistance()
        if (travelled <= dragSlop) continue

        val board = state.toBoard(change.position)
        val targetCol = ((board.x - mapper.originX) / mapper.socketPitch).roundToInt()
        val targetTrench = nearestTrench(mapper, board.y)
        if (targetCol != chip.placedIc.startColumn || targetTrench != chip.placedIc.trench) {
            if (!recordedUndo) {
                callbacks.onBeforeTopologyMutation()
                recordedUndo = true
            }
            if (circuit.moveChip(chip.placedIc.id, targetTrench, targetCol)) moved = true
        }
    }
    if (moved) {
        circuit.step()
        callbacks.onCircuitChanged()
    }
    return travelled > dragSlop
}

private fun nearestTrench(mapper: BreadboardGeometryMapper, boardY: Float): Int {
    val t1 = mapper.originY + 5 * mapper.socketPitch + mapper.blockGap / 2f
    val t2 = mapper.originY + 15 * mapper.socketPitch + mapper.blockGap + mapper.sectionGap +
        mapper.blockGap / 2f
    return if (kotlin.math.abs(boardY - t1) <= kotlin.math.abs(boardY - t2)) 1 else 2
}

// ==============================================================================================
// Wire drawing
// ==============================================================================================

private suspend fun AwaitPointerEventScope.drawWire(
    state: BreadboardCanvasState,
    mapper: BreadboardGeometryMapper,
    hitTester: SocketHitTester,
    circuit: BreadboardCircuit,
    density: Float,
    down: PointerInputChange,
    startSocket: Int,
    color: WireColor,
    manhattanDefault: Boolean,
    dragSlop: Float,
    selectedWireId: String?,
    callbacks: BreadboardGestureCallbacks
) {
    val previousAnchor = state.draftAnchor.value

    state.draftAnchor.value = startSocket
    state.draftFinger.value = state.toBoard(down.position)
    state.draftLocked.value = startSocket
    state.draftLockStrength.value = 1f
    state.recomputeHighlights(mapper, circuit, selectedWireId)
    if (state.loupeEnabled.value) {
        state.loupe.value = LoupeState(
            isVisible = true,
            touchPosition = down.position,
            snappedSocket = startSocket
        )
    }

    var travelled = 0f
    var snapped: Int? = startSocket
    var lastSnapped: Int? = startSocket

    while (true) {
        val event = awaitPointerEvent()

        if (event.changes.size > 1) {
            event.asPinch(state)
            continue
        }

        val change = event.changes.firstOrNull { it.id == down.id }
        if (change == null || !change.pressed) break
        change.consume()

        travelled += (change.position - change.previousPosition).getDistance()
        val board = state.toBoard(change.position)
        snapped = hitTester.resolve(board, density, state.scale.value)
        state.draftFinger.value = board
        state.draftLocked.value = snapped
        state.draftLockStrength.value = if (snapped == null) 0f else hitTester.lockStrength
        if (state.loupeEnabled.value || event.keyboardModifiers.isAltPressed) {
            state.loupe.value = LoupeState(
                isVisible = true,
                touchPosition = change.position,
                snappedSocket = snapped
            )
        }
        if (snapped != lastSnapped) {
            lastSnapped = snapped
            if (snapped != null) callbacks.onSocketSelected(snapped)
        }
    }

    if (!state.loupeEnabled.value) state.hideLoupe()
    hitTester.reset()

    val tapLike = travelled <= dragSlop * 2.2f

    if (tapLike) {
        handleWireModeTap(state, circuit, callbacks, startSocket, previousAnchor, color, manhattanDefault)
    } else {
        val end = snapped
        if (end != null && end != startSocket) {
            state.clearDraft()
            callbacks.onBeforeTopologyMutation()
            circuit.addWire(startSocket, end, color, manhattanDefault)
            circuit.step()
            callbacks.onCircuitChanged()
        } else {
            state.draftAnchor.value = startSocket
            state.draftFinger.value = null
            state.draftLocked.value = null
            state.draftLockStrength.value = 0f
            callbacks.onSocketSelected(startSocket)
        }
    }
    state.recomputeHighlights(mapper, circuit, selectedWireId)
}

private fun handleWireModeTap(
    state: BreadboardCanvasState,
    circuit: BreadboardCircuit,
    callbacks: BreadboardGestureCallbacks,
    socket: Int,
    previousAnchor: Int?,
    color: WireColor,
    manhattanDefault: Boolean
) {
    when {
        previousAnchor == null -> {
            state.draftAnchor.value = socket
            state.draftFinger.value = null
            state.draftLocked.value = null
            state.draftLockStrength.value = 0f
            callbacks.onSocketSelected(socket)
        }
        previousAnchor == socket -> {
            state.clearDraft()
        }
        else -> {
            state.clearDraft()
            callbacks.onBeforeTopologyMutation()
            circuit.addWire(previousAnchor, socket, color, manhattanDefault)
            circuit.step()
            callbacks.onCircuitChanged()
        }
    }
}

// ==============================================================================================
// Pan / pinch / click selection
// ==============================================================================================

private suspend fun AwaitPointerEventScope.panZoomOrSelect(
    state: BreadboardCanvasState,
    mapper: BreadboardGeometryMapper,
    hitTester: SocketHitTester,
    circuit: BreadboardCircuit,
    density: Float,
    down: PointerInputChange,
    boardDown: Offset,
    hitWire: JumperWire?,
    hitChip: BreadboardCircuit.PlacedChipRuntime?,
    isWireModeActive: Boolean,
    isWireSelectMode: Boolean,
    selectedWireId: String?,
    selectedChipId: String?,
    dragSlop: Float,
    activeWireColor: WireColor,
    defaultWireManhattan: Boolean,
    callbacks: BreadboardGestureCallbacks,
    onEmptyTap: ((Offset) -> Unit)? = null
) {
    var lastPos = down.position
    var travelled = 0f
    var pinched = false

    while (true) {
        val event = awaitPointerEvent()
        event.changes.firstOrNull()?.let { lastPos = it.position }

        if (event.changes.all { !it.pressed }) break

        if (event.asPinch(state)) {
            pinched = true
            travelled += dragSlop * 4f
            continue
        }

        val change = event.changes.firstOrNull()
        if (change != null && change.pressed && change.positionChanged()) {
            val delta = change.position - change.previousPosition
            state.panBy(delta)
            travelled += delta.getDistance()
            change.consume()
        }
    }

    val net = (lastPos - down.position).getDistance()
    if (pinched || (net >= dragSlop * 2.5f && travelled >= dragSlop * 2.5f)) return

    val boardUp = state.toBoard(lastPos)
    val scale = state.scale.value

    val tappedSocket = hitTester.resolve(boardDown, density, scale)
        ?: hitTester.resolve(boardUp, density, scale)
    hitTester.reset()

    if (isWireModeActive && !isWireSelectMode && tappedSocket != null) {
        handleWireModeTap(
            state, circuit, callbacks, tappedSocket,
            state.draftAnchor.value, activeWireColor, defaultWireManhattan
        )
        state.recomputeHighlights(mapper, circuit, selectedWireId)
        return
    }

    val wireAtSocket = if (tappedSocket != null) {
        val net2 = mapper.getConnectedColumnSockets(tappedSocket)
        circuit.wires.firstOrNull { it.startSocket in net2 || it.endSocket in net2 }
    } else null

    val candidate = hitWire
        ?: hitTester.resolveWire(boardUp, circuit.wires, density, scale)
        ?: wireAtSocket

    when {
        candidate != null ->
            callbacks.onWireSelected(if (selectedWireId == candidate.id) null else candidate.id)

        !isWireSelectMode && hitChip != null -> {
            callbacks.onChipSelected(
                if (selectedChipId == hitChip.placedIc.id) null else hitChip.placedIc.id
            )
            callbacks.onWireSelected(null)
        }

        else -> {
            callbacks.onWireSelected(null)
            callbacks.onChipSelected(null)
            if (!isWireModeActive && !isWireSelectMode) {
                onEmptyTap?.invoke(lastPos)
            }
        }
    }
    state.clearDraft()
    state.recomputeHighlights(mapper, circuit, selectedWireId)
}
