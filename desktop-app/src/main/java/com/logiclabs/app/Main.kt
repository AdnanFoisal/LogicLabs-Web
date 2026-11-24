package com.logiclabs.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.logiclabs.app.desktop.DesktopPlatform
import com.logiclabs.app.di.AppContainer
import com.logiclabs.app.di.dataDir
import com.logiclabs.app.ui.navigation.BenchRequest
import com.logiclabs.app.ui.navigation.DesktopDestination
import com.logiclabs.app.ui.navigation.DesktopShellController
import com.logiclabs.app.ui.navigation.LogicLabsNavHost
import com.logiclabs.app.ui.screens.home.HomeTab
import com.logiclabs.core.data.settings.Settings
import com.logiclabs.core.data.settings.ThemeMode
import com.logiclabs.core.designsystem.theme.HardwareTheme
import com.logiclabs.core.designsystem.theme.LocalRenderEffects
import com.logiclabs.core.designsystem.theme.LogicLabsTheme
import com.logiclabs.feature.instruments.ui.hardware.PanelHaptics
import com.logiclabs.feature.tools.feedback.Haptics
import com.logiclabs.feature.tools.feedback.LocalConsoleSfx
import com.logiclabs.feature.tools.feedback.rememberConsoleSfx
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import javax.swing.UIManager

