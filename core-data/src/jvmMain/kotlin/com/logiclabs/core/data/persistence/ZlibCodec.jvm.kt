package com.logiclabs.core.data.persistence

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

actual fun zlibCompress(input: ByteArray): ByteArray {
    val deflater = Deflater(Deflater.BEST_COMPRESSION)
    val out = ByteArrayOutputStream()
    try {
        deflater.setInput(input)
        deflater.finish()
        val buf = ByteArray(512)
        while (!deflater.finished()) {
            val count = deflater.deflate(buf)
            out.write(buf, 0, count)
        }
    } finally {
        deflater.end()
    }
    return out.toByteArray()
}

actual fun zlibDecompress(input: ByteArray, maxOutputBytes: Int): ByteArray {
    val inflater = Inflater()
    val out = ByteArrayOutputStream()
    try {
        inflater.setInput(input)
        val buf = ByteArray(512)
        var total = 0
        while (!inflater.finished()) {
            val count = inflater.inflate(buf)
            if (count == 0) {
                if (inflater.needsInput() || inflater.needsDictionary()) break
            }
            total += count
            if (total > maxOutputBytes) {
                throw IllegalArgumentException("Decompressed payload exceeds $maxOutputBytes byte limit")
            }
            out.write(buf, 0, count)
        }
    } catch (e: DataFormatException) {
        throw IllegalArgumentException("Decompression failed (corrupt Deflate stream): ${e.message}", e)
    } finally {
        inflater.end()
    }
    return out.toByteArray()
}
