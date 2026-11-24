package com.logiclabs.core.data.persistence

import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.bridge.model.WireColor
import com.logiclabs.core.bridge.platform.randomUuid
import com.logiclabs.core.bridge.topology.AD200Topology
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Result of validating a Logic Labs circuit share code.
 */
data class BytecodeValidationResult(
    val isValid: Boolean,
    val chipCount: Int = 0,
    val wireCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * R3 Compact Circuit Sharing Bytecode Protocol (LLCB).
 *
 * Encodes placed ICs, jumper wires, switch settings, and console states into a high-density
 * binary payload, compresses it with Deflate (ZLIB), and encodes it as URL-safe Base64
 * prepended with the "LOGIC-" prefix.
 *
 * 100% binary-compatible across Android, Desktop (JVM), and Web (WasmGC / JS).
 */
@OptIn(ExperimentalEncodingApi::class)
object CircuitShareBytecode {

    const val PREFIX: String = "LOGIC-"
    const val VERSION: Byte = 0x01
    val MAGIC_HEADER: ByteArray = byteArrayOf(0x4C, 0x4C, 0x43, 0x42) // "LLCB"

    val CHIP_CATALOG_LOOKUP: List<String> = listOf(
        "7400", "7402", "7404", "7408", "7410", "7411", "7420",
        "7432", "7486", "74266", "7483", "7474", "7476", "7448"
    )

    private val urlSafeNoPad = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    private data class InternalWire(
        val startSocket: Int,
        val endSocket: Int,
        val color: WireColor,
        val elevationLevel: Int,
        val isManhattan: Boolean
    )

    /** Normalizes part number strings (e.g. "SN7400N" -> "7400"). */
    fun normalizePartNumber(partNumber: String): String {
        val trimmed = partNumber.trim()
        if (trimmed in CHIP_CATALOG_LOOKUP) return trimmed
        val stripped = trimmed.removePrefix("SN").removeSuffix("N").trim()
        if (stripped in CHIP_CATALOG_LOOKUP) return stripped
        return trimmed
    }

    /** Encodes a live [BreadboardCircuit] into a compact "LOGIC-..." share code. */
    fun encode(circuit: BreadboardCircuit): String {
        val chips = circuit.placedChips.map {
            SerializedChip(
                id = it.placedIc.id,
                partNumber = it.placedIc.partNumber,
                trench = it.placedIc.trench,
                startColumn = it.placedIc.startColumn,
                isRotated180 = it.placedIc.isRotated180
            )
        }
        val wires = circuit.wires.map {
            InternalWire(
                startSocket = it.startSocket,
                endSocket = it.endSocket,
                color = it.color,
                elevationLevel = it.elevationLevel,
                isManhattan = it.isManhattan
            )
        }
        return encodeBinary(
            chips = chips,
            wires = wires,
            switches = circuit.switches.toList(),
            masterPower = circuit.masterPower,
            clockFrequencyHz = circuit.clockFrequencyHz,
            clockRunning = circuit.clockRunning
        )
    }

    /** Encodes a [SerializedProject] into a compact "LOGIC-..." share code. */
    fun encode(project: SerializedProject): String {
        val wires = project.wires.map { w ->
            val color = try {
                WireColor.valueOf(w.colorName)
            } catch (_: Exception) {
                WireColor.RED
            }
            InternalWire(
                startSocket = w.startSocket,
                endSocket = w.endSocket,
                color = color,
                elevationLevel = 0,
                isManhattan = w.isManhattan
            )
        }
        return encodeBinary(
            chips = project.chips,
            wires = wires,
            switches = project.switches,
            masterPower = project.masterPower,
            clockFrequencyHz = project.clockFrequencyHz,
            clockRunning = project.clockRunning
        )
    }

    private fun encodeBinary(
        chips: List<SerializedChip>,
        wires: List<InternalWire>,
        switches: List<Boolean>,
        masterPower: Boolean,
        clockFrequencyHz: Double,
        clockRunning: Boolean
    ): String {
        if (chips.size > 255) {
            throw IllegalArgumentException("Chip count exceeds maximum supported by bytecode protocol (255)")
        }
        if (wires.size > 65535) {
            throw IllegalArgumentException("Wire count exceeds maximum supported by bytecode protocol (65535)")
        }

        val totalSize = 14 + chips.size * 4 + wires.size * 6
        val buf = ByteArray(totalSize)
        var pos = 0

        fun writeByte(v: Int) {
            buf[pos++] = (v and 0xFF).toByte()
        }

        fun writeShortBE(v: Int) {
            buf[pos++] = ((v ushr 8) and 0xFF).toByte()
            buf[pos++] = (v and 0xFF).toByte()
        }

        fun writeIntBE(v: Int) {
            buf[pos++] = ((v ushr 24) and 0xFF).toByte()
            buf[pos++] = ((v ushr 16) and 0xFF).toByte()
            buf[pos++] = ((v ushr 8) and 0xFF).toByte()
            buf[pos++] = (v and 0xFF).toByte()
        }

        // 1. Magic header (4 bytes)
        MAGIC_HEADER.copyInto(buf, pos)
        pos += 4

        // 2. Version (1 byte)
        writeByte(VERSION.toInt())

        // 3. Switches bitmask (1 byte)
        var switchMask = 0
        for (i in 0 until 8) {
            if (i < switches.size && switches[i]) {
                switchMask = switchMask or (1 shl i)
            }
        }
        writeByte(switchMask)

        // 4. Console flags (1 byte)
        var consoleFlags = 0
        if (masterPower) consoleFlags = consoleFlags or 0x01
        if (clockRunning) consoleFlags = consoleFlags or 0x02
        writeByte(consoleFlags)

        // 5. Clock frequency (4 bytes IEEE-754 big-endian float)
        writeIntBE(clockFrequencyHz.toFloat().toRawBits())

        // 6. Chip count N (1 byte)
        writeByte(chips.size)

        // 7. Packed chips (4 * N bytes)
        for (chip in chips) {
            val normalized = normalizePartNumber(chip.partNumber)
            val lookupId = CHIP_CATALOG_LOOKUP.indexOf(normalized)
            if (lookupId < 0) {
                throw IllegalArgumentException("Unsupported IC part number: ${chip.partNumber}")
            }
            if (chip.trench !in 1..2) {
                throw IllegalArgumentException("Invalid trench ${chip.trench} for chip ${chip.partNumber}")
            }
            if (chip.startColumn !in 0..63) {
                throw IllegalArgumentException("Invalid start column ${chip.startColumn} for chip ${chip.partNumber}")
            }
            writeByte(lookupId)
            writeByte(chip.trench)
            writeByte(chip.startColumn)
            writeByte(if (chip.isRotated180) 0x01 else 0x00)
        }

        // 8. Wire count M (2 bytes short)
        writeShortBE(wires.size and 0xFFFF)

        // 9. Packed wires (6 * M bytes)
        for (wire in wires) {
            if (wire.startSocket !in 0 until AD200Topology.TOTAL_NETLIST_NODES) {
                throw IllegalArgumentException("Start socket out of range: ${wire.startSocket}")
            }
            if (wire.endSocket !in 0 until AD200Topology.TOTAL_NETLIST_NODES) {
                throw IllegalArgumentException("End socket out of range: ${wire.endSocket}")
            }
            writeShortBE(wire.startSocket and 0xFFFF)
            writeShortBE(wire.endSocket and 0xFFFF)
            writeByte(wire.color.ordinal.coerceIn(0, WireColor.entries.size - 1))

            var routingFlags = (wire.elevationLevel and 0x03) shl 1
            if (wire.isManhattan) routingFlags = routingFlags or 0x01
            writeByte(routingFlags)
        }

        val compressed = zlibCompress(buf)
        val base64 = urlSafeNoPad.encode(compressed).trimEnd('=')
        return "$PREFIX$base64"
    }

    /**
     * Decodes a "LOGIC-..." bytecode string into a [SerializedProject].
     * Throws [IllegalArgumentException] if the bytecode is invalid, corrupted, or incompatible.
     */
    fun decodeToProject(code: String): SerializedProject {
        if (!code.startsWith(PREFIX)) {
            throw IllegalArgumentException("Missing required '$PREFIX' prefix")
        }
        val payload = code.removePrefix(PREFIX).trim()
        if (payload.isEmpty()) {
            throw IllegalArgumentException("Share code payload is empty")
        }

        val compressed = try {
            urlSafeNoPad.decode(payload)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid URL-safe Base64 encoding: ${e.message}", e)
        }

        val uncompressed = zlibDecompress(compressed)
        if (uncompressed.size < 14) {
            throw IllegalArgumentException("Bytecode payload too short: ${uncompressed.size} bytes (minimum is 14)")
        }

        var pos = 0
        fun readUByte(): Int = uncompressed[pos++].toInt() and 0xFF
        fun readUShortBE(): Int {
            val hi = readUByte()
            val lo = readUByte()
            return (hi shl 8) or lo
        }
        fun readIntBE(): Int {
            val b0 = readUByte()
            val b1 = readUByte()
            val b2 = readUByte()
            val b3 = readUByte()
            return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        }

        // 1. Verify Magic Header
        for (i in 0 until 4) {
            if (uncompressed[pos++] != MAGIC_HEADER[i]) {
                throw IllegalArgumentException("Invalid magic header: expected 'LLCB'")
            }
        }

        // 2. Version
        val version = readUByte()
        if (version != VERSION.toInt()) {
            throw IllegalArgumentException("Unsupported bytecode version: $version (expected $VERSION)")
        }

        // 3. Switches bitmask
        val switchMask = readUByte()
        val switches = (0 until 8).map { (switchMask and (1 shl it)) != 0 }

        // 4. Console flags
        val consoleFlags = readUByte()
        val masterPower = (consoleFlags and 0x01) != 0
        val clockRunning = (consoleFlags and 0x02) != 0

        // 5. Clock frequency
        val clockFrequencyHz = Float.fromBits(readIntBE()).toDouble()

        // 6. Chip count N
        val chipCount = readUByte()
        val minRequiredSize = 14 + (chipCount * 4)
        if (uncompressed.size < minRequiredSize) {
            throw IllegalArgumentException("Truncated chip data: expected at least $minRequiredSize bytes, got ${uncompressed.size}")
        }

        // 7. Packed chips
        val chips = mutableListOf<SerializedChip>()
        for (i in 0 until chipCount) {
            val lookupId = readUByte()
            if (lookupId !in CHIP_CATALOG_LOOKUP.indices) {
                throw IllegalArgumentException("Unknown IC lookup ID $lookupId at index $i")
            }
            val trench = readUByte()
            if (trench !in 1..2) {
                throw IllegalArgumentException("Invalid trench $trench for chip at index $i")
            }
            val startColumn = readUByte()
            if (startColumn !in 0..63) {
                throw IllegalArgumentException("Invalid start column $startColumn for chip at index $i")
            }
            val flags = readUByte()
            val isRotated180 = (flags and 0x01) != 0

            chips.add(
                SerializedChip(
                    id = randomUuid(),
                    partNumber = CHIP_CATALOG_LOOKUP[lookupId],
                    trench = trench,
                    startColumn = startColumn,
                    isRotated180 = isRotated180
                )
            )
        }

        // 8. Wire count M
        val wireCount = readUShortBE()
        val totalExpectedSize = 14 + (chipCount * 4) + (wireCount * 6)
        if (uncompressed.size < totalExpectedSize) {
            throw IllegalArgumentException("Truncated wire data: expected $totalExpectedSize bytes, got ${uncompressed.size}")
        }

        // 9. Packed wires
        val wires = mutableListOf<SerializedWire>()
        for (i in 0 until wireCount) {
            val startSocket = readUShortBE()
            val endSocket = readUShortBE()
            if (startSocket !in 0 until AD200Topology.TOTAL_NETLIST_NODES ||
                endSocket !in 0 until AD200Topology.TOTAL_NETLIST_NODES
            ) {
                throw IllegalArgumentException("Wire socket out of range at index $i: start=$startSocket, end=$endSocket")
            }

            val colorByte = readUByte()
            val wireColor = if (colorByte in WireColor.entries.indices) {
                WireColor.entries[colorByte]
            } else {
                WireColor.RED
            }

            val routingFlags = readUByte()
            val isManhattan = (routingFlags and 0x01) != 0

            wires.add(
                SerializedWire(
                    id = randomUuid(),
                    startSocket = startSocket,
                    endSocket = endSocket,
                    colorName = wireColor.name,
                    isManhattan = isManhattan
                )
            )
        }

        if (pos < uncompressed.size) {
            throw IllegalArgumentException("Corrupt bytecode: unexpected trailing bytes (${uncompressed.size - pos} remaining)")
        }

        return SerializedProject(
            title = "Shared Circuit",
            chips = chips,
            wires = wires,
            switches = switches,
            masterPower = masterPower,
            clockFrequencyHz = clockFrequencyHz,
            clockRunning = clockRunning
        )
    }

    /** Validates a candidate share code without throwing exceptions. */
    fun validate(code: String): BytecodeValidationResult {
        return try {
            val project = decodeToProject(code)
            BytecodeValidationResult(
                isValid = true,
                chipCount = project.chips.size,
                wireCount = project.wires.size,
                errorMessage = null
            )
        } catch (e: Exception) {
            BytecodeValidationResult(
                isValid = false,
                chipCount = 0,
                wireCount = 0,
                errorMessage = e.message ?: "Invalid circuit code"
            )
        }
    }
}