fun main() {
    runCatching {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }

    val container = AppContainer()
    val initialSize = computeInitialWindowSize()

    application {
        val controller = remember { DesktopShellController() }
        val appIcon = remember { loadAppIconPainter() }
        val windowState = rememberWindowState(
            size = initialSize,
            position = WindowPosition(Alignment.Center)
        )

        Window(
            onCloseRequest = { exitApplication() },
            state = windowState,
            title = "Logic Labs — IDL-800A Digital Electronics Workstation",
            icon = appIcon,
            onPreviewKeyEvent = { ev ->
                if (ev.type != KeyEventType.KeyDown) return@Window false
                val ctrl = ev.isCtrlPressed || ev.isMetaPressed
                when {
                    ev.key == Key.F1 -> {
                        controller.showControlsGuide = !controller.showControlsGuide
                        true
                    }
                    ctrl && ev.key == Key.N -> {
                        controller.openBench(BenchRequest.Sandbox)
                        true
                    }
                    ctrl && ev.key == Key.O -> {
                        controller.triggerImportJson()
                        true
                    }
                    ctrl && ev.key == Key.One && controller.destination == DesktopDestination.Home -> {
                        controller.homeTab = HomeTab.PROJECTS
                        true
                    }
                    ctrl && ev.key == Key.Two && controller.destination == DesktopDestination.Home -> {
                        controller.homeTab = HomeTab.COURSEWORK
                        true
                    }
                    ctrl && ev.key == Key.Three && controller.destination == DesktopDestination.Home -> {
                        controller.homeTab = HomeTab.REFERENCE
                        true
                    }
                    ctrl && ev.key == Key.Four && controller.destination == DesktopDestination.Home -> {
                        controller.homeTab = HomeTab.SETTINGS
                        true
                    }
                    else -> false
                }
            }
        ) {
            // Safe minimum size so laptop screens (1366x768 @ 125% DPI) never clip below taskbar
            LaunchedEffect(window) {
                window.minimumSize = Dimension(900, 560)
            }

            val bench = controller.benchActions
            val onBench = bench != null

            MenuBar {
                Menu("File", mnemonic = 'F') {
                    Item(
                        "New Sandbox Bench",
                        shortcut = KeyShortcut(Key.N, ctrl = true),
                        onClick = { controller.openBench(BenchRequest.Sandbox) }
                    )
                    Item(
                        "Import Project (.json)...",
                        shortcut = KeyShortcut(Key.O, ctrl = true),
                        onClick = { controller.triggerImportJson() }
                    )
                    Separator()
                    Item(
                        "Save Project",
                        enabled = onBench,
                        shortcut = KeyShortcut(Key.S, ctrl = true),
                        onClick = { bench?.save?.invoke() }
                    )
                    Item(
                        "Save Project As...",
                        enabled = onBench,
                        shortcut = KeyShortcut(Key.S, ctrl = true, shift = true),
                        onClick = { bench?.saveAs?.invoke() }
                    )
                    Item(
                        "Export Circuit (.json)...",
                        enabled = onBench,
                        shortcut = KeyShortcut(Key.E, ctrl = true),
                        onClick = { bench?.exportJson?.invoke() }
                    )
                    Item(
                        "Share Circuit Bytecode...",
                        enabled = onBench,
                        onClick = { bench?.shareCode?.invoke() }
                    )
                    Separator()
                    Item(
                        "Open Projects Folder",
                        onClick = { DesktopPlatform.openFolderInExplorer(container.dataDir) }
                    )
                    Item(
                        "Return to Home",
                        enabled = onBench,
                        onClick = { bench?.requestLeaveBench?.invoke() }
                    )
                    Separator()
                    Item(
                        "Exit",
                        onClick = { exitApplication() }
                    )
                }

                Menu("Edit", mnemonic = 'E') {
                    Item(
                        "Undo",
                        enabled = bench?.canUndo?.invoke() == true,
                        shortcut = KeyShortcut(Key.Z, ctrl = true),
                        onClick = { bench?.undo?.invoke() }
                    )
                    Item(
                        "Redo",
                        enabled = bench?.canRedo?.invoke() == true,
                        shortcut = KeyShortcut(Key.Y, ctrl = true),
                        onClick = { bench?.redo?.invoke() }
                    )
                    Separator()
                    Item(
                        "Clear All Wires",
                        enabled = onBench,
                        onClick = { bench?.clearWires?.invoke() }
                    )
                    Item(
                        "Reset Entire Breadboard",
                        enabled = onBench,
                        onClick = { bench?.resetBoard?.invoke() }
                    )
                }

                Menu("View", mnemonic = 'V') {
                    Item(
                        if (bench?.isConsoleCollapsed?.invoke() == true) "Show Trainer Console Rack" else "Hide Trainer Console Rack",
                        enabled = onBench,
                        onClick = { bench?.toggleConsole?.invoke() }
                    )
                    Item(
                        if (bench?.isViewingDiagram?.invoke() == true) "Switch to Breadboard View" else "Switch to Live Boolean Schematic",
                        enabled = onBench,
                        onClick = { bench?.toggleDiagram?.invoke() }
                    )
                    Separator()
                    Item(
                        "Fit Circuit to View",
                        enabled = onBench,
                        onClick = { bench?.fitView?.invoke() }
                    )
                    Item(
                        "Zoom In",
                        enabled = onBench,
                        onClick = { bench?.zoomIn?.invoke() }
                    )
                    Item(
                        "Zoom Out",
                        enabled = onBench,
                        onClick = { bench?.zoomOut?.invoke() }
                    )
                    Item(
                        if (bench?.isLoupeEnabled?.invoke() == true) "Disable 2.2x Loupe" else "Enable 2.2x Loupe",
                        enabled = onBench,
                        onClick = { bench?.toggleLoupe?.invoke() }
                    )
                    Separator()
                    Item("Home: Projects", shortcut = KeyShortcut(Key.One, ctrl = true), onClick = { controller.openHome(HomeTab.PROJECTS) })
                    Item("Home: Coursework (49 Labs)", shortcut = KeyShortcut(Key.Two, ctrl = true), onClick = { controller.openHome(HomeTab.COURSEWORK) })
                    Item("Home: 74xx IC Reference", shortcut = KeyShortcut(Key.Three, ctrl = true), onClick = { controller.openHome(HomeTab.REFERENCE) })
                    Item("Home: Settings", shortcut = KeyShortcut(Key.Four, ctrl = true), onClick = { controller.openHome(HomeTab.SETTINGS) })
                }

                Menu("Bench", mnemonic = 'B') {
                    Item(
                        "Place 74xx IC... [C]",
                        enabled = onBench,
                        onClick = { bench?.openChipCatalog?.invoke() }
                    )
                    Item(
                        "Load Lab Experiment... [B]",
                        enabled = onBench,
                        onClick = { bench?.openLabCatalog?.invoke() }
                    )
                    Item(
                        "Dual-Channel Oscilloscope [O]",
                        enabled = onBench,
                        onClick = { bench?.openScope?.invoke() }
                    )
                    Item(
                        "Verify Circuit Truth Table [F5]",
                        enabled = onBench,
                        shortcut = KeyShortcut(Key.F5),
                        onClick = { bench?.runVerify?.invoke() }
                    )
                    Separator()
                    Item(
                        "Toggle Master +5V Power [P]",
                        enabled = onBench,
                        onClick = { bench?.togglePower?.invoke() }
                    )
                    Item(
                        "Start / Pause Clock [K]",
                        enabled = onBench,
                        onClick = { bench?.toggleClock?.invoke() }
                    )
                    Item(
                        "Single-Step Clock [.]",
                        enabled = onBench,
                        onClick = { bench?.stepClock?.invoke() }
                    )
                    Separator()
                    Item(
                        "Toggle IDEAL / PRACTICAL Mode",
                        enabled = onBench,
                        onClick = { bench?.toggleSimulationMode?.invoke() }
                    )
                }

                Menu("Help", mnemonic = 'H') {
                    Item(
                        "Controls & Keyboard Shortcuts",
                        shortcut = KeyShortcut(Key.F1),
                        onClick = { controller.showControlsGuide = true }
                    )
                    Separator()
                    Item(
                        "Open Projects & Data Folder",
                        onClick = { DesktopPlatform.openFolderInExplorer(container.dataDir) }
                    )
                }
            }

            LogicLabsDesktopApp(container = container, controller = controller)
        }
    }
}

