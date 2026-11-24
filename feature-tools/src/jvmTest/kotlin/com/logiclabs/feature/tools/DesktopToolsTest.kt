package com.logiclabs.feature.tools

import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.feature.tools.courseware.LabCurriculum
import com.logiclabs.feature.tools.feedback.ConsoleSfx
import com.logiclabs.feature.tools.report.LabCertificatePdf
import com.logiclabs.feature.tools.report.exportToFile
import com.logiclabs.feature.tools.testbench.TestBenchVerifier
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DesktopToolsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun consoleSfx_synthesizesNonSilentPcmWaveforms() {
        val click = ConsoleSfx.synthClick()
        val pop = ConsoleSfx.synthPop()
        val tick = ConsoleSfx.synthTick()
        assertTrue(click.isNotEmpty())
        assertTrue(pop.isNotEmpty())
        assertTrue(tick.isNotEmpty())
        assertTrue(click.any { it != 0.toShort() })
        assertTrue(pop.any { it != 0.toShort() })
        assertTrue(tick.any { it != 0.toShort() })
    }

    @Test
    fun labCertificatePdf_generatesValidPdfDocument() {
        val lab = LabCurriculum.classicLabs.first()
        val circuit = BreadboardCircuit()
        lab.buildCircuit(circuit)
        val report = TestBenchVerifier.verify(
            circuit = circuit,
            switchIndices = lab.switchIndices,
            outputReader = { lab.ledIndices.map { circuit.ledValues[it] } },
            expectedFunction = lab.expectedFunction,
            inputNames = lab.inputLabels,
            outputNames = lab.outputLabels,
            experimentTitle = lab.title,
            ledIndices = lab.ledIndices
        )
        assertTrue(report.isAllPassed)

        val bytes = LabCertificatePdf.renderPdfBytes(report)
        assertNotNull("Skia PDF generation returned null", bytes)
        assertTrue("PDF too small: ${bytes!!.size}", bytes.size > 500)
        val header = String(bytes, 0, 5, Charsets.US_ASCII)
        assertTrue("Expected %PDF- header, got: $header", header.startsWith("%PDF-"))

        val outFile = File(tempFolder.root, "cert.pdf")
        assertTrue(LabCertificatePdf.exportToFile(outFile, report))
        assertTrue(outFile.length() > 500)
    }
}
