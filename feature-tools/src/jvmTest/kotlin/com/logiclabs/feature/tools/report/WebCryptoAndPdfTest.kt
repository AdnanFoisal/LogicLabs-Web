package com.logiclabs.feature.tools.report

import com.logiclabs.feature.tools.testbench.CryptoSha256
import com.logiclabs.feature.tools.testbench.TestBenchReport
import com.logiclabs.feature.tools.testbench.TruthTableRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Verifies that pure-Kotlin HMAC-SHA256 matches JVM JCA bit-for-bit and that
 * PureKotlinPdfWriter produces a valid A4 PDF 1.4 certificate for Web browsers.
 */
class WebCryptoAndPdfTest {

    @Test
    fun pureKotlinHmacSha256_matchesJcaBitForBit() {
        val secret = "LOGICLABS-VERIFY-KEY-V1"
        val samples = listOf(
            "",
            "V=0:IN=0:OUT=1;V=1:IN=1:OUT=0;",
            "V=0:IN=00:OUT=00;V=1:IN=01:OUT=10;V=2:IN=10:OUT=10;V=3:IN=11:OUT=01;",
            "A".repeat(500)
        )
        for (payload in samples) {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
            val expected = mac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }
            val actual = CryptoSha256.hmacSha256Hex(secret.encodeToByteArray(), payload.encodeToByteArray())
            assertEquals("HMAC-SHA256 mismatch for payload len=${payload.length}", expected, actual)
        }
    }

    @Test
    fun pureKotlinPdfWriter_generatesValidPdf14Document() {
        val report = TestBenchReport(
            rows = listOf(
                TruthTableRow(listOf(false, false), listOf(false, false), listOf(false, false), true),
                TruthTableRow(listOf(false, true), listOf(true, false), listOf(true, false), true),
                TruthTableRow(listOf(true, false), listOf(true, false), listOf(true, false), true),
                TruthTableRow(listOf(true, true), listOf(false, true), listOf(false, true), true)
            ),
            passedCount = 4,
            totalCount = 4,
            isAllPassed = true,
            hmacSha256Hash = "0123456789abcdef".repeat(4),
            diagnostics = emptyList(),
            inputNames = listOf("A (SW0)", "B (SW1)"),
            outputNames = listOf("SUM (LED0)", "CARRY (LED1)"),
            experimentTitle = "Half Adder Binary Arithmetic",
            sweepDurationMs = 2L
        )
        val bytes = PureKotlinPdfWriter.renderA4Certificate(report)
        val text = bytes.decodeToString()
        assertTrue(text.startsWith("%PDF-1.4\n"))
        assertTrue(text.trimEnd().endsWith("%%EOF"))
        assertTrue(text.contains("Half Adder Binary Arithmetic"))
        assertTrue(text.contains("VERIFIED PASS"))
        assertTrue(text.contains("0123 4567 89ab cdef"))
    }
}
