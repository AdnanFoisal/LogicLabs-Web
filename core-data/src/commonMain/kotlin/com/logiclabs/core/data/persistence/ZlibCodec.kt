package com.logiclabs.core.data.persistence

/** Platform ZLIB (RFC 1950) compression/decompression for cross-platform `LOGIC-...` codes. */
expect fun zlibCompress(input: ByteArray): ByteArray
expect fun zlibDecompress(input: ByteArray, maxOutputBytes: Int = 2 * 1024 * 1024): ByteArray

/**
 * Pure-Kotlin RFC 1950 (ZLIB) + RFC 1951 (DEFLATE / INFLATE) implementation.
 *
 * Supports all RFC 1951 inflate block types (Stored, Fixed Huffman, Dynamic Huffman) with
 * Adler-32 integrity checks and zip-bomb protection, plus LZ77 Fixed-Huffman Deflate.
 * Binary-compatible with Android and Desktop `java.util.zip.Deflater` / `Inflater`.
 */
object PureKotlinZlib {

    private val LEN_BASE = intArrayOf(
        3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
        35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258
    )
    private val LEN_EXTRA = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
        3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0
    )
    private val DIST_BASE = intArrayOf(
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
        257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577
    )
    private val DIST_EXTRA = intArrayOf(
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
    )
    private val CL_ORDER = intArrayOf(
        16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15
    )

    private val FIXED_LIT_LEN_TABLE: HuffmanTree by lazy {
        val lengths = IntArray(288)
        for (i in 0..143) lengths[i] = 8
        for (i in 144..255) lengths[i] = 9
        for (i in 256..279) lengths[i] = 7
        for (i in 280..287) lengths[i] = 8
        HuffmanTree.build(lengths)
    }

    private val FIXED_DIST_TABLE: HuffmanTree by lazy {
        HuffmanTree.build(IntArray(32) { 5 })
    }

    fun adler32(data: ByteArray, length: Int = data.size): Int {
        var s1 = 1L
        var s2 = 0L
        for (i in 0 until length) {
            s1 = (s1 + (data[i].toInt() and 0xFF)) % 65521L
            s2 = (s2 + s1) % 65521L
        }
        return ((s2 shl 16) or s1).toInt()
    }

    /** Compresses [input] into an RFC 1950 ZLIB stream using LZ77 + Fixed Huffman DEFLATE. */
    fun compress(input: ByteArray): ByteArray {
        val bw = BitWriter()
        // ZLIB header: CMF=0x78 (deflate, 32K window), FLG=0x9C (default compression, fcheck valid)
        bw.writeRawByte(0x78)
        bw.writeRawByte(0x9C)

        // Single final Fixed-Huffman DEFLATE block: BFINAL=1, BTYPE=01 (bits LSB-first: 1, 1, 0)
        bw.writeBits(1, 1)
        bw.writeBits(1, 2)

        var pos = 0
        val n = input.size
        while (pos < n) {
            var bestLen = 0
            var bestDist = 0
            if (pos + 3 <= n) {
                val windowStart = (pos - 32768).coerceAtLeast(0)
                val maxMatch = (n - pos).coerceAtMost(258)
                val b0 = input[pos]
                val b1 = input[pos + 1]
                val b2 = input[pos + 2]
                var cand = pos - 1
                var checks = 0
                while (cand >= windowStart && checks < 512) {
                    if (input[cand] == b0 && input[cand + 1] == b1 && input[cand + 2] == b2) {
                        var len = 3
                        while (len < maxMatch && input[cand + len] == input[pos + len]) {
                            len++
                        }
                        if (len > bestLen) {
                            bestLen = len
                            bestDist = pos - cand
                            if (bestLen == maxMatch) break
                        }
                    }
                    cand--
                    checks++
                }
            }

            if (bestLen >= 3) {
                emitFixedLength(bw, bestLen)
                emitFixedDistance(bw, bestDist)
                pos += bestLen
            } else {
                emitFixedLiteral(bw, input[pos].toInt() and 0xFF)
                pos++
            }
        }

        // End-of-block symbol 256 (7-bit code 0000000)
        emitFixedLiteral(bw, 256)
        bw.alignToByte()

        // Big-endian Adler-32 trailer
        val checksum = adler32(input)
        bw.writeRawByte((checksum ushr 24) and 0xFF)
        bw.writeRawByte((checksum ushr 16) and 0xFF)
        bw.writeRawByte((checksum ushr 8) and 0xFF)
        bw.writeRawByte(checksum and 0xFF)
        return bw.toByteArray()
    }

    /** Decompresses an RFC 1950 ZLIB stream with strict header, Huffman, and Adler-32 verification. */
    fun decompress(input: ByteArray, maxOutputBytes: Int = 2 * 1024 * 1024): ByteArray {
        if (input.size < 6) {
            throw IllegalArgumentException("ZLIB stream too short (${input.size} bytes)")
        }
        val cmf = input[0].toInt() and 0xFF
        val flg = input[1].toInt() and 0xFF
        if ((cmf and 0x0F) != 8) {
            throw IllegalArgumentException("Unsupported ZLIB compression method: ${cmf and 0x0F}")
        }
        if ((cmf ushr 4) > 7) {
            throw IllegalArgumentException("Invalid ZLIB window size")
        }
        if (((cmf shl 8) + flg) % 31 != 0) {
            throw IllegalArgumentException("Invalid ZLIB header checksum")
        }
        if ((flg and 0x20) != 0) {
            throw IllegalArgumentException("ZLIB preset dictionary is not supported")
        }

        val br = BitReader(input, startByte = 2)
        val out = DynamicByteBuffer(initialCapacity = (input.size * 3).coerceIn(64, 4096), maxBytes = maxOutputBytes)

        var bfinal = 0
        while (bfinal == 0) {
            bfinal = br.readBits(1)
            val btype = br.readBits(2)
            when (btype) {
                0 -> {
                    br.alignToByte()
                    val len = br.readUInt16LE()
                    val nlen = br.readUInt16LE()
                    if (len != (nlen xor 0xFFFF)) {
                        throw IllegalArgumentException("Corrupt stored DEFLATE block length")
                    }
                    for (i in 0 until len) {
                        out.append(br.readAlignedByte())
                    }
                }
                1 -> decodeHuffmanBlock(br, out, FIXED_LIT_LEN_TABLE, FIXED_DIST_TABLE)
                2 -> {
                    val hlit = br.readBits(5) + 257
                    val hdist = br.readBits(5) + 1
                    val hclen = br.readBits(4) + 4
                    if (hlit > 286 || hdist > 30) {
                        throw IllegalArgumentException("Invalid dynamic Huffman table dimensions")
                    }
                    val clLengths = IntArray(19)
                    for (i in 0 until hclen) {
                        clLengths[CL_ORDER[i]] = br.readBits(3)
                    }
                    val clTree = HuffmanTree.build(clLengths)
                    val combined = IntArray(hlit + hdist)
                    var idx = 0
                    while (idx < combined.size) {
                        val sym = clTree.decode(br)
                        when {
                            sym in 0..15 -> combined[idx++] = sym
                            sym == 16 -> {
                                if (idx == 0) throw IllegalArgumentException("Invalid RLE repeat at index 0")
                                val repeat = br.readBits(2) + 3
                                val prev = combined[idx - 1]
                                if (idx + repeat > combined.size) throw IllegalArgumentException("Code length overflow")
                                repeat(repeat) { combined[idx++] = prev }
                            }
                            sym == 17 -> {
                                val repeat = br.readBits(3) + 3
                                if (idx + repeat > combined.size) throw IllegalArgumentException("Code length overflow")
                                repeat(repeat) { combined[idx++] = 0 }
                            }
                            sym == 18 -> {
                                val repeat = br.readBits(7) + 11
                                if (idx + repeat > combined.size) throw IllegalArgumentException("Code length overflow")
                                repeat(repeat) { combined[idx++] = 0 }
                            }
                            else -> throw IllegalArgumentException("Invalid code-length symbol: $sym")
                        }
                    }
                    val litTree = HuffmanTree.build(combined.copyOfRange(0, hlit))
                    val distTree = HuffmanTree.build(combined.copyOfRange(hlit, hlit + hdist))
                    decodeHuffmanBlock(br, out, litTree, distTree)
                }
                else -> throw IllegalArgumentException("Invalid DEFLATE block type: $btype")
            }
        }

        br.alignToByte()
        if (br.remainingBytes() < 4) {
            throw IllegalArgumentException("Truncated ZLIB Adler-32 trailer")
        }
        val expectedAdler = (br.readAlignedByte() shl 24) or
            (br.readAlignedByte() shl 16) or
            (br.readAlignedByte() shl 8) or
            br.readAlignedByte()

        val result = out.toByteArray()
        val actualAdler = adler32(result)
        if (actualAdler != expectedAdler) {
            throw IllegalArgumentException("ZLIB Adler-32 checksum mismatch")
        }
        return result
    }

    private fun decodeHuffmanBlock(
        br: BitReader,
        out: DynamicByteBuffer,
        litTree: HuffmanTree,
        distTree: HuffmanTree
    ) {
        while (true) {
            val sym = litTree.decode(br)
            when {
                sym < 256 -> out.append(sym)
                sym == 256 -> return
                sym in 257..285 -> {
                    val li = sym - 257
                    val length = LEN_BASE[li] + br.readBits(LEN_EXTRA[li])
                    val distSym = distTree.decode(br)
                    if (distSym !in 0..29) {
                        throw IllegalArgumentException("Invalid DEFLATE distance symbol: $distSym")
                    }
                    val distance = DIST_BASE[distSym] + br.readBits(DIST_EXTRA[distSym])
                    out.copyBackReference(distance, length)
                }
                else -> throw IllegalArgumentException("Invalid DEFLATE literal/length symbol: $sym")
            }
        }
    }

    private fun emitFixedLiteral(bw: BitWriter, sym: Int) {
        when (sym) {
            in 0..143 -> bw.writeBitsReversed(0x30 + sym, 8)
            in 144..255 -> bw.writeBitsReversed(0x190 + (sym - 144), 9)
            in 256..279 -> bw.writeBitsReversed(sym - 256, 7)
            in 280..287 -> bw.writeBitsReversed(0xC0 + (sym - 280), 8)
            else -> throw IllegalArgumentException("Invalid literal symbol $sym")
        }
    }

    private fun emitFixedLength(bw: BitWriter, length: Int) {
        var idx = LEN_BASE.size - 1
        while (idx > 0 && LEN_BASE[idx] > length) idx--
        emitFixedLiteral(bw, 257 + idx)
        val extra = LEN_EXTRA[idx]
        if (extra > 0) {
            bw.writeBits(length - LEN_BASE[idx], extra)
        }
    }

    private fun emitFixedDistance(bw: BitWriter, distance: Int) {
        var idx = DIST_BASE.size - 1
        while (idx > 0 && DIST_BASE[idx] > distance) idx--
        bw.writeBitsReversed(idx, 5)
        val extra = DIST_EXTRA[idx]
        if (extra > 0) {
            bw.writeBits(distance - DIST_BASE[idx], extra)
        }
    }

    private class HuffmanTree(
        private val counts: IntArray,
        private val symbols: IntArray,
        private val maxBits: Int
    ) {
        fun decode(br: BitReader): Int {
            var code = 0
            var first = 0
            var index = 0
            for (len in 1..maxBits) {
                code = (code shl 1) or br.readBits(1)
                val count = counts[len]
                if (code - count < first) {
                    return symbols[index + (code - first)]
                }
                index += count
                first = (first + count) shl 1
            }
            throw IllegalArgumentException("Invalid Huffman code in DEFLATE stream")
        }

        companion object {
            fun build(lengths: IntArray): HuffmanTree {
                val maxBits = 15
                val counts = IntArray(maxBits + 1)
                var highestBit = 0
                for (len in lengths) {
                    if (len < 0 || len > maxBits) {
                        throw IllegalArgumentException("Invalid Huffman bit length: $len")
                    }
                    if (len > 0) {
                        counts[len]++
                        if (len > highestBit) highestBit = len
                    }
                }
                // Validate non-oversubscribed code lengths
                var left = 1
                for (len in 1..maxBits) {
                    left = (left shl 1) - counts[len]
                    if (left < 0) throw IllegalArgumentException("Oversubscribed Huffman tree")
                }
                val offsets = IntArray(maxBits + 1)
                for (len in 1 until maxBits) {
                    offsets[len + 1] = offsets[len] + counts[len]
                }
                val symbols = IntArray(lengths.size)
                for (sym in lengths.indices) {
                    val len = lengths[sym]
                    if (len != 0) {
                        symbols[offsets[len]++] = sym
                    }
                }
                return HuffmanTree(counts, symbols, highestBit.coerceAtLeast(1))
            }
        }
    }

    private class BitReader(private val data: ByteArray, startByte: Int) {
        private var bytePos = startByte
        private var bitBuf = 0
        private var bitCount = 0

        fun readBits(n: Int): Int {
            if (n == 0) return 0
            while (bitCount < n) {
                if (bytePos >= data.size) {
                    throw IllegalArgumentException("Unexpected end of DEFLATE bitstream")
                }
                bitBuf = bitBuf or ((data[bytePos++].toInt() and 0xFF) shl bitCount)
                bitCount += 8
            }
            val mask = (1 shl n) - 1
            val v = bitBuf and mask
            bitBuf = bitBuf ushr n
            bitCount -= n
            return v
        }

        fun alignToByte() {
            bitBuf = 0
            bitCount = 0
        }

        fun readAlignedByte(): Int {
            if (bytePos >= data.size) {
                throw IllegalArgumentException("Unexpected end of ZLIB stream")
            }
            return data[bytePos++].toInt() and 0xFF
        }

        fun readUInt16LE(): Int {
            val lo = readAlignedByte()
            val hi = readAlignedByte()
            return lo or (hi shl 8)
        }

        fun remainingBytes(): Int = (data.size - bytePos).coerceAtLeast(0)
    }

    private class BitWriter {
        private var buf = ByteArray(128)
        private var size = 0
        private var bitBuf = 0
        private var bitCount = 0

        fun writeRawByte(b: Int) {
            if (size >= buf.size) buf = buf.copyOf(buf.size * 2)
            buf[size++] = (b and 0xFF).toByte()
        }

        fun writeBits(value: Int, count: Int) {
            bitBuf = bitBuf or ((value and ((1 shl count) - 1)) shl bitCount)
            bitCount += count
            while (bitCount >= 8) {
                writeRawByte(bitBuf and 0xFF)
                bitBuf = bitBuf ushr 8
                bitCount -= 8
            }
        }

        fun writeBitsReversed(value: Int, count: Int) {
            var rev = 0
            var v = value
            for (i in 0 until count) {
                rev = (rev shl 1) or (v and 1)
                v = v ushr 1
            }
            writeBits(rev, count)
        }

        fun alignToByte() {
            if (bitCount > 0) {
                writeRawByte(bitBuf and 0xFF)
                bitBuf = 0
                bitCount = 0
            }
        }

        fun toByteArray(): ByteArray = buf.copyOf(size)
    }

    private class DynamicByteBuffer(initialCapacity: Int, private val maxBytes: Int) {
        private var buf = ByteArray(initialCapacity)
        private var size = 0

        fun append(b: Int) {
            if (size >= maxBytes) throw IllegalArgumentException("Decompressed payload exceeds $maxBytes byte limit")
            if (size >= buf.size) {
                buf = buf.copyOf((buf.size * 2).coerceAtMost(maxBytes))
            }
            buf[size++] = (b and 0xFF).toByte()
        }

        fun copyBackReference(distance: Int, length: Int) {
            if (distance <= 0 || distance > size) {
                throw IllegalArgumentException("Invalid DEFLATE back-reference distance $distance at size $size")
            }
            if (size + length > maxBytes) {
                throw IllegalArgumentException("Decompressed payload exceeds $maxBytes byte limit")
            }
            for (i in 0 until length) {
                append(buf[size - distance].toInt() and 0xFF)
            }
        }

        fun toByteArray(): ByteArray = buf.copyOf(size)
    }
}
