package com.logiclabs.feature.tools.report

import com.logiclabs.feature.tools.testbench.TestBenchReport

internal expect fun webCopyToClipboard(text: String)

internal actual fun renderPlatformPdfBytes(report: TestBenchReport): ByteArray? =
    runCatching { PureKotlinPdfWriter.renderA4Certificate(report) }.getOrNull()

internal actual fun copyHashPlatform(hash: String) = webCopyToClipboard(hash)
