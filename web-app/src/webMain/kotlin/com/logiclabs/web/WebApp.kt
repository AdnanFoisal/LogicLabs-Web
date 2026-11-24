package com.logiclabs.web

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.logiclabs.app.di.AppContainer
import com.logiclabs.app.ui.navigation.BenchRequest
import com.logiclabs.app.ui.navigation.DesktopDestination
import com.logiclabs.app.ui.navigation.DesktopShellController
import com.logiclabs.app.ui.navigation.LogicLabsNavHost
import com.logiclabs.app.ui.screens.home.HomeTab
import com.logiclabs.core.data.persistence.CircuitShareBytecode
import com.logiclabs.core.data.settings.Settings
import com.logiclabs.core.data.settings.ThemeMode
import com.logiclabs.core.designsystem.theme.CanvasBackground
import com.logiclabs.core.designsystem.theme.HardwareTheme
import com.logiclabs.core.designsystem.theme.LogicLabsTheme
import com.logiclabs.feature.instruments.ui.hardware.PanelHaptics
import com.logiclabs.feature.tools.courseware.ExperimentCatalog
import com.logiclabs.feature.tools.feedback.Haptics
import com.logiclabs.feature.tools.feedback.LocalConsoleSfx
import com.logiclabs.feature.tools.feedback.rememberConsoleSfx
import kotlinx.coroutines.launch

internal expect fun webRegisterBrowserBridges(
    onDroppedJson: (fileName: String, content: String) -> Unit,
    onBrowserBack: () -> Boolean
)

internal expect fun webSyncBrowserState(
    pageTitle: String,
    querySuffix: String,
    pushHistory: Boolean,
    themeColorHex: String
)

/**
 * Parsed URL deep-link parameters supported by Logic Labs Web:
 * - `?circuit=LOGIC-...` or `#circuit=LOGIC-...` -> opens the live breadboard with that shared circuit
 * - `?lab=lab7_half_adder` or `?lab=7` -> opens a specific guided coursework lab directly
 * - `?tab=projects|coursework|ics|settings` -> opens directly on a Home Hub tab
 * - `?theme=obsidian|cleanroom|amber|solarized|british|slate|titanium|cyberpunk|tokyo` -> applies theme immediately
 * - `?skipSplash=1` -> skips splash animation (also auto-skipped when any deep-link param is present)
 */
data class WebLaunchParams(
    val circuitCode: String? = null,
    val labQuery: String? = null,
    val initialTab: HomeTab? = null,
    val initialTheme: ThemeMode? = null,
    val skipSplash: Boolean = false
) {
    companion object {
        fun parse(queryString: String, hashString: String): WebLaunchParams {
            val params = LinkedHashMap<String, String>()
            fun collect(raw: String) {
                val cleaned = raw.trimStart('?', '#')
                if (cleaned.isEmpty()) return
                for (part in cleaned.split('&')) {
                    if (part.isEmpty()) continue
                    val eq = part.indexOf('=')
                    if (eq > 0) {
                        val k = part.substring(0, eq).lowercase()
                        val v = decodePercent(part.substring(eq + 1))
                        params[k] = v
                    }
                }
            }
            collect(queryString)
            collect(hashString)

            val circuit = params["circuit"] ?: params["code"]
            val lab = params["lab"]
            val tab = when (params["tab"]?.lowercase()) {
                "projects", "home" -> HomeTab.PROJECTS
                "coursework", "labs" -> HomeTab.COURSEWORK
                "ics", "reference", "74xx" -> HomeTab.REFERENCE
                "settings" -> HomeTab.SETTINGS
                else -> null
            }
            val theme = when (params["theme"]?.lowercase()) {
                "obsidian", "dark" -> ThemeMode.OBSIDIAN
                "cleanroom", "light" -> ThemeMode.CLEANROOM
                "amber", "amber_crt" -> ThemeMode.AMBER_CRT
                "slate", "hp_slate" -> ThemeMode.HP_SLATE
                "cyberpunk", "cyberpunk_neon" -> ThemeMode.CYBERPUNK_NEON
                "tokyo", "tokyo_night" -> ThemeMode.TOKYO_NIGHT
                "solarized", "solarized_dark" -> ThemeMode.SOLARIZED_DARK
                "british", "vintage", "vintage_british_lab" -> ThemeMode.VINTAGE_BRITISH_LAB
                "titanium", "titanium_frost" -> ThemeMode.TITANIUM_FROST
                else -> null
            }
            val skipSplash = params["skipsplash"] == "1" ||
                params["skipsplash"] == "true" ||
                !circuit.isNullOrBlank() ||
                !lab.isNullOrBlank() ||
                tab != null

            return WebLaunchParams(
                circuitCode = circuit?.takeIf { it.isNotBlank() },
                labQuery = lab?.takeIf { it.isNotBlank() },
                initialTab = tab,
                initialTheme = theme,
                skipSplash = skipSplash
            )
        }

        private fun decodePercent(s: String): String {
            val out = StringBuilder(s.length)
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '+') {
                    out.append(' ')
                    i++
                } else if (c == '%' && i + 2 < s.length) {
                    val hi = s[i + 1].digitToIntOrNull(16)
                    val lo = s[i + 2].digitToIntOrNull(16)
                    if (hi != null && lo != null) {
                        out.append(((hi shl 4) or lo).toChar())
                        i += 3
                    } else {
                        out.append(c)
                        i++
                    }
                } else {
                    out.append(c)
                    i++
                }
            }
            return out.toString()
        }
    }
}

