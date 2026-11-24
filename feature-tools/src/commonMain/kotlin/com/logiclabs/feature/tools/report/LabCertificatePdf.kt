package com.logiclabs.feature.tools.report

import com.logiclabs.core.bridge.platform.currentTimeMillis
import com.logiclabs.feature.tools.testbench.TestBenchReport

internal expect fun renderPlatformPdfBytes(report: TestBenchReport): ByteArray?

internal expect fun copyHashPlatform(hash: String)

/**
 * Cross-platform A4 Lab Certificate PDF renderer & verification seal helper.
 *
 * - JVM Desktop: renders via Apache PDFBox with embedded Inter & JetBrains Mono fonts.
 * - Web (WasmGC / JS): renders a standards-compliant A4 PDF 1.4 document directly in-browser
 *   via [PureKotlinPdfWriter] (zero server, works 100% offline in PWA mode).
 */
object LabCertificatePdf {

    /** Renders [report] to A4 PDF bytes in memory. Returns null on error. */
    fun renderPdfBytes(report: TestBenchReport): ByteArray? = renderPlatformPdfBytes(report)

    fun suggestedFileName(): String {
        val stamp = UtcTimestamp.formatFileStamp(currentTimeMillis())
        return "logic-labs-report-$stamp.pdf"
    }

    /** Copies [hash] onto the system / browser clipboard. */
    fun copyHashToClipboard(hash: String) = copyHashPlatform(hash)

    /** Splits the 64-hex-character digest into four grouped 16-character rows. */
    fun hashRows(hash: String): List<String> {
        if (hash.isEmpty()) return listOf("(unsealed)")
        return hash.chunked(16).map { row -> row.chunked(4).joinToString(" ") }
    }
}

/**
 * Lightweight pure-Kotlin PDF 1.4 A4 (595 x 842 pt) lab verification certificate writer.
 * Uses built-in PDF Type1 Helvetica / Helvetica-Bold / Courier fonts so it works with zero
 * dependencies on WasmGC, JS, and JVM.
 */
