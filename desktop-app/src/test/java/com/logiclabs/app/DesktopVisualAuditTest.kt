package com.logiclabs.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.logiclabs.app.di.AppContainer
import com.logiclabs.app.ui.dialogs.ControlsGuideDialog
import com.logiclabs.app.ui.navigation.BenchRequest
import com.logiclabs.app.ui.screens.bench.LogicLabsMainScreen
import com.logiclabs.app.ui.screens.home.HomeScreen
import com.logiclabs.app.ui.screens.home.HomeTab
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.designsystem.theme.HardwareTheme
import com.logiclabs.core.designsystem.theme.LocalRenderEffects
import com.logiclabs.core.designsystem.theme.LogicLabsTheme
import com.logiclabs.feature.tools.courseware.LabCurriculum
import com.logiclabs.feature.tools.diagram.BooleanDiagramView
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DesktopVisualAuditTest {

    private fun renderPng(
        width: Int,
        height: Int,
        density: Float = 1f,
        outFile: File?,
        frames: Int = 6,
        content: @androidx.compose.runtime.Composable () -> Unit
    ): ByteArray {
        val scene = ImageComposeScene(
            width = width,
            height = height,
            density = Density(density),
            content = {
                LogicLabsTheme(hardwareTheme = HardwareTheme.OBSIDIAN) {
                    CompositionLocalProvider(LocalRenderEffects provides true) {
                        content()
                    }
                }
            }
        )
        return try {
            var img = scene.render(0L)
            for (i in 1..frames) {
                img = scene.render(i * 16_000_000L)
            }
            val bytes = img.encodeToData(EncodedImageFormat.PNG)!!.bytes
            if (outFile != null) {
                outFile.parentFile?.mkdirs()
                outFile.writeBytes(bytes)
            }
            bytes
        } finally {
            scene.close()
        }
    }

    @Test
    fun `renders Home, Coursework, IC Reference, Workbench, Schematic, and Controls Guide cleanly`() {
        val capture = System.getProperty("logiclabs.captureScreenshots") == "true"
        val outDir = if (capture) File("build/screenshots").apply { mkdirs() } else null
        val tempDataDir = Files.createTempDirectory("logiclabs-visual-test").toFile()

        try {
            val container = AppContainer(tempDataDir)

            // 1. Home (Projects) at 1366x768 laptop resolution
            val homePng = renderPng(1366, 768, 1f, outDir?.let { File(it, "01_home_1366x768.png") }) {
                HomeScreen(
                    container = container,
                    selectedTab = HomeTab.PROJECTS,
                    onTabSelected = {},
                    onContinue = {},
                    onOpenControlsGuide = {}
                )
            }
            assertTrue("Home screenshot must be non-empty PNG", homePng.size > 5_000)

            // 2. Coursework Tab at 1366x768
            val courseworkPng = renderPng(1366, 768, 1f, outDir?.let { File(it, "02_coursework_1366x768.png") }) {
                HomeScreen(
                    container = container,
                    selectedTab = HomeTab.COURSEWORK,
                    onTabSelected = {},
                    onContinue = {},
                    onOpenControlsGuide = {}
                )
            }
            assertTrue(courseworkPng.size > 5_000)

            // 3. 74xx IC Reference Tab at 1366x768
            val icRefPng = renderPng(1366, 768, 1f, outDir?.let { File(it, "03_ic_reference_1366x768.png") }) {
                HomeScreen(
                    container = container,
                    selectedTab = HomeTab.REFERENCE,
                    onTabSelected = {},
                    onContinue = {},
                    onOpenControlsGuide = {}
                )
            }
            assertTrue(icRefPng.size > 5_000)

            // 4. Settings Tab at 1366x768
            val settingsPng = renderPng(1366, 768, 1f, outDir?.let { File(it, "04_settings_1366x768.png") }) {
                HomeScreen(
                    container = container,
                    selectedTab = HomeTab.SETTINGS,
                    onTabSelected = {},
                    onContinue = {},
                    onOpenControlsGuide = {}
                )
            }
            assertTrue(settingsPng.size > 5_000)

            // 5. Workbench with Full Adder Lab at 1366x768 Laptop AND 1600x960 PC Monitor
            val fullAdderId = LabCurriculum.classicLabs.firstOrNull { it.labNumber == 7 }?.id
                ?: LabCurriculum.classicLabs.first().id

            val benchLaptopPng = renderPng(1366, 768, 1f, outDir?.let { File(it, "05_workbench_laptop_1366x768.png") }, frames = 10) {
                LogicLabsMainScreen(
                    benchRequest = BenchRequest.Lab(fullAdderId),
                    container = container,
                    onNavigateHome = {}
                )
            }
            assertTrue(benchLaptopPng.size > 10_000)

            val benchDesktopPng = renderPng(1600, 960, 1.25f, outDir?.let { File(it, "06_workbench_hidpi_1600x960.png") }, frames = 10) {
                LogicLabsMainScreen(
                    benchRequest = BenchRequest.Lab(fullAdderId),
                    container = container,
                    onNavigateHome = {}
                )
            }
            assertTrue(benchDesktopPng.size > 10_000)

            // 6. Live Gate-Level Boolean Schematic View
            val schematicCircuit = BreadboardCircuit().apply {
                LabCurriculum.classicLabs.first { it.id == fullAdderId }.buildCircuit(this)
                step()
            }
            val schematicPng = renderPng(1366, 768, 1f, outDir?.let { File(it, "07_schematic_1366x768.png") }) {
                BooleanDiagramView(
                    circuit = schematicCircuit,
                    circuitVersion = 1,
                    renderVersion = 1
                )
            }
            assertTrue(schematicPng.size > 5_000)

            // 7. Controls & Keyboard Shortcuts Guide Overlay
            val controlsPng = renderPng(1366, 768, 1f, outDir?.let { File(it, "08_controls_guide_1366x768.png") }) {
                HomeScreen(
                    container = container,
                    selectedTab = HomeTab.PROJECTS,
                    onTabSelected = {},
                    onContinue = {},
                    onOpenControlsGuide = {}
                )
                ControlsGuideDialog(onDismiss = {})
            }
            assertTrue(controlsPng.size > 5_000)
        } finally {
            tempDataDir.deleteRecursively()
        }
    }
}
