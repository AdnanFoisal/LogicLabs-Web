package com.logiclabs.app.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.logiclabs.app.di.AppContainer
import com.logiclabs.app.ui.dialogs.ControlsGuideDialog
import com.logiclabs.app.ui.screens.bench.LogicLabsMainScreen
import com.logiclabs.app.ui.screens.home.HomeScreen
import com.logiclabs.app.ui.screens.home.HomeTab
import com.logiclabs.app.ui.screens.settings.SettingsScreen
import com.logiclabs.app.ui.screens.splash.SplashScreen

sealed interface DesktopDestination {
    data object Splash : DesktopDestination
    data object Home : DesktopDestination
    data class Bench(val request: BenchRequest) : DesktopDestination
    data object Settings : DesktopDestination
}

/**
 * Callbacks registered by the active [LogicLabsMainScreen] so the top Windows MenuBar
 * can invoke every bench command directly without coupling.
 */
class BenchMenuActions(
    val canUndo: () -> Boolean,
    val canRedo: () -> Boolean,
    val undo: () -> Unit,
    val redo: () -> Unit,
    val save: () -> Unit,
    val saveAs: () -> Unit,
    val exportJson: () -> Unit,
    val shareCode: () -> Unit,
    val openChipCatalog: () -> Unit,
    val openLabCatalog: () -> Unit,
    val openScope: () -> Unit,
    val toggleDiagram: () -> Unit,
    val isViewingDiagram: () -> Boolean,
    val runVerify: () -> Unit,
    val togglePower: () -> Unit,
    val toggleClock: () -> Unit,
    val stepClock: () -> Unit,
    val toggleSimulationMode: () -> Unit,
    val toggleConsole: () -> Unit,
    val isConsoleCollapsed: () -> Boolean,
    val toggleLoupe: () -> Unit,
    val isLoupeEnabled: () -> Boolean,
    val fitView: () -> Unit,
    val zoomIn: () -> Unit,
    val zoomOut: () -> Unit,
    val clearWires: () -> Unit,
    val resetBoard: () -> Unit,
    val requestLeaveBench: () -> Unit
)

/**
 * Shared desktop shell navigation & menu controller owned by `Main.kt`.
 */
@Stable
class DesktopShellController {
    var destination: DesktopDestination by mutableStateOf(DesktopDestination.Splash)
    var homeTab: HomeTab by mutableStateOf(HomeTab.PROJECTS)
    var showControlsGuide: Boolean by mutableStateOf(false)
    var importJsonTrigger: Int by mutableIntStateOf(0)
    var benchActions: BenchMenuActions? by mutableStateOf(null)

    fun openBench(request: BenchRequest) {
        destination = DesktopDestination.Bench(request)
    }

    fun openHome(tab: HomeTab = homeTab) {
        homeTab = tab
        destination = DesktopDestination.Home
    }

    fun triggerImportJson() {
        if (destination !is DesktopDestination.Home) {
            homeTab = HomeTab.PROJECTS
            destination = DesktopDestination.Home
        }
        importJsonTrigger++
    }
}

/**
 * Pure Compose Desktop navigation shell: Splash → Home ↔ Bench / Settings,
 * using clean cross-fades and hosting the global [ControlsGuideDialog] (`F1`).
 */
@Composable
fun LogicLabsNavHost(
    container: AppContainer,
    controller: DesktopShellController
) {
    Crossfade(
        targetState = controller.destination,
        animationSpec = tween(220),
        label = "desktopNavCrossfade"
    ) { dest ->
        when (dest) {
            DesktopDestination.Splash -> {
                SplashScreen(
                    onFinished = {
                        controller.destination = DesktopDestination.Home
                    }
                )
            }

            DesktopDestination.Home -> {
                HomeScreen(
                    container = container,
                    selectedTab = controller.homeTab,
                    onTabSelected = { controller.homeTab = it },
                    onContinue = { request -> controller.openBench(request) },
                    onOpenControlsGuide = { controller.showControlsGuide = true },
                    importJsonTrigger = controller.importJsonTrigger
                )
            }

            is DesktopDestination.Bench -> {
                LogicLabsMainScreen(
                    benchRequest = dest.request,
                    container = container,
                    onNavigateHome = { controller.openHome() },
                    onOpenControlsGuide = { controller.showControlsGuide = true },
                    onRegisterMenuActions = { actions -> controller.benchActions = actions },
                    externalModalOpen = controller.showControlsGuide
                )
            }

            DesktopDestination.Settings -> {
                SettingsScreen(
                    container = container,
                    onBack = { controller.openHome() },
                    showBackButton = true,
                    onOpenControlsGuide = { controller.showControlsGuide = true }
                )
            }
        }
    }

    if (controller.showControlsGuide) {
        ControlsGuideDialog(
            onDismiss = { controller.showControlsGuide = false }
        )
    }
}
