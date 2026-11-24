package com.logiclabs.app

import com.logiclabs.app.ui.state.UndoRedoManager
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.bridge.model.WireColor
import com.logiclabs.core.bridge.topology.AD200Topology
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoRedoManagerTest {

    @Test
    fun `multi-step undo and redo restores chips, rotations, moves, and wires faithfully`() {
        val circuit = BreadboardCircuit()
        val mgr = UndoRedoManager(maxSteps = 20)

        assertFalse(mgr.canUndo)
        assertFalse(mgr.canRedo)

        // Step 1: place 7404
        mgr.pushUndoSnapshot(circuit)
        val chipId = circuit.addChip("7404", 1, 10)!!.placedIc.id
        assertEquals(1, circuit.placedChips.size)
        assertEquals(1, mgr.undoCount)

        // Step 2: wire SW0 -> pin 1
        mgr.pushUndoSnapshot(circuit)
        val chip = circuit.placedChips.first()
        circuit.addWire(AD200Topology.TERM_SW0, chip.getPinSocket(1), WireColor.YELLOW)
        assertEquals(1, circuit.wires.size)
        assertEquals(2, mgr.undoCount)

        // Step 3: rotate chip
        mgr.pushUndoSnapshot(circuit)
        circuit.rotateChip(chipId)
        assertTrue(circuit.placedChips.first().placedIc.isRotated180)
        assertEquals(3, mgr.undoCount)

        // Step 4: clear wires
        mgr.pushUndoSnapshot(circuit)
        circuit.clearWires()
        assertEquals(0, circuit.wires.size)
        assertEquals(4, mgr.undoCount)

        // Undo step 4 -> wire returns
        assertTrue(mgr.undo(circuit))
        assertEquals(1, circuit.wires.size)
        assertTrue(circuit.placedChips.first().placedIc.isRotated180)
        assertEquals(1, mgr.redoCount)

        // Undo step 3 -> chip unrotated
        assertTrue(mgr.undo(circuit))
        assertFalse(circuit.placedChips.first().placedIc.isRotated180)
        assertEquals(1, circuit.wires.size)

        // Undo step 2 -> wire removed, chip remains
        assertTrue(mgr.undo(circuit))
        assertEquals(0, circuit.wires.size)
        assertEquals(1, circuit.placedChips.size)

        // Redo step 2 -> wire returns
        assertTrue(mgr.redo(circuit))
        assertEquals(1, circuit.wires.size)

        // New mutation clears redo stack
        mgr.pushUndoSnapshot(circuit)
        circuit.moveChip(chipId, 1, 20)
        assertFalse(mgr.canRedo)
        assertEquals(20, circuit.placedChips.first().placedIc.startColumn)

        // Undo move restores column 10
        assertTrue(mgr.undo(circuit))
        assertEquals(10, circuit.placedChips.first().placedIc.startColumn)
    }

    @Test
    fun `history is bounded by maxSteps`() {
        val circuit = BreadboardCircuit()
        val mgr = UndoRedoManager(maxSteps = 3)

        for (col in listOf(4, 14, 24, 34, 44)) {
            mgr.pushUndoSnapshot(circuit)
            circuit.addChip("7400", 1, col)
        }

        assertEquals(3, mgr.undoCount)
        assertTrue(mgr.undo(circuit))
        assertTrue(mgr.undo(circuit))
        assertTrue(mgr.undo(circuit))
        assertFalse(mgr.undo(circuit))
        assertEquals(2, circuit.placedChips.size)
    }
}
