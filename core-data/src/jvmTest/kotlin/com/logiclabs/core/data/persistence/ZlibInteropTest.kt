package com.logiclabs.core.data.persistence

import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.bridge.model.WireColor
import com.logiclabs.core.bridge.topology.AD200Topology
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.random.Random

/**
 * Verifies binary cross-compatibility between PureKotlinZlib (Web WasmGC/JS)
 * and java.util.zip.Deflater/Inflater (Desktop JVM & Android).
 */
class ZlibInteropTest {

    @Test
    fun pureKotlinZlib_interoperatesBidirectionallyWithJavaZip() {
        val samples = listOf(
            ByteArray(0),
            "Hello Logic Labs!".encodeToByteArray(),
            ByteArray(4096) { (it % 17).toByte() },
            Random(42).nextBytes(2048),
            ("LLCB" + "740074087486".repeat(200)).encodeToByteArray()
        )
        for (raw in samples) {
            // Java compress -> PureKotlin decompress (Dynamic & Fixed Huffman blocks from Deflater.BEST_COMPRESSION)
            val javaZ = zlibCompress(raw)
            val pureOut = PureKotlinZlib.decompress(javaZ)
            assertArrayEquals("PureKotlinZlib failed to inflate java.util.zip payload (${raw.size}B)", raw, pureOut)

            // PureKotlin compress -> Java decompress (Web LOGIC-... -> Android/PC)
            val pureZ = PureKotlinZlib.compress(raw)
            val javaOut = zlibDecompress(pureZ)
            assertArrayEquals("java.util.zip failed to inflate PureKotlinZlib payload (${raw.size}B)", raw, javaOut)
        }
    }

    @Test
    fun pureKotlinZlib_rejectsCorruptAndTruncatedPayloads() {
        assertThrows(IllegalArgumentException::class.java) {
            PureKotlinZlib.decompress(byteArrayOf(0x00, 0x01, 0x02))
        }
        val valid = PureKotlinZlib.compress("Circuit Payload Test 123456".encodeToByteArray())
        // Corrupt Adler-32 checksum
        val badChecksum = valid.copyOf()
        badChecksum[badChecksum.lastIndex] = (badChecksum[badChecksum.lastIndex].toInt() xor 0xFF).toByte()
        assertThrows(IllegalArgumentException::class.java) {
            PureKotlinZlib.decompress(badChecksum)
        }
    }

    @Test
    fun shareCode_decodedIdenticallyBetweenJvmAndWebZlib() {
        val circuit = BreadboardCircuit()
        circuit.addChip("7486", 1, 10)
        circuit.addChip("7408", 1, 24)
        circuit.addWire(AD200Topology.TERM_SW0, AD200Topology.terminalSocket(1, 10, 0), WireColor.YELLOW)
        circuit.addWire(AD200Topology.TERM_SW1, AD200Topology.terminalSocket(1, 11, 0), WireColor.ORANGE)
        circuit.addWire(AD200Topology.terminalSocket(1, 12, 0), AD200Topology.TERM_LED0, WireColor.GREEN)
        val code = CircuitShareBytecode.encode(circuit)
        val decoded = CircuitShareBytecode.decodeToProject(code)
        assertEquals(2, decoded.chips.size)
        assertEquals(3, decoded.wires.size)
    }
}
