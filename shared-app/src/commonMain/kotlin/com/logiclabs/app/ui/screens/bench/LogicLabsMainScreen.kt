package com.logiclabs.app.ui.screens.bench

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.logiclabs.app.platform.AppPlatform
import androidx.compose.ui.text.font.FontWeight
import com.logiclabs.core.designsystem.component.desktopPointerHand
import com.logiclabs.core.designsystem.theme.PhosphorCore
import com.logiclabs.core.designsystem.theme.SurfaceRaised
import com.logiclabs.core.designsystem.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.logiclabs.app.di.AppContainer
import com.logiclabs.app.ui.component.BenchDialog
import com.logiclabs.app.ui.component.BenchDialogAction
import com.logiclabs.app.ui.component.BenchDialogActionDivider
import com.logiclabs.app.ui.component.BenchDialogTone
import com.logiclabs.app.ui.hud.CommandBar
import com.logiclabs.app.ui.hud.ContextRibbon
import com.logiclabs.app.ui.hud.ToolAction
import com.logiclabs.app.ui.hud.ToolRail
import com.logiclabs.app.ui.navigation.BenchMenuActions
import com.logiclabs.app.ui.navigation.BenchRequest
import com.logiclabs.app.ui.sheets.ChipCatalogSheet
import com.logiclabs.app.ui.sheets.IcPinoutInspectorSheet
import com.logiclabs.app.ui.sheets.LabCatalogSheet
import com.logiclabs.app.ui.sheets.ScopeSheet
import com.logiclabs.app.ui.state.BoardMode
import com.logiclabs.app.ui.state.Overlay
import com.logiclabs.app.ui.state.UndoRedoManager
import com.logiclabs.app.ui.state.rememberWorkbenchState
import com.logiclabs.app.ui.verify.LabRelevanceEvaluator
import com.logiclabs.app.ui.verify.LabRelevanceResult
import com.logiclabs.app.ui.verify.VerifierDialog
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.bridge.model.JumperWire
import com.logiclabs.core.bridge.model.SimulationMode
import com.logiclabs.core.bridge.model.WireColor
import com.logiclabs.core.bridge.topology.AD200Topology
import com.logiclabs.core.data.persistence.CircuitShareBytecode
import com.logiclabs.core.data.persistence.ProjectPersistence
import com.logiclabs.core.data.progress.Badge
import com.logiclabs.core.data.session.SessionKind
import com.logiclabs.core.data.settings.Settings
import com.logiclabs.core.designsystem.component.DesktopVerticalScrollbar
import com.logiclabs.core.designsystem.component.LogicIcons
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.AmberCore
import com.logiclabs.core.designsystem.theme.CanvasBackground
import com.logiclabs.core.designsystem.theme.ChassisBase
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.ShortCircuitAlert
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.SurfaceCardBorder
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextSecondary
import com.logiclabs.feature.breadboard.ui.BreadboardCanvas
import com.logiclabs.feature.breadboard.ui.BreadboardCanvasState
import com.logiclabs.feature.breadboard.ui.BreadboardContextMenuRequest
import com.logiclabs.feature.instruments.ui.TrainerConsoleDock
import com.logiclabs.feature.tools.courseware.ExperimentCatalog
import com.logiclabs.feature.tools.courseware.LabCurriculum
import com.logiclabs.feature.tools.courseware.LabExperiment
import com.logiclabs.feature.tools.diagram.BooleanDiagramView
import com.logiclabs.feature.tools.feedback.Haptics
import com.logiclabs.feature.tools.testbench.TestBenchReport
import com.logiclabs.feature.tools.testbench.TestBenchVerifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Desktop Workstation Workbench.
 *
 * Houses the resizable split-pane layout (AD-200 Breadboard / Live Boolean Schematic on the left,
 * collapsible K&H IDL-800A Trainer Console Rack on the right), multi-step Undo/Redo (`Ctrl+Z`/`Ctrl+Y`),
 * right-click context menus, double-click IC Pinout Inspector, full CAD & hardware keyboard shortcuts,
 * and native Windows JSON export / clipboard sharing.
 */