private fun themeColorHexFor(theme: HardwareTheme): String = when (theme) {
    HardwareTheme.CLEANROOM -> "#EEF2F6"
    HardwareTheme.TITANIUM_FROST -> "#141A24"
    HardwareTheme.AMBER_CRT -> "#110D07"
    HardwareTheme.VINTAGE_BRITISH_LAB -> "#0B1612"
    HardwareTheme.SOLARIZED_DARK -> "#00212B"
    HardwareTheme.TOKYO_NIGHT -> "#16161E"
    HardwareTheme.CYBERPUNK_NEON -> "#0D0818"
    HardwareTheme.HP_SLATE -> "#11161D"
    HardwareTheme.OBSIDIAN -> "#090D14"
}

@Composable
fun LogicLabsWebRoot(launchParams: WebLaunchParams) {
    val container = remember { AppContainer() }
    val scope = rememberCoroutineScope()
    val controller = remember {
        DesktopShellController().apply {
            launchParams.initialTab?.let { homeTab = it }
            when {
                !launchParams.circuitCode.isNullOrBlank() -> {
                    destination = DesktopDestination.Bench(BenchRequest.ImportCode(launchParams.circuitCode))
                }
                !launchParams.labQuery.isNullOrBlank() -> {
                    val q = launchParams.labQuery.trim()
                    val num = q.toIntOrNull()
                    val lab = if (num != null) {
                        ExperimentCatalog.allLabs.firstOrNull { it.labNumber == num }
                    } else {
                        ExperimentCatalog.allLabs.firstOrNull {
                            it.id.equals(q, ignoreCase = true) ||
                                it.id.contains(q, ignoreCase = true)
                        }
                    }
                    destination = DesktopDestination.Bench(
                        if (lab != null) BenchRequest.Lab(lab.id) else BenchRequest.Continue
                    )
                }
                launchParams.skipSplash -> {
                    destination = DesktopDestination.Home
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        container.progress.ensureFirstLaunch()
        launchParams.initialTheme?.let { container.settings.setThemeMode(it) }
    }

    // Register drag-and-drop JSON import & browser Back button bridge
    DisposableEffect(container, controller) {
        webRegisterBrowserBridges(
            onDroppedJson = { _, raw ->
                val trimmed = raw.trim()
                if (trimmed.startsWith(CircuitShareBytecode.PREFIX)) {
                    Haptics.success()
                    controller.openBench(BenchRequest.ImportCode(trimmed))
                } else {
                    scope.launch {
                        val imported = container.projects.import(trimmed)
                        if (imported != null) {
                            Haptics.success()
                            controller.openBench(BenchRequest.Project(imported.id))
                        }
                    }
                }
            },
            onBrowserBack = {
                when {
                    controller.showControlsGuide -> {
                        controller.showControlsGuide = false
                        true
                    }
                    controller.destination is DesktopDestination.Bench -> {
                        val actions = controller.benchActions
                        if (actions != null) actions.requestLeaveBench()
                        else controller.openHome()
                        true
                    }
                    controller.destination is DesktopDestination.Settings -> {
                        controller.openHome()
                        true
                    }
                    else -> false
                }
            }
        )
        onDispose {}
    }

    val settings by container.settings.settings.collectAsState(initial = Settings())
    val systemDark = isSystemInDarkTheme()
    val hardwareTheme = remember(settings.themeMode, systemDark) {
        when (settings.themeMode) {
            ThemeMode.SYSTEM -> if (systemDark) HardwareTheme.OBSIDIAN else HardwareTheme.CLEANROOM
            ThemeMode.OBSIDIAN -> HardwareTheme.OBSIDIAN
            ThemeMode.AMBER_CRT -> HardwareTheme.AMBER_CRT
            ThemeMode.HP_SLATE -> HardwareTheme.HP_SLATE
            ThemeMode.CLEANROOM -> HardwareTheme.CLEANROOM
            ThemeMode.CYBERPUNK_NEON -> HardwareTheme.CYBERPUNK_NEON
            ThemeMode.TOKYO_NIGHT -> HardwareTheme.TOKYO_NIGHT
            ThemeMode.SOLARIZED_DARK -> HardwareTheme.SOLARIZED_DARK
            ThemeMode.VINTAGE_BRITISH_LAB -> HardwareTheme.VINTAGE_BRITISH_LAB
            ThemeMode.TITANIUM_FROST -> HardwareTheme.TITANIUM_FROST
        }
    }

    // Sync browser tab title, URL query state, and mobile status-bar theme-color
    LaunchedEffect(controller.destination, controller.homeTab, hardwareTheme) {
        val hex = themeColorHexFor(hardwareTheme)
        when (val dest = controller.destination) {
            DesktopDestination.Splash -> Unit
            DesktopDestination.Home -> {
                val tabParam = when (controller.homeTab) {
                    HomeTab.PROJECTS -> ""
                    HomeTab.COURSEWORK -> "?tab=coursework"
                    HomeTab.REFERENCE -> "?tab=ics"
                    HomeTab.SETTINGS -> "?tab=settings"
                }
                val tabTitle = when (controller.homeTab) {
                    HomeTab.PROJECTS -> "Logic Labs — IDL-800A Digital Logic Workstation"
                    HomeTab.COURSEWORK -> "49 Guided Labs — Logic Labs"
                    HomeTab.REFERENCE -> "74xx TTL IC Datasheets — Logic Labs"
                    HomeTab.SETTINGS -> "Settings — Logic Labs"
                }
                webSyncBrowserState(tabTitle, tabParam, pushHistory = false, themeColorHex = hex)
            }
            is DesktopDestination.Bench -> {
                when (val req = dest.request) {
                    is BenchRequest.Lab -> {
                        val lab = ExperimentCatalog.findById(req.labId)
                        val title = if (lab != null) "${lab.title} — Logic Labs" else "Workbench — Logic Labs"
                        webSyncBrowserState(title, "?lab=${req.labId}", pushHistory = true, themeColorHex = hex)
                    }
                    is BenchRequest.ImportCode -> {
                        webSyncBrowserState("Shared Circuit — Logic Labs", "?circuit=${req.code}", pushHistory = true, themeColorHex = hex)
                    }
                    else -> {
                        webSyncBrowserState("Breadboard Workbench — Logic Labs", "", pushHistory = true, themeColorHex = hex)
                    }
                }
            }
            DesktopDestination.Settings -> {
                webSyncBrowserState("Settings — Logic Labs", "?tab=settings", pushHistory = false, themeColorHex = hex)
            }
        }
    }

    val sfx = rememberConsoleSfx(enabled = settings.soundEnabled)
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

    LogicLabsTheme(hardwareTheme = hardwareTheme) {
        CompositionLocalProvider(
            LocalConsoleSfx provides sfx,
            com.logiclabs.core.designsystem.theme.LocalRenderEffects provides settings.bloomEffects
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CanvasBackground)
                    .onPreviewKeyEvent { ev ->
                        if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val ctrl = ev.isCtrlPressed || ev.isMetaPressed
                        when {
                            ev.key == Key.F1 -> {
                                controller.showControlsGuide = !controller.showControlsGuide
                                true
                            }
                            ev.key == Key.F11 -> {
                                com.logiclabs.app.platform.AppPlatform.toggleFullscreen()
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
                LogicLabsNavHost(container = container, controller = controller)
            }
        }
    }
}