object PureKotlinPdfWriter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 48

    fun renderA4Certificate(report: TestBenchReport): ByteArray {
        val content = buildString {
            // Top accent header bar
            val passed = report.isAllPassed
            if (passed) append("0.04 0.48 0.34 rg\n") else append("0.72 0.16 0.20 rg\n")
            append("0 ${PAGE_H - 14} $PAGE_W 14 re f\n")

            // Title & metadata
            text(this, "/F2", 18, MARGIN, PAGE_H - 64, 0.08, 0.12, 0.18, "LOGIC LABS — LAB VERIFICATION CERTIFICATE")
            text(this, "/F1", 10, MARGIN, PAGE_H - 84, 0.32, 0.38, 0.46, "K&H IDL-800A Digital Logic Trainer & AD-200 Breadboard Workstation")

            // Status pill box
            val pillText = if (passed) "VERIFIED PASS" else "VERIFICATION FAILED"
            val pillW = if (passed) 118 else 148
            if (passed) append("0.88 0.98 0.93 rg\n") else append("0.99 0.90 0.91 rg\n")
            append("${PAGE_W - MARGIN - pillW} ${PAGE_H - 86} $pillW 26 re f\n")
            text(
                this, "/F2", 10, PAGE_W - MARGIN - pillW + 14, PAGE_H - 77,
                if (passed) 0.02 else 0.68, if (passed) 0.45 else 0.12, if (passed) 0.30 else 0.16,
                pillText
            )

            // Divider rule
            append("0.82 0.86 0.90 RG 0.8 w\n")
            append("$MARGIN ${PAGE_H - 102} m ${PAGE_W - MARGIN} ${PAGE_H - 102} l S\n")

            // Experiment metadata block
            var y = PAGE_H - 128
            text(this, "/F2", 13, MARGIN, y, 0.08, 0.12, 0.18, report.experimentTitle.ifBlank { "Custom Digital Logic Circuit" })
            y -= 20
            val issuedUtc = UtcTimestamp.formatDisplay(currentTimeMillis()) + " UTC"
            text(
                this, "/F3", 9, MARGIN, y, 0.25, 0.32, 0.40,
                "VECTORS: ${report.passedCount}/${report.totalCount} PASSED   |   SWEEP: ${report.sweepDurationMs} ms   |   ISSUED: $issuedUtc"
            )
            y -= 16
            text(
                this, "/F3", 9, MARGIN, y, 0.25, 0.32, 0.40,
                "INPUTS: ${report.inputNames.joinToString(", ")}   |   OUTPUTS: ${report.outputNames.joinToString(", ")}"
            )

            // Truth table header
            y -= 28
            append("0.93 0.95 0.97 rg\n")
            append("$MARGIN ${y - 6} ${PAGE_W - MARGIN * 2} 20 re f\n")
            text(this, "/F2", 9, MARGIN + 8, y, 0.12, 0.18, 0.24, "#")
            text(this, "/F2", 9, MARGIN + 42, y, 0.12, 0.18, 0.24, "INPUTS")
            text(this, "/F2", 9, MARGIN + 185, y, 0.12, 0.18, 0.24, "EXPECTED")
            text(this, "/F2", 9, MARGIN + 315, y, 0.12, 0.18, 0.24, "ACTUAL")
            text(this, "/F2", 9, MARGIN + 435, y, 0.12, 0.18, 0.24, "RESULT")
            y -= 20

            val maxRows = 24
            val shownRows = report.rows.take(maxRows)
            shownRows.forEachIndexed { idx, row ->
                if (idx % 2 == 1) {
                    append("0.97 0.98 0.99 rg\n")
                    append("$MARGIN ${y - 4} ${PAGE_W - MARGIN * 2} 16 re f\n")
                }
                val inBits = row.inputValues.joinToString(" ") { if (it) "1" else "0" }
                val expBits = row.expectedOutputs.joinToString(" ") { if (it) "1" else "0" }
                val actBits = row.actualOutputs.joinToString(" ") { if (it) "1" else "0" }
                text(this, "/F3", 9, MARGIN + 8, y, 0.25, 0.30, 0.38, "${idx + 1}")
                text(this, "/F3", 9, MARGIN + 42, y, 0.10, 0.14, 0.20, inBits)
                text(this, "/F3", 9, MARGIN + 185, y, 0.10, 0.14, 0.20, expBits)
                text(this, "/F3", 9, MARGIN + 315, y, 0.10, 0.14, 0.20, actBits)
                if (row.isPassed) {
                    text(this, "/F2", 9, MARGIN + 435, y, 0.04, 0.48, 0.30, "PASS")
                } else {
                    text(this, "/F2", 9, MARGIN + 435, y, 0.72, 0.14, 0.18, "FAIL")
                }
                y -= 16
            }
            if (report.rows.size > maxRows) {
                text(
                    this, "/F1", 8, MARGIN + 8, y, 0.40, 0.46, 0.54,
                    "... and ${report.rows.size - maxRows} additional verified vectors (all included in HMAC-SHA256 seal below)."
                )
                y -= 18
            }

            // Cryptographic HMAC-SHA256 Verification Seal box
            val boxBottom = (y - 96).coerceAtLeast(88)
            append("0.95 0.97 0.99 rg\n")
            append("$MARGIN $boxBottom ${PAGE_W - MARGIN * 2} 84 re f\n")
            append("0.75 0.82 0.88 RG 0.8 w\n")
            append("$MARGIN $boxBottom ${PAGE_W - MARGIN * 2} 84 re S\n")
            text(this, "/F2", 9, MARGIN + 12, boxBottom + 66, 0.10, 0.16, 0.24, "HMAC-SHA256 TAMPER-EVIDENT VERIFICATION SEAL")
            val hashRows = LabCertificatePdf.hashRows(report.hmacSha256Hash)
            var hy = boxBottom + 48
            for (hRow in hashRows) {
                text(this, "/F3", 9, MARGIN + 12, hy, 0.14, 0.22, 0.30, hRow)
                hy -= 12
            }

            // Footer
            text(
                this, "/F1", 8, MARGIN, 44, 0.45, 0.50, 0.58,
                "Generated offline by Logic Labs (K&H IDL-800A Digital Electronics Workstation) — GPL-3.0-only"
            )
        }

        val contentBytes = content.encodeToByteArray()
        val objects = listOf(
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $PAGE_W $PAGE_H] /Resources << /Font << /F1 4 0 R /F2 5 0 R /F3 6 0 R >> >> /Contents 7 0 R >>\nendobj\n",
            "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n",
            "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n",
            "6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n",
            "7 0 obj\n<< /Length ${contentBytes.size} >>\nstream\n$content\nendstream\nendobj\n"
        )

        val pdf = StringBuilder()
        pdf.append("%PDF-1.4\n")
        val offsets = IntArray(objects.size + 1)
        for (i in objects.indices) {
            offsets[i + 1] = pdf.length
            pdf.append(objects[i])
        }
        val xrefOffset = pdf.length
        pdf.append("xref\n0 ${objects.size + 1}\n")
        pdf.append("0000000000 65535 f \n")
        for (i in 1..objects.size) {
            pdf.append(offsets[i].toString().padStart(10, '0')).append(" 00000 n \n")
        }
        pdf.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        pdf.append("startxref\n$xrefOffset\n%%EOF\n")
        return pdf.toString().encodeToByteArray()
    }

    private fun text(
        sb: StringBuilder,
        font: String,
        size: Int,
        x: Int,
        y: Int,
        r: Double,
        g: Double,
        b: Double,
        raw: String
    ) {
        val safe = buildString(raw.length) {
            for (ch in raw) {
                when {
                    ch == '\\' -> append("\\\\")
                    ch == '(' -> append("\\(")
                    ch == ')' -> append("\\)")
                    ch.code in 32..126 -> append(ch)
                    ch == '—' || ch == '–' -> append('-')
                    ch == '·' -> append('.')
                    ch == '×' -> append('x')
                    else -> append(' ')
                }
            }
        }
        sb.append("$r $g $b rg BT $font $size Tf $x $y Td ($safe) Tj ET\n")
    }
}