@Composable
private fun LogicLabsDesktopApp(
    container: AppContainer,
    controller: DesktopShellController
) {
    val settings by container.settings.settings.collectAsState(initial = Settings())
    val sfx = rememberConsoleSfx()

    DisposableEffect(sfx) {
        Haptics.sfx = sfx
        PanelHaptics.onTickSound = { sfx.click() }
        PanelHaptics.onHeavySound = { sfx.pop() }
        onDispose {
            Haptics.sfx = null
            PanelHaptics.onTickSound = null
            PanelHaptics.onHeavySound = null
        }
    }

    LaunchedEffect(settings.soundEnabled) {
        sfx.muted = !settings.soundEnabled
        Haptics.enabled = settings.soundEnabled
    }

    val systemDark = isSystemInDarkTheme()
    val hardwareTheme = when (settings.themeMode) {
        ThemeMode.OBSIDIAN -> HardwareTheme.OBSIDIAN
        ThemeMode.AMBER_CRT -> HardwareTheme.AMBER_CRT
        ThemeMode.HP_SLATE -> HardwareTheme.HP_SLATE
        ThemeMode.CLEANROOM -> HardwareTheme.CLEANROOM
        ThemeMode.CYBERPUNK_NEON -> HardwareTheme.CYBERPUNK_NEON
        ThemeMode.TOKYO_NIGHT -> HardwareTheme.TOKYO_NIGHT
        ThemeMode.SOLARIZED_DARK -> HardwareTheme.SOLARIZED_DARK
        ThemeMode.VINTAGE_BRITISH_LAB -> HardwareTheme.VINTAGE_BRITISH_LAB
        ThemeMode.TITANIUM_FROST -> HardwareTheme.TITANIUM_FROST
        ThemeMode.SYSTEM -> if (systemDark) HardwareTheme.OBSIDIAN else HardwareTheme.CLEANROOM
    }

    LogicLabsTheme(hardwareTheme = hardwareTheme) {
        CompositionLocalProvider(
            LocalConsoleSfx provides sfx,
            LocalRenderEffects provides settings.bloomEffects
        ) {
            LogicLabsNavHost(container = container, controller = controller)
        }
    }
}

/**
 * Computes a comfortable default window size clamped to the user's actual usable display
 * bounds (excluding Windows taskbar) so 1366×768 laptops at 100%–150% DPI never clip,
 * while 1080p/1440p/4K PC monitors open spaciously at 1280×800.
 */
private fun computeInitialWindowSize(): DpSize {
    return runCatching {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val usable = ge.maximumWindowBounds
        val w = usable.width.coerceAtMost(1320).coerceAtLeast(940).let {
            if (usable.width > 0 && it > usable.width - 24) (usable.width - 24).coerceAtLeast(880) else it
        }
        val h = usable.height.coerceAtMost(820).coerceAtLeast(600).let {
            if (usable.height > 0 && it > usable.height - 24) (usable.height - 24).coerceAtLeast(560) else it
        }
        DpSize(w.dp, h.dp)
    }.getOrDefault(DpSize(1280.dp, 800.dp))
}

private fun loadAppIconPainter(): Painter? = runCatching {
    val stream = DesktopShellController::class.java.classLoader?.getResourceAsStream("icon.png")
        ?: return null
    BitmapPainter(stream.use { loadImageBitmap(it) })
}.getOrNull()
