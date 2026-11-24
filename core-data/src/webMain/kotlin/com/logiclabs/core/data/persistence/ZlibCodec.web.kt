package com.logiclabs.core.data.persistence

actual fun zlibCompress(input: ByteArray): ByteArray = PureKotlinZlib.compress(input)

actual fun zlibDecompress(input: ByteArray, maxOutputBytes: Int): ByteArray =
    PureKotlinZlib.decompress(input, maxOutputBytes)