@Composable
fun LogicLabsMainScreen(
    benchRequest: BenchRequest,
    container: AppContainer,
    onNavigateHome: () -> Unit,
    onOpenControlsGuide: () -> Unit = {},
    onRegisterMenuActions: (BenchMenuActions?) -> Unit = {},
    externalModalOpen: Boolean = false
) {
    val bench = rememberWorkbenchState()
    val circuit = bench.circuit
    val canvasState = remember { BreadboardCanvasState() }
    val undoRedo = remember { UndoRedoManager(maxSteps = 60) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val rootFocusRequester = remember { FocusRequester() }
    val density = LocalDensity.current

    var activeLab by remember { mutableStateOf<LabExperiment?>(null) }
    var report by remember { mutableStateOf<TestBenchReport?>(null) }
    var newBadges by remember { mutableStateOf<List<Badge>>(emptyList()) }
    var candidateMatches by remember { mutableStateOf<List<LabRelevanceResult>>(emptyList()) }
    var relevanceJob by remember { mutableStateOf<Job?>(null) }

    var currentProjectId by remember { mutableStateOf<String?>(null) }
    var currentProjectTitle by remember { mutableStateOf<String?>(null) }
    var saveAsOpen by remember { mutableStateOf(false) }
    var shareDialogOpen by remember { mutableStateOf(false) }
    var confirmLeaveBench by remember { mutableStateOf(false) }
    var resetFitTrigger by remember { mutableStateOf(0) }

    var consoleCollapsed by remember { mutableStateOf(false) }
    var consoleWidthDp by remember { mutableStateOf(380.dp) }
    var mobileConsoleHeightDp by remember { mutableStateOf(272.dp) }
    var isFullscreen by remember { mutableStateOf(AppPlatform.isFullscreen()) }
    var contextMenuRequest by remember { mutableStateOf<BreadboardContextMenuRequest?>(null) }
    var inspectedChipPartNumber by remember { mutableStateOf<String?>(null) }

    val hasModalOpen = bench.overlay != Overlay.NONE ||
        saveAsOpen ||
        shareDialogOpen ||
        confirmLeaveBench ||
        inspectedChipPartNumber != null ||
        externalModalOpen

    LaunchedEffect(hasModalOpen) {
        if (!hasModalOpen) {
            delay(30)
            runCatching { rootFocusRequester.requestFocus() }
        }
    }

    fun showNotice(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    fun driveTopologyWithUndo(block: (BreadboardCircuit) -> Unit) {
        undoRedo.pushUndoSnapshot(circuit)
        bench.driveTopology(block)
    }

    fun performUndo() {
        if (undoRedo.undo(circuit)) {
            bench.clearSelection()
            bench.syncTopology()
            bench.syncFromCircuit()
            Haptics.tick()
            showNotice("Undid last change")
        }
    }

    fun performRedo() {
        if (undoRedo.redo(circuit)) {
            bench.clearSelection()
            bench.syncTopology()
            bench.syncFromCircuit()
            Haptics.tick()
            showNotice("Redid change")
        }
    }

    fun restoreWire(w: JumperWire) = driveTopologyWithUndo {
        it.addWire(w.startSocket, w.endSocket, w.color, w.isManhattan, w.id)
    }

    fun deleteSelection() {
        val wire = bench.selectedWire()
        if (wire != null) {
            driveTopologyWithUndo { it.removeWire(wire.id) }
            Haptics.tick()
            scope.launch {
                val res = snackbarHostState.showSnackbar(
                    message = "Jumper wire removed (Ctrl+Z to undo)",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                if (res == SnackbarResult.ActionPerformed) performUndo()
            }
            return
        }

        val chipId = bench.selectedChipId
        val chip = chipId?.let { id -> circuit.placedChips.find { it.placedIc.id == id } }
        if (chip != null) {
            val ic = chip.placedIc
            driveTopologyWithUndo { it.removeChip(ic.id) }
            Haptics.tick()
            scope.launch {
                val res = snackbarHostState.showSnackbar(
                    message = "IC ${ic.partNumber} removed (Ctrl+Z to undo)",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                if (res == SnackbarResult.ActionPerformed) performUndo()
            }
            return
        }

        bench.overlay = Overlay.BOARD_ACTIONS
    }

    fun nudgeSelectedChip(deltaCol: Int) {
        val id = bench.selectedChipId ?: return
        val chip = circuit.placedChips.find { it.placedIc.id == id }?.placedIc ?: return
        val target = chip.startColumn + deltaCol
        if (target in 0..55) {
            driveTopologyWithUndo { it.moveChip(id, chip.trench, target) }
            Haptics.tick()
        }
    }

    fun rotateSelectedChip() {
        val id = bench.selectedChipId
        if (id != null) {
            driveTopologyWithUndo { it.rotateChip(id) }
            Haptics.tick()
        } else {
            showNotice("Select an IC first to rotate it")
        }
    }

    fun exportCurrentCircuitJson() {
        scope.launch {
            val title = currentProjectTitle ?: defaultProjectTitle(activeLab)
            val safeName = title.replace(Regex("[^A-Za-z0-9 _-]"), "").ifBlank { "circuit" }
            val jsonText = ProjectPersistence.toJson(
                ProjectPersistence.serialize(circuit, title, activeLab?.id)
                    .copy(id = currentProjectId ?: "")
            )
            AppPlatform.exportJsonText("$safeName.logiclabs.json", jsonText) { ok, message ->
                if (ok) Haptics.success()
                showNotice(message)
            }
        }
    }

    fun exportCurrentCircuitPng() {
        scope.launch {
            val title = currentProjectTitle ?: defaultProjectTitle(activeLab)
            val safeName = title.replace(Regex("[^A-Za-z0-9 _-]"), "").ifBlank { "circuit" }
            val bytes = com.logiclabs.feature.breadboard.canvas.BreadboardSnapshotExporter.renderPngBytes(circuit)
            if (bytes == null) {
                showNotice("Could not render PNG snapshot")
                return@launch
            }
            AppPlatform.exportPngSnapshot("$safeName-breadboard.png", bytes) { ok, message ->
                if (ok) Haptics.success()
                showNotice(message)
            }
        }
    }

    fun runVerification() {
        val connectedSwitches = (0..7).filter { circuit.dsu.getNetSize(AD200Topology.TERM_SW0 + it) > 1 }
        val connectedLeds = (0..7).filter { circuit.dsu.getNetSize(AD200Topology.TERM_LED0 + it) > 2 }

        val lab = activeLab ?: run {
            val placedChips = circuit.placedChips.map { it.placedIc.partNumber }
            findBestMatchingLab(placedChips, connectedSwitches.size, connectedLeds.size, circuit, connectedSwitches, connectedLeds)
        }

        val fallbackSwitches = if (connectedSwitches.isEmpty()) {
            lab.switchIndices
        } else if (connectedSwitches.size >= lab.switchIndices.size) {
            if (lab.switchIndices.all { it in connectedSwitches }) lab.switchIndices else connectedSwitches.take(lab.switchIndices.size)
        } else {
            val padded = connectedSwitches.toMutableList()
            for (sw in lab.switchIndices) {
                if (padded.size >= lab.switchIndices.size) break
                if (sw !in padded) padded.add(sw)
            }
            var candidate = 0
            while (padded.size < lab.switchIndices.size && candidate < 8) {
                if (candidate !in padded) padded.add(candidate)
                candidate++
            }
            padded
        }

        val fallbackLeds = if (connectedLeds.isEmpty()) {
            lab.ledIndices
        } else if (connectedLeds.size >= lab.ledIndices.size) {
            if (lab.ledIndices.all { it in connectedLeds }) lab.ledIndices else connectedLeds.take(lab.ledIndices.size)
        } else {
            val padded = connectedLeds.toMutableList()
            for (led in lab.ledIndices) {
                if (padded.size >= lab.ledIndices.size) break
                if (led !in padded) padded.add(led)
            }
            var candidate = 0
            while (padded.size < lab.ledIndices.size && candidate < 8) {
                if (candidate !in padded) padded.add(candidate)
                candidate++
            }
            padded
        }

        val (effectiveSwitches, effectiveLeds, _) = discoverBestIoMapping(
            circuit = circuit,
            connectedSwitches = connectedSwitches,
            connectedLeds = connectedLeds,
            requiredSwitchCount = lab.switchIndices.size,
            requiredLedCount = lab.ledIndices.size,
            expectedFunction = lab.expectedFunction,
            fallbackSwitches = fallbackSwitches,
            fallbackLeds = fallbackLeds
        )

        val dynamicInputLabels = lab.inputLabels.mapIndexed { idx, label ->
            val expectedSw = lab.switchIndices.getOrNull(idx) ?: idx
            val actualSw = effectiveSwitches.getOrNull(idx) ?: expectedSw
            if (label.contains("SW$expectedSw")) {
                label.replace("SW$expectedSw", "SW$actualSw")
            } else if (label.contains("SW")) {
                label
            } else {
                "$label (SW$actualSw)"
            }
        }

        val dynamicOutputLabels = lab.outputLabels.mapIndexed { idx, label ->
            val expectedLed = lab.ledIndices.getOrNull(idx) ?: idx
            val actualLed = effectiveLeds.getOrNull(idx) ?: expectedLed
            if (label.contains("LED$expectedLed")) {
                label.replace("LED$expectedLed", "LED$actualLed")
            } else if (label.contains("LED")) {
                label
            } else {
                "$label (LED$actualLed)"
            }
        }

        val sealed = TestBenchVerifier.verify(
            circuit = circuit,
            switchIndices = effectiveSwitches,
            outputReader = { effectiveLeds.map { circuit.ledValues[it] } },
            expectedFunction = lab.expectedFunction,
            inputNames = dynamicInputLabels,
            outputNames = dynamicOutputLabels,
            experimentTitle = lab.title,
            ledIndices = effectiveLeds
        )
        bench.syncFromCircuit()
        report = sealed
        bench.lastVerificationPassed = sealed.isAllPassed
        bench.overlay = Overlay.VERIFIER
        if (sealed.isAllPassed) {
            scope.launch {
                newBadges = container.progress.markLabVerified(lab.id, LabCurriculum.classicLabs.size)
            }
        } else {
            newBadges = emptyList()
        }

        relevanceJob?.cancel()
        val evalSnapshot = circuit.snapshot()
        relevanceJob = scope.launch(Dispatchers.Default) {
            val matches = LabRelevanceEvaluator.evaluateRelevance(
                circuit = evalSnapshot,
                candidateLabs = ExperimentCatalog.allLabs,
                isCancelled = { !isActive }
            )
            if (!isActive) return@launch
            withContext(Dispatchers.Main) {
                candidateMatches = matches
            }
        }
    }

    fun loadLab(lab: LabExperiment) {
        relevanceJob?.cancel()
        undoRedo.clear()
        activeLab = lab
        currentProjectId = null
        currentProjectTitle = null
        bench.clearSelection()
        report = null
        candidateMatches = emptyList()
        bench.lastVerificationPassed = null
        bench.driveTopology {
            it.clearAll()
            lab.buildCircuit(it)
        }
        resetFitTrigger++
        Haptics.thud()
        bench.overlay = Overlay.NONE
        container.session.recordLab(lab.id)
    }

    fun loadSandbox() {
        undoRedo.clear()
        activeLab = null
        currentProjectId = null
        currentProjectTitle = null
        bench.clearSelection()
        report = null
        bench.lastVerificationPassed = null
        bench.driveTopology {
            it.clearAll()
            it.masterPower = true
        }
        resetFitTrigger++
        Haptics.thud()
        bench.overlay = Overlay.NONE
        container.session.recordSandbox()
    }

    suspend fun loadProject(projectId: String) {
        val project = container.projects.load(projectId)
        if (project == null) {
            loadLab(LabCurriculum.classicLabs[0])
            return
        }
        undoRedo.clear()
        ProjectPersistence.deserialize(project, circuit)
        activeLab = project.activeLabId?.let { id ->
            ExperimentCatalog.allLabs.find { it.id == id }
        }
        currentProjectId = project.id
        currentProjectTitle = project.title
        bench.clearSelection()
        report = null
        bench.lastVerificationPassed = null
        bench.syncTopology()
        bench.syncFromCircuit()
        resetFitTrigger++
        Haptics.thud()
        bench.overlay = Overlay.NONE
        container.session.recordProject(projectId)
    }

    fun performSave(title: String) {
        scope.launch {
            val base = ProjectPersistence.serialize(circuit, title, activeLab?.id)
            val withId = currentProjectId?.let { base.copy(id = it) } ?: base
            val stored = container.projects.save(withId)
            currentProjectId = stored.project.id
            currentProjectTitle = stored.project.title
            container.session.recordProject(stored.project.id)
            if (stored.isNew) {
                val earned = container.progress.incrementCircuitsBuilt()
                Haptics.success()
                snackbarHostState.showSnackbar(
                    message = if (earned.isEmpty()) {
                        "Project saved: ${stored.project.title}"
                    } else {
                        "Project saved · Badge earned: ${earned.first().title}"
                    },
                    duration = SnackbarDuration.Short
                )
            } else {
                Haptics.tick()
                snackbarHostState.showSnackbar(
                    message = "Project saved: ${stored.project.title}",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    fun triggerQuickSave() {
        val title = currentProjectTitle
        if (title != null) performSave(title) else saveAsOpen = true
    }

    // Register live callbacks for the top Windows MenuBar
    DisposableEffect(bench, undoRedo, canvasState) {
        onRegisterMenuActions(
            BenchMenuActions(
                canUndo = { undoRedo.canUndo },
                canRedo = { undoRedo.canRedo },
                undo = { performUndo() },
                redo = { performRedo() },
                save = { triggerQuickSave() },
                saveAs = { saveAsOpen = true },
                exportJson = { exportCurrentCircuitJson() },
                shareCode = { shareDialogOpen = true },
                openChipCatalog = { bench.overlay = Overlay.CHIP_CATALOG },
                openLabCatalog = { bench.overlay = Overlay.LAB_CATALOG },
                openScope = { bench.overlay = Overlay.SCOPE },
                toggleDiagram = { bench.viewingDiagram = !bench.viewingDiagram },
                isViewingDiagram = { bench.viewingDiagram },
                runVerify = { runVerification() },
                togglePower = {
                    bench.driveMasterPower(!bench.masterPower)
                    Haptics.thud()
                },
                toggleClock = {
                    bench.driveClockRunning(!bench.clockRunning)
                    Haptics.tick()
                },
                stepClock = {
                    bench.tickClock()
                    Haptics.tick()
                },
                toggleSimulationMode = { bench.toggleSimulationMode() },
                toggleConsole = { consoleCollapsed = !consoleCollapsed },
                isConsoleCollapsed = { consoleCollapsed },
                toggleLoupe = { canvasState.loupeEnabled.value = !canvasState.loupeEnabled.value },
                isLoupeEnabled = { canvasState.loupeEnabled.value },
                fitView = {
                    bench.clearSelection()
                    resetFitTrigger++
                },
                zoomIn = { canvasState.nudgeZoom(1.25f) },
                zoomOut = { canvasState.nudgeZoom(0.8f) },
                clearWires = { driveTopologyWithUndo { it.clearWires() } },
                resetBoard = {
                    relevanceJob?.cancel()
                    driveTopologyWithUndo { it.clearAll() }
                    activeLab = null
                    report = null
                    candidateMatches = emptyList()
                    bench.lastVerificationPassed = null
                },
                requestLeaveBench = { confirmLeaveBench = true }
            )
        )
        onDispose {
            onRegisterMenuActions(null)
        }
    }

    // Resolve opening request once per composition
    LaunchedEffect(benchRequest) {
        when (val request = benchRequest) {
            BenchRequest.Continue -> {
                val session = container.session.current()
                when (session?.kind) {
                    SessionKind.LAB -> {
                        val lab = ExperimentCatalog.allLabs.find { it.id == session.id }
                        if (lab != null) loadLab(lab) else loadLab(LabCurriculum.classicLabs[0])
                    }
                    SessionKind.PROJECT -> {
                        val id = session.id
                        if (id != null) loadProject(id) else loadLab(LabCurriculum.classicLabs[0])
                    }
                    SessionKind.SANDBOX -> {
                        val draft = container.projects.loadDraft()
                        if (draft != null && (draft.chips.isNotEmpty() || draft.wires.isNotEmpty())) {
                            undoRedo.clear()
                            ProjectPersistence.deserialize(draft, circuit)
                            activeLab = draft.activeLabId?.let { id ->
                                ExperimentCatalog.allLabs.find { it.id == id }
                            }
                            bench.clearSelection()
                            report = null
                            bench.lastVerificationPassed = null
                            bench.syncTopology()
                            bench.syncFromCircuit()
                            resetFitTrigger++
                            container.session.recordSandbox()
                        } else {
                            loadSandbox()
                        }
                    }
                    null -> loadLab(LabCurriculum.classicLabs[0])
                }
            }
            is BenchRequest.Lab -> {
                val lab = ExperimentCatalog.allLabs.find { it.id == request.labId }
                if (lab != null) loadLab(lab) else loadLab(LabCurriculum.classicLabs[0])
            }
            is BenchRequest.Project -> loadProject(request.projectId)
            is BenchRequest.ImportCode -> {
                val decoded = runCatching { CircuitShareBytecode.decodeToProject(request.code) }.getOrNull()
                if (decoded != null) {
                    undoRedo.clear()
                    ProjectPersistence.deserialize(decoded, circuit)
                    activeLab = null
                    currentProjectId = null
                    currentProjectTitle = "Shared Circuit"
                    bench.clearSelection()
                    report = null
                    bench.lastVerificationPassed = null
                    bench.syncTopology()
                    bench.syncFromCircuit()
                    resetFitTrigger++
                    Haptics.success()
                    showNotice("Loaded shared circuit (${decoded.chips.size} ICs, ${decoded.wires.size} wires)")
                } else {
                    loadLab(LabCurriculum.classicLabs[0])
                    showNotice("Could not decode shared circuit link — loaded default lab")
                }
            }
            BenchRequest.Sandbox -> {
                container.projects.clearDraft()
                loadSandbox()
            }
        }
    }

    // Accrue bench time while open
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            container.progress.addBenchTime(60_000)
        }
    }

    val settings by container.settings.settings.collectAsState(initial = Settings())
    LaunchedEffect(settings.defaultWireColor) {
        runCatching { WireColor.valueOf(settings.defaultWireColor) }
            .getOrNull()
            ?.let { bench.activeWireColor = it }
    }
    LaunchedEffect(settings.ledAfterglow) {
        bench.afterglowEnabled = settings.ledAfterglow
        bench.syncFromCircuit()
    }

    // Debounced recovery draft autosave
    LaunchedEffect(bench.topologyVersion, settings.autosaveEnabled) {
        if (!settings.autosaveEnabled || bench.topologyVersion <= 1) return@LaunchedEffect
        delay(4_000)
        container.projects.saveDraftAsync(
            ProjectPersistence.serialize(
                circuit,
                currentProjectTitle ?: defaultProjectTitle(activeLab),
                activeLab?.id
            ).copy(id = currentProjectId ?: "")
        )
    }

    val breadboardPane: @Composable (Modifier) -> Unit = { paneModifier ->
        Column(modifier = paneModifier) {
            CommandBar(
                chipCount = bench.chipCount,
                wireCount = bench.wireCount,
                masterPower = bench.masterPower,
                verificationPassed = bench.lastVerificationPassed,
                onVerifyClick = { runVerification() },
                projectTitle = currentProjectTitle,
                onSaveClick = { triggerQuickSave() },
                onShareClick = { shareDialogOpen = true },
                onBackClick = { confirmLeaveBench = true },
                simulationMode = bench.simulationMode,
                onToggleSimulationMode = { bench.toggleSimulationMode() },
                hasBurnedChips = bench.hasBurnedChips,
                onRestoreChips = { bench.restoreBurnedChips() },
                canUndo = undoRedo.canUndo,
                canRedo = undoRedo.canRedo,
                onUndoClick = { performUndo() },
                onRedoClick = { performRedo() },
                onControlsGuideClick = onOpenControlsGuide,
                consoleCollapsed = consoleCollapsed,
                onToggleConsoleClick = { consoleCollapsed = !consoleCollapsed },
                isFullscreen = isFullscreen,
                onToggleFullscreenClick = if (AppPlatform.supportsFullscreen) {
                    {
                        Haptics.tick()
                        isFullscreen = AppPlatform.toggleFullscreen()
                    }
                } else null
            )

            ToolRail(
                boardMode = bench.boardMode,
                onAction = { action ->
                    Haptics.tick()
                    when (action) {
                        ToolAction.CHIPS -> bench.overlay = Overlay.CHIP_CATALOG
                        ToolAction.SCOPE -> bench.overlay = Overlay.SCOPE
                        ToolAction.LABS -> bench.overlay = Overlay.LAB_CATALOG
                        ToolAction.ROAM -> {
                            bench.boardMode = BoardMode.ROAM
                            bench.clearSelection()
                        }
                        ToolAction.FIT_VIEW -> {
                            bench.clearSelection()
                            resetFitTrigger++
                        }
                        ToolAction.ROTATE -> rotateSelectedChip()
                        ToolAction.CLEAR -> deleteSelection()
                        ToolAction.DIAGRAM -> bench.viewingDiagram = !bench.viewingDiagram
                    }
                },
                onModeChange = { mode ->
                    Haptics.tick()
                    bench.boardMode = mode
                    if (mode != BoardMode.WIRE_SELECT) bench.clearSelection()
                },
                deleteLabel = when {
                    bench.selectedWireId != null -> "DEL WIRE"
                    bench.selectedChipId != null -> "DEL IC"
                    else -> "CLEAR"
                },
                simulationModeLabel = if (bench.simulationMode == SimulationMode.PRACTICAL) "PRACTICAL" else "IDEAL",
                chipCount = bench.chipCount,
                wireCount = bench.wireCount,
                zoomPercent = canvasState.zoomPercent(),
                diagramActive = bench.viewingDiagram
            )

            ContextRibbon(
                selectedWire = bench.selectedWire(),
                selectedChipLabel = bench.selectedChipId?.let { id ->
                    circuit.placedChips.find { it.placedIc.id == id }?.placedIc?.partNumber
                },
                activeWireColor = bench.activeWireColor,
                wireModeActive = bench.boardMode == BoardMode.WIRE_DRAW,
                onWireColorPicked = { color ->
                    recolorSelection(bench, undoRedo, color)
                },
                onFlipWire = { flipSelectedWire(bench, undoRedo) },
                onToggleRouting = { toggleSelectedRouting(bench, undoRedo) },
                onDeleteSelection = { deleteSelection() },
                onClearSelection = { bench.clearSelection() },
                onNudgeChipLeft = { nudgeSelectedChip(-1) },
                onNudgeChipRight = { nudgeSelectedChip(1) },
                onRotateChip = { rotateSelectedChip() },
                modifier = Modifier.padding(vertical = Dimens.Space1)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (bench.viewingDiagram) {
                    BooleanDiagramView(
                        circuit = circuit,
                        modifier = Modifier.fillMaxSize(),
                        circuitVersion = bench.topologyVersion,
                        renderVersion = bench.renderVersion,
                        zoomCaptureEnabled = !hasModalOpen
                    )
                } else {
                    BreadboardCanvas(
                        circuit = circuit,
                        modifier = Modifier.fillMaxSize(),
                        state = canvasState,
                        activeWireColor = bench.activeWireColor,
                        defaultWireManhattan = settings.defaultRoutingManhattan,
                        isWireModeActive = bench.boardMode == BoardMode.WIRE_DRAW,
                        isWireSelectMode = bench.boardMode == BoardMode.WIRE_SELECT,
                        selectedWireId = bench.selectedWireId,
                        selectedChipId = bench.selectedChipId,
                        circuitVersion = bench.topologyVersion,
                        renderVersion = bench.renderVersion,
                        resetFitTrigger = resetFitTrigger,
                        zoomCaptureEnabled = !hasModalOpen,
                        onWireSelected = { id -> bench.selectWire(id) },
                        onChipSelected = { id -> bench.selectChip(id) },
                        onChipDoubleClicked = { chipId ->
                            val chip = circuit.placedChips.find { it.placedIc.id == chipId }
                            if (chip != null) {
                                inspectedChipPartNumber = chip.placedIc.partNumber
                                Haptics.tick()
                            }
                        },
                        onContextMenu = { req ->
                            if (req.wireId != null) bench.selectWire(req.wireId)
                            else if (req.chipId != null) bench.selectChip(req.chipId)
                            contextMenuRequest = req
                        },
                        onBeforeTopologyMutation = { undoRedo.pushUndoSnapshot(circuit) },
                        onDeleteEquipment = { deleteSelection() },
                        onToggleWireMode = {
                            bench.boardMode =
                                if (bench.boardMode == BoardMode.WIRE_DRAW) BoardMode.ROAM
                                else BoardMode.WIRE_DRAW
                        },
                        onSocketSelected = { Haptics.tick() },
                        onCircuitChanged = {
                            bench.syncTopology()
                            bench.syncFromCircuit()
                        }
                    )

                    // Right-click desktop context menu
                    val menuReq = contextMenuRequest
                    if (menuReq != null) {
                        val dpOffset = with(density) {
                            DpOffset(menuReq.screenOffset.x.toDp(), menuReq.screenOffset.y.toDp())
                        }
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { contextMenuRequest = null },
                            offset = dpOffset,
                            containerColor = SurfaceCard
                        ) {
                            when {
                                menuReq.wireId != null -> {
                                    DropdownMenuItem(
                                        text = { Text("Flip Wire Endpoints", style = LogicLabsType.BodySm, color = TextPrimary) },
                                        onClick = {
                                            contextMenuRequest = null
                                            flipSelectedWire(bench, undoRedo)
                                            Haptics.tick()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Toggle Manhattan / Curved [M]", style = LogicLabsType.BodySm, color = TextPrimary) },
                                        onClick = {
                                            contextMenuRequest = null
                                            toggleSelectedRouting(bench, undoRedo)
                                            Haptics.tick()
                                        }
                                    )
                                    HorizontalDivider(color = SurfaceCardBorder)
                                    listOf(
                                        WireColor.RED to "Red",
                                        WireColor.BLACK to "Black",
                                        WireColor.YELLOW to "Yellow",
                                        WireColor.GREEN to "Green",
                                        WireColor.BLUE to "Blue",
                                        WireColor.WHITE to "White"
                                    ).forEach { (col, name) ->
                                        DropdownMenuItem(
                                            text = { Text("Color: $name", style = LogicLabsType.BodySm, color = TextSecondary) },
                                            onClick = {
                                                contextMenuRequest = null
                                                recolorSelection(bench, undoRedo, col)
                                                Haptics.tick()
                                            }
                                        )
                                    }
                                    HorizontalDivider(color = SurfaceCardBorder)
                                    DropdownMenuItem(
                                        text = { Text("Delete Wire [Del]", style = LogicLabsType.BodySm, color = ShortCircuitAlert) },
                                        onClick = {
                                            contextMenuRequest = null
                                            deleteSelection()
                                        }
                                    )
                                }

                                menuReq.chipId != null -> {
                                    val chip = circuit.placedChips.find { it.placedIc.id == menuReq.chipId }
                                    if (chip != null) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Inspect ${chip.placedIc.partNumber} Pinout",
                                                    style = LogicLabsType.BodySm,
                                                    color = AccentCyan
                                                )
                                            },
                                            onClick = {
                                                contextMenuRequest = null
                                                inspectedChipPartNumber = chip.placedIc.partNumber
                                                Haptics.tick()
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Rotate IC 180° [R]", style = LogicLabsType.BodySm, color = TextPrimary) },
                                        onClick = {
                                            contextMenuRequest = null
                                            rotateSelectedChip()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Nudge Left [←]", style = LogicLabsType.BodySm, color = TextPrimary) },
                                        onClick = {
                                            contextMenuRequest = null
                                            nudgeSelectedChip(-1)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Nudge Right [→]", style = LogicLabsType.BodySm, color = TextPrimary) },
                                        onClick = {
                                            contextMenuRequest = null
                                            nudgeSelectedChip(1)
                                        }
                                    )
                                    HorizontalDivider(color = SurfaceCardBorder)
                                    DropdownMenuItem(
                                        text = { Text("Delete IC [Del]", style = LogicLabsType.BodySm, color = ShortCircuitAlert) },
                                        onClick = {
                                            contextMenuRequest = null
                                            deleteSelection()
                                        }
                                    )
                                }

                                else -> {
                                    DropdownMenuItem(
                                        text = { Text("Place 74xx IC... [C]", style = LogicLabsType.BodySm, color = TextPrimary) },
                                        onClick = {
                                            contextMenuRequest = null
                                            bench.overlay = Overlay.CHIP_CATALOG
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            val label = if (bench.boardMode == BoardMode.WIRE_DRAW) "Exit Wire Mode [W]" else "Wire Draw Mode [W]"
                                            Text(label, style = LogicLabsType.BodySm, color = TextPrimary)
                                        },
                                        onClick = {
                                            contextMenuRequest = null
                                            bench.boardMode = if (bench.boardMode == BoardMode.WIRE_DRAW) BoardMode.ROAM else BoardMode.WIRE_DRAW
                                            Haptics.tick()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Fit View to Circuit [F]", style = LogicLabsType.BodySm, color = TextPrimary) },
                                        onClick = {
                                            contextMenuRequest = null
                                            resetFitTrigger++
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            val label = if (canvasState.loupeEnabled.value) "Hide 2.2x Loupe [L]" else "Show 2.2x Loupe [L]"
                                            Text(label, style = LogicLabsType.BodySm, color = TextPrimary)
                                        },
                                        onClick = {
                                            contextMenuRequest = null
                                            canvasState.loupeEnabled.value = !canvasState.loupeEnabled.value
                                        }
                                    )
                                    HorizontalDivider(color = SurfaceCardBorder)
                                    DropdownMenuItem(
                                        text = { Text("Clear All Wires", style = LogicLabsType.BodySm, color = ShortCircuitAlert) },
                                        onClick = {
                                            contextMenuRequest = null
                                            driveTopologyWithUndo { it.clearWires() }
                                            Haptics.tick()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.key == Key.F1 && ev.type == KeyEventType.KeyDown) {
                    onOpenControlsGuide()
                    return@onPreviewKeyEvent true
                }
                if (ev.key == Key.F11 && ev.type == KeyEventType.KeyDown && AppPlatform.supportsFullscreen) {
                    Haptics.tick()
                    isFullscreen = AppPlatform.toggleFullscreen()
                    return@onPreviewKeyEvent true
                }
                if (hasModalOpen) return@onPreviewKeyEvent false

                val ctrl = ev.isCtrlPressed || ev.isMetaPressed
                val shift = ev.isShiftPressed

                // Hold-and-release keys: Space (pan), Q (Pulser A), E (Pulser B)
                if (!ctrl) {
                    when (ev.key) {
                        Key.Spacebar -> {
                            canvasState.spacePanActive.value = (ev.type == KeyEventType.KeyDown)
                            return@onPreviewKeyEvent true
                        }
                        Key.Q -> {
                            val down = ev.type == KeyEventType.KeyDown
                            if (bench.pulserA != down) {
                                bench.drivePulserA(down)
                                if (down) Haptics.tick()
                            }
                            return@onPreviewKeyEvent true
                        }
                        Key.E -> {
                            val down = ev.type == KeyEventType.KeyDown
                            if (bench.pulserB != down) {
                                bench.drivePulserB(down)
                                if (down) Haptics.tick()
                            }
                            return@onPreviewKeyEvent true
                        }
                        else -> Unit
                    }
                }

                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                if (ctrl) {
                    when (ev.key) {
                        Key.Z -> {
                            if (shift) performRedo() else performUndo()
                            return@onPreviewKeyEvent true
                        }
                        Key.Y -> {
                            performRedo()
                            return@onPreviewKeyEvent true
                        }
                        Key.S -> {
                            if (shift) saveAsOpen = true else triggerQuickSave()
                            return@onPreviewKeyEvent true
                        }
                        Key.E -> {
                            exportCurrentCircuitJson()
                            return@onPreviewKeyEvent true
                        }
                        Key.P -> {
                            if (shift) {
                                exportCurrentCircuitPng()
                                return@onPreviewKeyEvent true
                            }
                            return@onPreviewKeyEvent false
                        }
                        Key.Equals, Key.Plus, Key.NumPadAdd -> {
                            canvasState.nudgeZoom(1.22f)
                            return@onPreviewKeyEvent true
                        }
                        Key.Minus, Key.NumPadSubtract -> {
                            canvasState.nudgeZoom(1f / 1.22f)
                            return@onPreviewKeyEvent true
                        }
                        Key.Zero, Key.NumPad0 -> {
                            bench.clearSelection()
                            resetFitTrigger++
                            return@onPreviewKeyEvent true
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                }

                when (ev.key) {
                    // Switches SW0..SW7 mapped to keys 1..8 (and NumPad 1..8)
                    Key.One, Key.NumPad1 -> { bench.toggleSwitch(0); Haptics.tick(); true }
                    Key.Two, Key.NumPad2 -> { bench.toggleSwitch(1); Haptics.tick(); true }
                    Key.Three, Key.NumPad3 -> { bench.toggleSwitch(2); Haptics.tick(); true }
                    Key.Four, Key.NumPad4 -> { bench.toggleSwitch(3); Haptics.tick(); true }
                    Key.Five, Key.NumPad5 -> { bench.toggleSwitch(4); Haptics.tick(); true }
                    Key.Six, Key.NumPad6 -> { bench.toggleSwitch(5); Haptics.tick(); true }
                    Key.Seven, Key.NumPad7 -> { bench.toggleSwitch(6); Haptics.tick(); true }
                    Key.Eight, Key.NumPad8 -> { bench.toggleSwitch(7); Haptics.tick(); true }

                    // Console power & clock
                    Key.P -> { bench.driveMasterPower(!bench.masterPower); Haptics.thud(); true }
                    Key.K -> { bench.driveClockRunning(!bench.clockRunning); Haptics.tick(); true }
                    Key.Period -> { bench.tickClock(); Haptics.tick(); true }

                    // Breadboard CAD modes & edits
                    Key.W -> {
                        bench.boardMode = if (bench.boardMode == BoardMode.WIRE_DRAW) BoardMode.ROAM else BoardMode.WIRE_DRAW
                        Haptics.tick()
                        true
                    }
                    Key.V -> {
                        bench.boardMode = BoardMode.ROAM
                        bench.clearSelection()
                        Haptics.tick()
                        true
                    }
                    Key.S -> {
                        bench.boardMode = BoardMode.WIRE_SELECT
                        Haptics.tick()
                        true
                    }
                    Key.R -> { rotateSelectedChip(); true }
                    Key.M -> {
                        if (bench.selectedWire() != null) {
                            toggleSelectedRouting(bench, undoRedo)
                            Haptics.tick()
                        }
                        true
                    }
                    Key.Delete, Key.Backspace -> {
                        if (bench.selectedWireId != null || bench.selectedChipId != null) {
                            deleteSelection()
                            true
                        } else false
                    }
                    Key.DirectionLeft -> {
                        if (bench.selectedChipId != null) nudgeSelectedChip(-1)
                        else canvasState.panBy(Offset(48f, 0f))
                        true
                    }
                    Key.DirectionRight -> {
                        if (bench.selectedChipId != null) nudgeSelectedChip(1)
                        else canvasState.panBy(Offset(-48f, 0f))
                        true
                    }
                    Key.DirectionUp -> {
                        canvasState.panBy(Offset(0f, 48f))
                        true
                    }
                    Key.DirectionDown -> {
                        canvasState.panBy(Offset(0f, -48f))
                        true
                    }

                    // Viewport zoom, fit, loupe
                    Key.F, Key.Zero, Key.NumPad0 -> {
                        bench.clearSelection()
                        resetFitTrigger++
                        true
                    }
                    Key.Equals, Key.Plus, Key.NumPadAdd -> {
                        canvasState.nudgeZoom(1.22f)
                        true
                    }
                    Key.Minus, Key.NumPadSubtract -> {
                        canvasState.nudgeZoom(1f / 1.22f)
                        true
                    }
                    Key.L -> {
                        canvasState.loupeEnabled.value = !canvasState.loupeEnabled.value
                        if (!canvasState.loupeEnabled.value) canvasState.hideLoupe()
                        Haptics.tick()
                        true
                    }

                    // Tools & overlays
                    Key.C -> { bench.overlay = Overlay.CHIP_CATALOG; Haptics.tick(); true }
                    Key.B -> { bench.overlay = Overlay.LAB_CATALOG; Haptics.tick(); true }
                    Key.O -> { bench.overlay = Overlay.SCOPE; Haptics.tick(); true }
                    Key.D -> { bench.viewingDiagram = !bench.viewingDiagram; Haptics.tick(); true }
                    Key.F5 -> { runVerification(); true }
                    Key.Slash -> {
                        if (shift) {
                            onOpenControlsGuide()
                            true
                        } else false
                    }
                    Key.Escape -> {
                        when {
                            contextMenuRequest != null -> contextMenuRequest = null
                            canvasState.draftAnchor.value != null -> canvasState.clearDraft()
                            bench.selectedWireId != null || bench.selectedChipId != null -> bench.clearSelection()
                            bench.boardMode != BoardMode.ROAM -> bench.boardMode = BoardMode.ROAM
                            else -> confirmLeaveBench = true
                        }
                        true
                    }
                    else -> false
                }
            }
    ) {
        Scaffold(
            containerColor = CanvasBackground,
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            }
        ) { _ ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CanvasBackground)
            ) {
                val isCompactMobile = maxWidth < 840.dp
                val screenMaxHeight = maxHeight
                if (!isCompactMobile) {
                    val maxConsoleWidth = (maxWidth * 0.48f).coerceAtLeast(320.dp).coerceAtMost(540.dp)
                    val effectiveConsoleWidth = consoleWidthDp.coerceIn(310.dp, maxConsoleWidth)

                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Pane: AD-200 Breadboard or Live Boolean Schematic
                        breadboardPane(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        if (!consoleCollapsed) {
                            // Draggable vertical splitter bar — crisp dividers on dark & Cleanroom themes
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .fillMaxHeight()
                                    .background(ChassisBase)
                                    .pointerHoverIcon(AppPlatform.resizeHorizontalPointerIcon)
                                    .pointerInput(maxConsoleWidth) {
                                        detectHorizontalDragGestures { change, dragAmount ->
                                            change.consume()
                                            val deltaDp = with(density) { dragAmount.toDp() }
                                            consoleWidthDp = (consoleWidthDp - deltaDp).coerceIn(310.dp, maxConsoleWidth)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(SurfaceCardBorder)
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(SurfaceCardBorder)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(TextSecondary.copy(alpha = 0.55f))
                                )
                            }

                            // Right Pane: Resizable K&H IDL-800A Trainer Console Rack
                            Box(
                                modifier = Modifier
                                    .width(effectiveConsoleWidth)
                                    .fillMaxHeight()
                            ) {
                                TrainerConsoleDock(
                                    circuit = circuit,
                                    circuitVersion = bench.topologyVersion,
                                    renderVersion = bench.renderVersion,
                                    onCircuitChanged = { bench.syncFromCircuit() },
                                    onCollapseClick = { consoleCollapsed = true },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                } else {
                    // Compact phone & portrait tablet layout: full-width breadboard + resizable bottom IDL-800A console drawer
                    val maxDrawerHeight = (screenMaxHeight * 0.56f).coerceIn(220.dp, 460.dp)
                    val effectiveMobileHeight = mobileConsoleHeightDp.coerceIn(190.dp, maxDrawerHeight)

                    Column(modifier = Modifier.fillMaxSize()) {
                        breadboardPane(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ChassisBase)
                        ) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceCardBorder))

                            // Drag-to-resize & tap-to-collapse mobile console handle bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(maxDrawerHeight, consoleCollapsed) {
                                        detectVerticalDragGestures { change, dragAmount ->
                                            change.consume()
                                            val deltaDp = with(density) { dragAmount.toDp() }
                                            if (consoleCollapsed && deltaDp < -8.dp) {
                                                consoleCollapsed = false
                                            } else if (!consoleCollapsed) {
                                                val next = mobileConsoleHeightDp - deltaDp
                                                if (next < 150.dp && deltaDp > 6.dp) {
                                                    consoleCollapsed = true
                                                } else {
                                                    mobileConsoleHeightDp = next.coerceIn(190.dp, maxDrawerHeight)
                                                }
                                            }
                                        }
                                    }
                                    .desktopPointerHand()
                                    .clickable {
                                        Haptics.tick()
                                        consoleCollapsed = !consoleCollapsed
                                    }
                                    .padding(horizontal = Dimens.Space3, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Interactive quick Power pill right on the drawer bar
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(Dimens.RadiusPill))
                                        .background(
                                            if (bench.masterPower) PhosphorCore.copy(alpha = 0.16f)
                                            else ShortCircuitAlert.copy(alpha = 0.14f)
                                        )
                                        .border(
                                            Dimens.Hairline,
                                            if (bench.masterPower) PhosphorCore.copy(alpha = 0.65f)
                                            else ShortCircuitAlert.copy(alpha = 0.55f),
                                            RoundedCornerShape(Dimens.RadiusPill)
                                        )
                                        .clickable {
                                            bench.driveMasterPower(!bench.masterPower)
                                            Haptics.thud()
                                        }
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (bench.masterPower) "5V ON" else "0V OFF",
                                        style = LogicLabsType.TechnicalXs,
                                        color = if (bench.masterPower) PhosphorCore else ShortCircuitAlert,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "IDL-800A",
                                    style = LogicLabsType.SwitchPlate,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )

                                // Center drag grip pill + live LED bar
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(34.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(TextTertiary.copy(alpha = 0.55f))
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val rv = bench.renderVersion
                                        for (ledIdx in 7 downTo 0) {
                                            val lit = rv >= 0 && circuit.masterPower && circuit.ledValues[ledIdx]
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(if (lit) PhosphorCore else SurfaceCard)
                                                    .border(
                                                        width = Dimens.Hairline,
                                                        color = if (lit) PhosphorCore else SurfaceCardBorder,
                                                        shape = androidx.compose.foundation.shape.CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(Dimens.RadiusSm))
                                        .background(SurfaceRaised)
                                        .border(Dimens.Hairline, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(Dimens.RadiusSm))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (consoleCollapsed) "CONSOLE ▲" else "HIDE ▼",
                                        style = LogicLabsType.TechnicalXs,
                                        color = AccentCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (consoleCollapsed) {
                                // Slim interactive Quick-I/O Strip when drawer is collapsed: tap S7..S0 directly while keeping 80%+ screen for breadboard!
                                val rv = bench.renderVersion
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = Dimens.Space2, end = Dimens.Space2, bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (idx in 7 downTo 0) {
                                        val swOn = rv >= 0 && circuit.switches[idx]
                                        val ledOn = rv >= 0 && circuit.masterPower && circuit.ledValues[idx]
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(if (swOn) AccentCyan.copy(alpha = 0.20f) else SurfaceCard)
                                                .border(
                                                    width = Dimens.Hairline,
                                                    color = if (swOn) AccentCyan else SurfaceCardBorder,
                                                    shape = RoundedCornerShape(5.dp)
                                                )
                                                .clickable {
                                                    bench.toggleSwitch(idx)
                                                    Haptics.tick()
                                                }
                                                .padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(if (ledOn) PhosphorCore else SurfaceRaised)
                                            )
                                            Text(
                                                text = "S$idx",
                                                style = LogicLabsType.TechnicalXs,
                                                color = if (swOn) AccentCyan else TextSecondary
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(effectiveMobileHeight)
                                ) {
                                    TrainerConsoleDock(
                                        circuit = circuit,
                                        circuitVersion = bench.topologyVersion,
                                        renderVersion = bench.renderVersion,
                                        onCircuitChanged = { bench.syncFromCircuit() },
                                        onCollapseClick = null,
                                        showTitleBar = false,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    when (bench.overlay) {
        Overlay.NONE -> Unit

        Overlay.LAB_CATALOG -> LabCatalogSheet(
            activeLabId = activeLab?.id,
            onDismiss = { bench.overlay = Overlay.NONE },
            onLoadLab = { loadLab(it) }
        )

        Overlay.CHIP_CATALOG -> ChipCatalogSheet(
            onDismiss = { bench.overlay = Overlay.NONE },
            onPick = { part ->
                driveTopologyWithUndo { it.addChip(part, 1, nextFreeColumn(bench)) }
                Haptics.thud()
                bench.overlay = Overlay.NONE
            }
        )

        Overlay.SCOPE -> ScopeSheet(
            circuit = circuit,
            onDismiss = { bench.overlay = Overlay.NONE },
            onSample = { bench.syncFromCircuit() }
        )

        Overlay.VERIFIER -> VerifierDialog(
            report = report,
            activeLabId = activeLab?.id,
            candidateMatches = candidateMatches,
            newBadges = newBadges,
            onSelectLab = { lab ->
                activeLab = lab
                runVerification()
            },
            onRetest = { runVerification() },
            onDismiss = { bench.overlay = Overlay.NONE }
        )

        Overlay.BOARD_ACTIONS -> BoardActionsDialog(
            onClearWires = {
                driveTopologyWithUndo { it.clearWires() }
                bench.overlay = Overlay.NONE
            },
            onResetBoard = {
                relevanceJob?.cancel()
                driveTopologyWithUndo { it.clearAll() }
                activeLab = null
                report = null
                candidateMatches = emptyList()
                bench.lastVerificationPassed = null
                bench.overlay = Overlay.NONE
            },
            onReloadLab = {
                loadLab(activeLab ?: LabCurriculum.classicLabs[0])
            },
            onExportProject = {
                bench.overlay = Overlay.NONE
                exportCurrentCircuitJson()
            },
            onExportPng = {
                bench.overlay = Overlay.NONE
                exportCurrentCircuitPng()
            },
            onDismiss = { bench.overlay = Overlay.NONE }
        )
    }

    inspectedChipPartNumber?.let { part ->
        IcPinoutInspectorSheet(
            partNumber = part,
            onDismiss = { inspectedChipPartNumber = null }
        )
    }

    var pendingExitAfterSave by remember { mutableStateOf(false) }

    if (saveAsOpen) {
        SaveProjectDialog(
            defaultTitle = currentProjectTitle ?: defaultProjectTitle(activeLab),
            onConfirm = { title ->
                saveAsOpen = false
                performSave(title)
                if (pendingExitAfterSave) {
                    pendingExitAfterSave = false
                    onNavigateHome()
                }
            },
            onDismiss = {
                saveAsOpen = false
                pendingExitAfterSave = false
            }
        )
    }

    if (shareDialogOpen) {
        val shareCode = remember(circuit, bench.topologyVersion) {
            CircuitShareBytecode.encode(circuit)
        }
        ShareCircuitDialog(
            code = shareCode,
            circuitTitle = currentProjectTitle ?: defaultProjectTitle(activeLab),
            onCopied = { msg -> showNotice(msg) },
            onExportJson = {
                shareDialogOpen = false
                exportCurrentCircuitJson()
            },
            onExportPng = {
                shareDialogOpen = false
                exportCurrentCircuitPng()
            },
            onDismiss = { shareDialogOpen = false }
        )
    }

    if (confirmLeaveBench) {
        LeaveBenchDialog(
            isLab = activeLab != null,
            labTitle = activeLab?.title,
            onSaveAndExit = {
                confirmLeaveBench = false
                val title = currentProjectTitle
                if (title != null) {
                    performSave(title)
                    onNavigateHome()
                } else {
                    pendingExitAfterSave = true
                    saveAsOpen = true
                }
            },
            onDiscardAndLeave = {
                confirmLeaveBench = false
                onNavigateHome()
            },
            onKeepEditing = {
                confirmLeaveBench = false
            }
        )
    }
}

private fun defaultProjectTitle(activeLab: LabExperiment?): String =
    activeLab?.let { "Lab ${it.labNumber} · ${it.title}" } ?: "Sandbox build"

private fun recolorSelection(
    bench: com.logiclabs.app.ui.state.WorkbenchState,
    undoRedo: UndoRedoManager,
    color: WireColor
) {
    bench.activeWireColor = color
    val wire = bench.selectedWire() ?: return
    undoRedo.pushUndoSnapshot(bench.circuit)
    bench.driveTopology {
        it.removeWire(wire.id)
        it.addWire(wire.startSocket, wire.endSocket, color, wire.isManhattan, wire.id)
    }
}

private fun flipSelectedWire(
    bench: com.logiclabs.app.ui.state.WorkbenchState,
    undoRedo: UndoRedoManager
) {
    val wire = bench.selectedWire() ?: return
    undoRedo.pushUndoSnapshot(bench.circuit)
    bench.driveTopology {
        it.removeWire(wire.id)
        it.addWire(wire.endSocket, wire.startSocket, wire.color, wire.isManhattan, wire.id)
    }
}

private fun toggleSelectedRouting(
    bench: com.logiclabs.app.ui.state.WorkbenchState,
    undoRedo: UndoRedoManager
) {
    val wire = bench.selectedWire() ?: return
    undoRedo.pushUndoSnapshot(bench.circuit)
    bench.driveTopology {
        it.removeWire(wire.id)
        it.addWire(wire.startSocket, wire.endSocket, wire.color, !wire.isManhattan, wire.id)
    }
}

private fun nextFreeColumn(bench: com.logiclabs.app.ui.state.WorkbenchState): Int {
    val occupied = bench.circuit.placedChips
        .filter { it.placedIc.trench == 1 }
        .map { it.placedIc.startColumn }
    var col = 10
    while (col <= 50) {
        if (occupied.none { kotlin.math.abs(it - col) < 8 }) return col
        col += 8
    }
    return 10
}

@Composable
private fun BoardActionsDialog(
    onClearWires: () -> Unit,
    onResetBoard: () -> Unit,
    onReloadLab: () -> Unit,
    onExportProject: (() -> Unit)?,
    onExportPng: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    BenchDialog(
        onDismissRequest = onDismiss,
        icon = LogicIcons.Breadboard,
        title = "Board actions",
        message = "Nothing is selected — these act on the whole breadboard (you can still press Ctrl+Z to undo).",
        actions = {
            if (onExportPng != null) {
                BenchDialogAction(
                    "DOWNLOAD PNG SNAPSHOT", onExportPng,
                    tone = BenchDialogTone.ACCENT, leadingIcon = LogicIcons.Camera
                )
            }
            if (onExportProject != null) {
                BenchDialogAction(
                    "EXPORT PROJECT (.JSON)", onExportProject,
                    tone = BenchDialogTone.ACCENT, leadingIcon = LogicIcons.Export
                )
            }
            BenchDialogAction(
                "CLEAR ALL WIRES", onClearWires,
                tone = BenchDialogTone.ACCENT, leadingIcon = LogicIcons.Trash
            )
            BenchDialogAction(
                "RELOAD EXPERIMENT", onReloadLab,
                tone = BenchDialogTone.ACCENT, leadingIcon = LogicIcons.Reload
            )
            BenchDialogAction("CANCEL", onDismiss, tone = BenchDialogTone.NEUTRAL)
            BenchDialogActionDivider()
            BenchDialogAction(
                "RESET BOARD", onResetBoard,
                tone = BenchDialogTone.DESTRUCTIVE, leadingIcon = LogicIcons.Power
            )
        }
    )
}

@Composable
private fun SaveProjectDialog(
    defaultTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(defaultTitle) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    BenchDialog(
        onDismissRequest = onDismiss,
        icon = LogicIcons.Save,
        title = "Save project",
        message = "Name this build — it is stored locally in ${if (AppPlatform.isWeb) "browser storage (works offline)" else "this PC"}.",
        content = {
            Spacer(Modifier.height(Dimens.Space3))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                textStyle = LogicLabsType.BodyMd.copy(color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentCyan,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = SurfaceCardBorder,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { ev ->
                        if (ev.type == KeyEventType.KeyDown &&
                            (ev.key == Key.Enter || ev.key == Key.NumPadEnter) &&
                            title.isNotBlank()
                        ) {
                            onConfirm(title.trim())
                            true
                        } else false
                    }
            )
        },
        actions = {
            BenchDialogAction(
                "SAVE", { onConfirm(title.trim()) },
                enabled = title.isNotBlank(), leadingIcon = LogicIcons.Save
            )
            BenchDialogAction("CANCEL", onDismiss, tone = BenchDialogTone.NEUTRAL)
        }
    )
}

@Composable
private fun LeaveBenchDialog(
    isLab: Boolean,
    labTitle: String?,
    onSaveAndExit: () -> Unit,
    onDiscardAndLeave: () -> Unit,
    onKeepEditing: () -> Unit
) {
    val title = if (isLab) "Exit lab experiment?" else "Exit simulation?"
    val message = if (isLab) {
        if (labTitle != null) {
            "Leaving “$labTitle” clears the breadboard. Save this build as a project first if you want to keep the wiring."
        } else {
            "Leaving clears the breadboard. Save this build as a project first if you want to keep the wiring."
        }
    } else {
        "The bench is still wired. Save it as a project to keep it, or leave without saving."
    }

    BenchDialog(
        onDismissRequest = onKeepEditing,
        icon = LogicIcons.Power,
        title = title,
        message = message,
        actions = {
            BenchDialogAction(
                "SAVE & EXIT", onSaveAndExit,
                leadingIcon = LogicIcons.Save
            )
            BenchDialogAction("KEEP WORKING", onKeepEditing, tone = BenchDialogTone.NEUTRAL)
            BenchDialogActionDivider()
            BenchDialogAction(
                if (isLab) "EXIT LAB" else "EXIT WITHOUT SAVING",
                onDiscardAndLeave,
                tone = BenchDialogTone.DESTRUCTIVE,
                leadingIcon = LogicIcons.Power
            )
        }
    )
}

@Composable
private fun ShareCircuitDialog(
    code: String,
    circuitTitle: String = "Logic Labs Circuit",
    onCopied: (String) -> Unit,
    onExportJson: () -> Unit,
    onExportPng: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    BenchDialog(
        onDismissRequest = onDismiss,
        icon = LogicIcons.Share,
        title = "Share & export circuit",
        message = "Share a live 1-click link, copy cross-platform bytecode, or download a High-DPI breadboard PNG / JSON file.",
        content = {
            Spacer(Modifier.height(Dimens.Space3))
            val codeScroll = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp)
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .background(SurfaceCard)
                    .border(Dimens.Hairline, SurfaceCardBorder, RoundedCornerShape(Dimens.RadiusSm))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(codeScroll)
                        .padding(start = Dimens.Space3, end = Dimens.Space4, top = Dimens.Space3, bottom = Dimens.Space3)
                ) {
                    Text(
                        text = code,
                        style = LogicLabsType.TechnicalXs.copy(fontFamily = FontFamily.Monospace),
                        color = AccentCyan
                    )
                }
                DesktopVerticalScrollbar(
                    scrollState = codeScroll,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(2.dp)
                )
            }
        },
        actions = {
            val shareUrl = AppPlatform.buildShareUrl(code)
            if (shareUrl != null) {
                BenchDialogAction(
                    "COPY DIRECT LINK",
                    onClick = {
                        AppPlatform.copyToClipboard(shareUrl)
                        Haptics.tick()
                        onCopied("Direct live circuit URL copied to clipboard")
                        onDismiss()
                    },
                    leadingIcon = LogicIcons.Share,
                    tone = BenchDialogTone.ACCENT
                )
                if (AppPlatform.canNativeShare()) {
                    BenchDialogAction(
                        "SHARE VIA DEVICE...",
                        onClick = {
                            Haptics.tick()
                            AppPlatform.nativeShareUrl(
                                title = "$circuitTitle — Logic Labs",
                                text = "Open this live K&H IDL-800A breadboard circuit in Logic Labs:",
                                url = shareUrl
                            )
                            onDismiss()
                        },
                        leadingIcon = LogicIcons.Share,
                        tone = BenchDialogTone.ACCENT
                    )
                }
            }
            if (onExportPng != null) {
                BenchDialogAction(
                    "DOWNLOAD PNG IMAGE",
                    onClick = onExportPng,
                    leadingIcon = LogicIcons.Camera,
                    tone = BenchDialogTone.ACCENT
                )
            }
            BenchDialogAction(
                "COPY CODE",
                onClick = {
                    AppPlatform.copyToClipboard(code)
                    Haptics.tick()
                    onCopied("Circuit bytecode copied to clipboard")
                    onDismiss()
                },
                leadingIcon = LogicIcons.Export,
                tone = BenchDialogTone.ACCENT
            )
            BenchDialogAction(
                "EXPORT .JSON FILE...",
                onClick = onExportJson,
                leadingIcon = LogicIcons.Save,
                tone = BenchDialogTone.ACCENT
            )
            BenchDialogAction("DISMISS", onDismiss, tone = BenchDialogTone.NEUTRAL)
        }
    )
}

/**
 * Resolves the best-matching curriculum lab for an unguided circuit build.
 */
fun findBestMatchingLab(
    placedChips: List<String>,
    connectedSwitchCount: Int,
    connectedLedCount: Int,
    circuit: BreadboardCircuit? = null,
    connectedSwitches: List<Int> = emptyList(),
    connectedLeds: List<Int> = emptyList()
): LabExperiment {
    val placedSet = placedChips.toSet()
    if (placedSet.isEmpty()) return LabCurriculum.classicLabs[0]

    val candidates = ExperimentCatalog.allLabs.filter { lab ->
        lab.targetChips.isNotEmpty() && lab.targetChips.all { it in placedSet }
    }

    if (candidates.isEmpty()) {
        return ExperimentCatalog.allLabs.find { lab ->
            lab.targetChips.any { it in placedSet }
        } ?: LabCurriculum.classicLabs[0]
    }

    return candidates.maxByOrNull { lab ->
        var score = 0
        val labChipSet = lab.targetChips.toSet()
        if (labChipSet == placedSet) score += 1000
        score += lab.targetChips.size * 100
        if (connectedSwitchCount > 0) {
            val diff = kotlin.math.abs(lab.switchIndices.size - connectedSwitchCount)
            score += (8 - diff) * 20
        }
        if (connectedLedCount > 0) {
            val diff = kotlin.math.abs(lab.ledIndices.size - connectedLedCount)
            score += (8 - diff) * 10
        }

        if (lab.id.contains("unsimplified")) {
            score -= 200
        }

        val isDefaultVariation = ExperimentCatalog.groupOf(lab.id)?.default?.id == lab.id
        if (isDefaultVariation) {
            score += 50
        }

        if (circuit != null && (connectedSwitches.isNotEmpty() || connectedLeds.isNotEmpty())) {
            val fallbackSw = if (connectedSwitches.size >= lab.switchIndices.size) connectedSwitches.take(lab.switchIndices.size) else lab.switchIndices
            val fallbackLed = if (connectedLeds.size >= lab.ledIndices.size) connectedLeds.take(lab.ledIndices.size) else lab.ledIndices

            val (_, _, matchingRows) = discoverBestIoMapping(
                circuit = circuit,
                connectedSwitches = connectedSwitches,
                connectedLeds = connectedLeds,
                requiredSwitchCount = lab.switchIndices.size,
                requiredLedCount = lab.ledIndices.size,
                expectedFunction = lab.expectedFunction,
                fallbackSwitches = fallbackSw,
                fallbackLeds = fallbackLed
            )

            val n = lab.switchIndices.size.coerceIn(1, 8)
            val vectorCount = 1 shl n

            if (matchingRows == vectorCount) {
                score += 10000 + vectorCount * 50
            } else {
                score += matchingRows * 10
            }
        }

        score
    } ?: candidates.first()
}

fun discoverBestIoMapping(
    circuit: BreadboardCircuit,
    connectedSwitches: List<Int>,
    connectedLeds: List<Int>,
    requiredSwitchCount: Int,
    requiredLedCount: Int,
    expectedFunction: (List<Boolean>) -> List<Boolean>,
    fallbackSwitches: List<Int>,
    fallbackLeds: List<Int>
): Triple<List<Int>, List<Int>, Int> {
    val n = requiredSwitchCount.coerceIn(1, 8)
    val vectorCount = 1 shl n

    val swCombinations: List<List<Int>> = if (connectedSwitches.size >= n) {
        generateCombinations(connectedSwitches, n)
    } else {
        val padded = connectedSwitches.toMutableList()
        for (sw in fallbackSwitches) {
            if (padded.size >= n) break
            if (sw !in padded) padded.add(sw)
        }
        var c = 0
        while (padded.size < n && c < 8) {
            if (c !in padded) padded.add(c)
            c++
        }
        listOf(padded)
    }

    val swCandidates: List<List<Int>> = swCombinations.flatMap { comb ->
        when {
            n <= 3 -> generatePermutations(comb)
            n == 4 && swCombinations.size <= 15 -> generatePermutations(comb)
            else -> listOf(comb, comb.reversed())
        }
    }

    val ledCombinations: List<List<Int>> = if (connectedLeds.size >= requiredLedCount) {
        generateCombinations(connectedLeds, requiredLedCount)
    } else {
        val padded = connectedLeds.toMutableList()
        for (led in fallbackLeds) {
            if (padded.size >= requiredLedCount) break
            if (led !in padded) padded.add(led)
        }
        var c = 0
        while (padded.size < requiredLedCount && c < 8) {
            if (c !in padded) padded.add(c)
            c++
        }
        listOf(padded)
    }

    val ledCandidates: List<List<Int>> = ledCombinations.flatMap { comb ->
        if (requiredLedCount <= 2) generatePermutations(comb) else listOf(comb, comb.reversed())
    }

    val origSwitches = circuit.switches.copyOf()

    if (fallbackSwitches.size == n && fallbackLeds.size == requiredLedCount) {
        for (i in circuit.switches.indices) {
            if (i !in fallbackSwitches) circuit.switches[i] = false
        }
        var fallbackPassCount = 0
        for (v in 0 until vectorCount) {
            val inputs = mutableListOf<Boolean>()
            for (i in 0 until n) {
                val bit = ((v shr (n - 1 - i)) and 1) == 1
                inputs.add(bit)
                circuit.switches[fallbackSwitches[i]] = bit
            }
            circuit.step()
            val actual = fallbackLeds.map { circuit.ledValues[it] }
            if (actual == expectedFunction(inputs)) fallbackPassCount++
        }
        if (fallbackPassCount == vectorCount) {
            origSwitches.copyInto(circuit.switches)
            circuit.step()
            return Triple(fallbackSwitches, fallbackLeds, vectorCount)
        }
    }

    var bestSw = fallbackSwitches
    var bestLed = fallbackLeds
    var bestMatchingRows = 0

    searchLoop@ for (candSw in swCandidates) {
        for (candLed in ledCandidates) {
            for (i in circuit.switches.indices) {
                if (i !in candSw) circuit.switches[i] = false
            }
            var matchingRows = 0
            for (v in 0 until vectorCount) {
                val inputs = mutableListOf<Boolean>()
                for (i in 0 until n) {
                    val bit = ((v shr (n - 1 - i)) and 1) == 1
                    inputs.add(bit)
                    circuit.switches[candSw[i]] = bit
                }
                circuit.step()
                val actual = candLed.map { circuit.ledValues[it] }
                if (actual == expectedFunction(inputs)) {
                    matchingRows++
                } else if (matchingRows + (vectorCount - 1 - v) <= bestMatchingRows) {
                    break
                }
            }

            if (matchingRows > bestMatchingRows) {
                bestMatchingRows = matchingRows
                bestSw = candSw
                bestLed = candLed
            }
            if (bestMatchingRows == vectorCount) break@searchLoop
        }
    }

    origSwitches.copyInto(circuit.switches)
    circuit.step()

    return Triple(bestSw, bestLed, bestMatchingRows)
}

fun <T> generateCombinations(list: List<T>, k: Int): List<List<T>> {
    if (k <= 0) return listOf(emptyList())
    if (list.size < k) return emptyList()
    if (list.size == k) return listOf(list)
    val head = list.first()
    val tail = list.drop(1)
    val withHead = generateCombinations(tail, k - 1).map { listOf(head) + it }
    val withoutHead = generateCombinations(tail, k)
    return withHead + withoutHead
}

fun <T> generatePermutations(list: List<T>): List<List<T>> {
    if (list.size <= 1) return listOf(list)
    val result = mutableListOf<List<T>>()
    for (i in list.indices) {
        val element = list[i]
        val remaining = list.take(i) + list.drop(i + 1)
        for (subPerm in generatePermutations(remaining)) {
            result.add(listOf(element) + subPerm)
        }
    }
    return result
}
