package com.logiclabs.feature.tools.report

import com.logiclabs.feature.tools.testbench.TestBenchReport
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders a [TestBenchReport] to a one-page A4 PDF (595 x 842 pt) using Apache PDFBox
 * with embedded Inter & JetBrains Mono TrueType fonts (with standard Type1 fallback).
 */
private object JvmPdfBoxCertificate {

    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 48f

    /** Renders [report] to PDF bytes in memory. Returns null on error. */
    fun renderPdfBytes(report: TestBenchReport): ByteArray? {
        return try {
            val baos = ByteArrayOutputStream()
            PDDocument().use { doc ->
                val page = PDPage(PDRectangle.A4)
                doc.addPage(page)

                val sansFont = loadEmbeddedFont(doc, "font/inter_semibold.ttf")
                    ?: PDType1Font(Standard14Fonts.FontName.HELVETICA)
                val sansBoldFont = loadEmbeddedFont(doc, "font/inter_semibold.ttf")
                    ?: PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                val monoFont = loadEmbeddedFont(doc, "font/jetbrains_mono_medium.ttf")
                    ?: PDType1Font(Standard14Fonts.FontName.COURIER)

                PDPageContentStream(doc, page).use { cs ->
                    drawPage(cs, report, sansFont, sansBoldFont, monoFont)
                }
                doc.save(baos)
            }
            baos.toByteArray()
        } catch (_: Throwable) {
            null
        }
    }

    /** Exports [report] directly to [targetFile] and returns true on success. */


    /** Copies [hash] onto the desktop system clipboard. */

    /** Splits the 64-hex-character digest into four grouped 16-character rows. */

    private fun loadEmbeddedFont(doc: PDDocument, resourcePath: String): PDFont? = runCatching {
        val stream = JvmPdfBoxCertificate::class.java.classLoader?.getResourceAsStream(resourcePath)
            ?: return null
        stream.use { PDType0Font.load(doc, it) }
    }.getOrNull()

    private fun drawPage(
        cs: PDPageContentStream,
        report: TestBenchReport,
        sans: PDFont,
        sansBold: PDFont,
        mono: PDFont
    ) {
        val inkDark = Color(0x10, 0x12, 0x16)
        val inkBody = Color(0x2A, 0x2E, 0x36)
        val ruleColor = Color(0xB8, 0xBC, 0xC4)
        val frameColor = Color(0x3A, 0x3E, 0x47)

        // Convert top-down coordinates to PDF bottom-up coordinates
        fun pdfY(topY: Float): Float = PAGE_HEIGHT - topY

        fun drawLine(x1: Float, topY: Float, x2: Float, color: Color, width: Float = 0.8f) {
            cs.setStrokingColor(color)
            cs.setLineWidth(width)
            cs.moveTo(x1, pdfY(topY))
            cs.lineTo(x2, pdfY(topY))
            cs.stroke()
        }

        fun drawTextLine(text: String, x: Float, topY: Float, font: PDFont, size: Float, color: Color) {
            val safe = sanitizeForFont(text, font)
            if (safe.isEmpty()) return
            cs.beginText()
            cs.setFont(font, size)
            cs.setNonStrokingColor(color)
            cs.newLineAtOffset(x, pdfY(topY))
            cs.showText(safe)
            cs.endText()
        }

        // Outer certificate frame
        cs.setStrokingColor(frameColor)
        cs.setLineWidth(1.4f)
        cs.addRect(
            MARGIN * 0.5f,
            MARGIN * 0.5f,
            PAGE_WIDTH - MARGIN,
            PAGE_HEIGHT - MARGIN
        )
        cs.stroke()

        val contentWidth = PAGE_WIDTH - MARGIN * 2f

        var y = MARGIN + 20f
        drawTextLine(
            ellipsize("LOGIC LABS - VERIFICATION CERTIFICATE", sansBold, 19f, contentWidth),
            MARGIN, y, sansBold, 19f, inkDark
        )
        y += 22f
        y = drawWrapped(
            ::drawTextLine,
            "K&H IDL-800A Digital Logic Trainer · AD-200 Breadboard (Windows Edition)",
            MARGIN, y, contentWidth, sans, 10f, inkBody, maxLines = 2
        )
        y += 4f
        drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, ruleColor)

