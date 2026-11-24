package com.logiclabs.app.ui.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.bridge.model.JumperWire
import com.logiclabs.core.bridge.model.PassiveComponent
import com.logiclabs.core.bridge.model.PlacedIC

/**
 * Immutable snapshot of board topology (placed chips, jumper wires, and passive components)
 * used by [UndoRedoManager] for multi-step `Ctrl+Z` / `Ctrl+Y` history.
 */
data class BoardTopologySnapshot(
    val chips: List<PlacedIC>,
    val wires: List<JumperWire>,
    val passives: List<PassiveComponent>
) {
    companion object {
        fun capture(circuit: BreadboardCircuit): BoardTopologySnapshot = BoardTopologySnapshot(
            chips = circuit.placedChips.map { it.placedIc.copy() },
            wires = circuit.wires.map { it.copy() },
            passives = circuit.passives.toList()
        )
    }

    fun restoreInto(circuit: BreadboardCircuit) {
        val power = circuit.masterPower
        val mode = circuit.simulationMode
        val switchesCopy = circuit.switches.copyOf()
        val freq = circuit.clockFrequencyHz
        val running = circuit.clockRunning
        val clkState = circuit.clockState

        circuit.clearAll()
        circuit.masterPower = power
        circuit.simulationMode = mode
        circuit.clockFrequencyHz = freq
        circuit.clockRunning = running
        circuit.clockState = clkState
        switchesCopy.copyInto(circuit.switches)

        for (ic in chips) {
            circuit.addChip(ic.partNumber, ic.trench, ic.startColumn, ic.isRotated180, ic.id)
        }
        for (w in wires) {
            circuit.addWire(w.startSocket, w.endSocket, w.color, w.isManhattan, w.id)
        }
        for (p in passives) {
            circuit.passives.add(p)
        }
        circuit.rebuildNetlist()
        circuit.step()
    }
}

/**
 * Multi-step Undo / Redo stack for breadboard topology edits (chip placement, move, rotate,
 * delete; wire draw, recolor, flip, routing toggle, endpoint drag, delete; clear/reset).
 */
@Stable
class UndoRedoManager(
    private val maxSteps: Int = 60
) {
    private val undoStack = ArrayDeque<BoardTopologySnapshot>()
    private val redoStack = ArrayDeque<BoardTopologySnapshot>()

    var undoCount: Int by mutableIntStateOf(0)
        private set

    var redoCount: Int by mutableIntStateOf(0)
        private set

    val canUndo: Boolean get() = undoCount > 0
    val canRedo: Boolean get() = redoCount > 0

    /** Records the board's state BEFORE a topology mutation executes. */
    fun pushUndoSnapshot(circuit: BreadboardCircuit) {
        val snap = BoardTopologySnapshot.capture(circuit)
        if (undoStack.lastOrNull() == snap) return
        undoStack.addLast(snap)
        while (undoStack.size > maxSteps) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        syncCounts()
    }

    /** Undoes the last topology edit. Returns true if state changed. */
    fun undo(circuit: BreadboardCircuit): Boolean {
        val target = undoStack.removeLastOrNull() ?: return false
        redoStack.addLast(BoardTopologySnapshot.capture(circuit))
        target.restoreInto(circuit)
        syncCounts()
        return true
    }

    /** Redoes the most recently undone edit. Returns true if state changed. */
    fun redo(circuit: BreadboardCircuit): Boolean {
        val target = redoStack.removeLastOrNull() ?: return false
        undoStack.addLast(BoardTopologySnapshot.capture(circuit))
        target.restoreInto(circuit)
        syncCounts()
        return true
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        syncCounts()
    }

    private fun syncCounts() {
        undoCount = undoStack.size
        redoCount = redoStack.size
    }
}