        y += 22f
        drawTextLine("EXPERIMENT", MARGIN, y, sansBold, 12f, inkDark)
        y += 16f
        y = drawWrapped(
            ::drawTextLine,
            report.experimentTitle,
            MARGIN, y, contentWidth, sans, 10f, inkBody, maxLines = 2
        )
        val issued = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date())
        drawTextLine("Issued: $issued", MARGIN, y, sans, 10f, inkBody)
        y += 14f
        drawTextLine(
            "Result: ${if (report.isAllPassed) "PASS" else "FAIL"}   Vectors: ${report.passedCount} / ${report.totalCount}",
            MARGIN, y, sansBold, 10f, inkDark
        )
        y += 16f

        y += 12f
        drawTextLine("TRUTH TABLE", MARGIN, y, sansBold, 12f, inkDark)
        y += 6f
        drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, ruleColor)
        y += 14f

        val inputHeader = if (report.inputNames.isEmpty()) "IN" else report.inputNames.joinToString(" ")
        val outputHeader = if (report.outputNames.isEmpty()) "OUT" else report.outputNames.joinToString(" ")
        val colVector = MARGIN
        val colInputs = MARGIN + 44f
        val colExpected = MARGIN + 190f
        val colActual = MARGIN + 320f
        val colResult = MARGIN + 440f
        val rightEdge = PAGE_WIDTH - MARGIN
        val gutter = 6f
        val wVector = colInputs - colVector - gutter
        val wInputs = colExpected - colInputs - gutter
        val wExpected = colActual - colExpected - gutter
        val wActual = colResult - colActual - gutter
        val wResult = rightEdge - colResult

        drawTextLine(ellipsize("VECTOR", mono, 9f, wVector), colVector, y, mono, 9f, inkDark)
        drawTextLine(ellipsize("IN [$inputHeader]", mono, 9f, wInputs), colInputs, y, mono, 9f, inkDark)
        drawTextLine(ellipsize("EXPECTED [$outputHeader]", mono, 9f, wExpected), colExpected, y, mono, 9f, inkDark)
        drawTextLine(ellipsize("ACTUAL", mono, 9f, wActual), colActual, y, mono, 9f, inkDark)
        drawTextLine(ellipsize("RESULT", mono, 9f, wResult), colResult, y, mono, 9f, inkDark)
        y += 6f
        drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, ruleColor)
        y += 14f

        val rowStep = 12.5f
        val rowLimit = PAGE_HEIGHT - MARGIN - 190f
        for ((index, row) in report.rows.withIndex()) {
            if (y > rowLimit) {
                drawTextLine(
                    ellipsize("... ${report.rows.size - index} further vectors omitted", mono, 9f, contentWidth),
                    colVector, y, mono, 9f, inkBody
                )
                y += rowStep
                break
            }
            drawTextLine("${index + 1}", colVector, y, mono, 9f, inkBody)
            drawTextLine(ellipsize(bitString(row.inputValues), mono, 9f, wInputs), colInputs, y, mono, 9f, inkBody)
            drawTextLine(ellipsize(bitString(row.expectedOutputs), mono, 9f, wExpected), colExpected, y, mono, 9f, inkBody)
            drawTextLine(ellipsize(bitString(row.actualOutputs), mono, 9f, wActual), colActual, y, mono, 9f, inkBody)
            drawTextLine(if (row.isPassed) "PASS" else "FAIL", colResult, y, mono, 9f, inkDark)
            y += rowStep
        }

        y += 12f
        drawTextLine("CIRCUIT SUMMARY", MARGIN, y, sansBold, 12f, inkDark)
        y += 15f
        y = drawWrapped(
            ::drawTextLine,
            "Inputs: ${report.inputNames.size}   Outputs: ${report.outputNames.size}   Vectors swept: ${report.totalCount}",
            MARGIN, y, contentWidth, sans, 10f, inkBody, maxLines = 2
        )
        val errors = report.diagnostics.count { it.isError }
        drawTextLine("Diagnostics: ${report.diagnostics.size} (errors: $errors)", MARGIN, y, sans, 10f, inkBody)
        y += 14f

        val bulletX = MARGIN + 8f
        val bulletWidth = contentWidth - 8f
        for (issue in report.diagnostics.take(6)) {
            y = drawWrapped(
                ::drawTextLine,
                "* ${if (issue.isError) "ERROR" else "NOTE"}: ${issue.message}",
                bulletX, y, bulletWidth, mono, 8.5f, inkBody, maxLines = 2
            )
        }

        y += 16f
        drawTextLine("HMAC-SHA256 PROVENANCE SEAL", MARGIN, y, sansBold, 12f, inkDark)
        y += 6f
        drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, ruleColor)
        y += 14f
        for (group in LabCertificatePdf.hashRows(report.hmacSha256Hash)) {
            drawTextLine(group, MARGIN, y, mono, 9f, inkBody)
            y += 12.5f
        }

        val footerLines = wrap(
            "This document is generated from a signed simulation run. The seal binds the input vectors to the observed outputs.",
            mono, 8.5f, contentWidth
        )
        var footerY = PAGE_HEIGHT - MARGIN - 12f * (footerLines.size - 1)
        for (line in footerLines) {
            drawTextLine(line, MARGIN, footerY, mono, 8.5f, inkBody)
            footerY += 12f
        }
    }

    private fun measureText(text: String, font: PDFont, size: Float): Float {
        val safe = sanitizeForFont(text, font)
        if (safe.isEmpty()) return 0f
        return runCatching { font.getStringWidth(safe) / 1000f * size }
            .getOrDefault(safe.length * size * 0.6f)
    }

    private fun sanitizeForFont(text: String, font: PDFont): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (ch == '\n' || ch == '\r' || ch == '\t') {
                sb.append(' ')
                continue
            }
            val ok = runCatching {
                font.encode(ch.toString())
                true
            }.getOrDefault(false)
            if (ok) {
                sb.append(ch)
            } else {
                sb.append(
                    when (ch) {
                        '—', '–', '−' -> '-'
                        '·', '•' -> '*'
                        '…' -> "..."
                        else -> '?'
                    }
                )
            }
        }
        return sb.toString()
    }

    private fun ellipsize(text: String, font: PDFont, size: Float, maxWidth: Float): String {
        if (maxWidth <= 0f) return ""
        if (measureText(text, font, size) <= maxWidth) return text
        val ellipsis = "..."
        val room = maxWidth - measureText(ellipsis, font, size)
        if (room <= 0f) return ellipsis
        var end = text.length
        while (end > 0 && measureText(text.substring(0, end), font, size) > room) {
            end--
        }
        return text.substring(0, end).trimEnd() + ellipsis
    }

    private fun wrap(text: String, font: PDFont, size: Float, maxWidth: Float): List<String> {
        if (maxWidth <= 0f || text.isEmpty()) return listOf(text)
        if (measureText(text, font, size) <= maxWidth) return listOf(text)
        val lines = mutableListOf<String>()
        var rest = text
        while (rest.isNotEmpty()) {
            if (measureText(rest, font, size) <= maxWidth) {
                lines += rest
                break
            }
            var fits = rest.length
            while (fits > 1 && measureText(rest.substring(0, fits), font, size) > maxWidth) {
                fits--
            }
            val space = rest.lastIndexOf(' ', fits.coerceAtMost(rest.length - 1))
            val cut = if (space > fits / 3) space else fits
            lines += rest.substring(0, cut).trimEnd()
            rest = rest.substring(cut).trimStart()
        }
        return lines
    }

    private fun drawWrapped(
        drawFn: (String, Float, Float, PDFont, Float, Color) -> Unit,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        font: PDFont,
        size: Float,
        color: Color,
        maxLines: Int = 3
    ): Float {
        val lh = size * 1.38f
        val lines = wrap(text, font, size, maxWidth)
        var baseline = y
        for ((i, line) in lines.withIndex()) {
            if (i == maxLines - 1 && lines.size > maxLines) {
                val tail = lines.drop(i).joinToString(" ")
                drawFn(ellipsize(tail, font, size, maxWidth), x, baseline, font, size, color)
                return baseline + lh
            }
            if (i >= maxLines) break
            drawFn(line, x, baseline, font, size, color)
            baseline += lh
        }
        return baseline
    }

    private fun bitString(values: List<Boolean>): String {
        if (values.isEmpty()) return "-"
        val sb = StringBuilder(values.size)
        for (v in values) sb.append(if (v) '1' else '0')
        return sb.toString()
    }
}

internal actual fun renderPlatformPdfBytes(report: TestBenchReport): ByteArray? =
    JvmPdfBoxCertificate.renderPdfBytes(report)

internal actual fun copyHashPlatform(hash: String) {
    try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(hash), null)
    } catch (_: Throwable) {
    }
}

/** Exports [report] directly to [targetFile] on Desktop JVM and returns true on success. */
fun LabCertificatePdf.exportToFile(targetFile: File, report: TestBenchReport): Boolean {
    val bytes = renderPdfBytes(report) ?: return false
    return try {
        targetFile.parentFile?.mkdirs()
        targetFile.writeBytes(bytes)
        true
    } catch (_: Throwable) {
        false
    }
}
