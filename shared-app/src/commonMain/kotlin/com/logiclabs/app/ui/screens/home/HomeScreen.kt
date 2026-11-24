package com.logiclabs.app.ui.screens.home

import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logiclabs.app.platform.AppPlatform
import com.logiclabs.app.di.AppContainer
import com.logiclabs.app.ui.component.BenchDialog
import com.logiclabs.app.ui.component.BenchDialogAction
import com.logiclabs.app.ui.component.BenchDialogActionDivider
import com.logiclabs.app.ui.component.BenchDialogTone
import com.logiclabs.app.ui.component.BoardThumbnail
import com.logiclabs.app.ui.navigation.BenchRequest
import com.logiclabs.app.ui.screens.settings.SettingsScreen
import com.logiclabs.app.ui.sheets.BadgesSheet
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.data.persistence.ChipSpot
import com.logiclabs.core.data.persistence.CircuitShareBytecode
import com.logiclabs.core.data.persistence.ProjectSummary
import com.logiclabs.core.data.persistence.WireSpot
import com.logiclabs.core.data.progress.BadgeCatalog
import com.logiclabs.core.data.progress.BenchStats
import com.logiclabs.core.data.session.Session
import com.logiclabs.core.data.session.SessionKind
import com.logiclabs.core.designsystem.component.ChamferedPanel
import com.logiclabs.core.designsystem.component.DesktopVerticalScrollbar
import com.logiclabs.core.designsystem.component.LogicIcons
import com.logiclabs.core.designsystem.component.TelemetryPill
import com.logiclabs.core.designsystem.component.desktopPointerHand
import com.logiclabs.core.designsystem.component.keyboardPageScroll
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.AmberCore
import com.logiclabs.core.designsystem.theme.CanvasBackground
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.Motion
import com.logiclabs.core.designsystem.theme.PhosphorCore
import com.logiclabs.core.designsystem.theme.ShortCircuitAlert
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.SurfaceCardBorder
import com.logiclabs.core.designsystem.theme.SurfaceRaised
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextSecondary
import com.logiclabs.core.designsystem.theme.TextTertiary
import com.logiclabs.feature.tools.courseware.ExperimentCatalog
import com.logiclabs.feature.tools.courseware.LabCurriculum
import com.logiclabs.feature.tools.courseware.LabExperiment
import com.logiclabs.feature.tools.feedback.Haptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Desktop Workstation Home Hub.
 *
 * Replaces the phone vertical bottom-bar shell with a persistent left navigation rail
 * ([DesktopHomeSidebar]) and a responsive widescreen content pane displaying the Continue
 * hero, Sandbox card, telemetry stats, and a 2-column Saved Projects grid.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onContinue: (BenchRequest) -> Unit,
    onOpenControlsGuide: () -> Unit,
    importJsonTrigger: Int = 0
) {
    val session by container.session.lastSession.collectAsState(initial = null)
    val stats by container.progress.stats.collectAsState(initial = BenchStats())

    val classicLabs = remember { LabCurriculum.classicLabs }
    val classicCount = classicLabs.size
    val verifiedClassic = classicLabs.count { it.id in stats.verifiedLabIds }

    var showBadges by remember { mutableStateOf(false) }
    var showImportCodeDialog by remember { mutableStateOf(false) }
    var deletingProject by remember { mutableStateOf<ProjectSummary?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var projects by remember { mutableStateOf<List<ProjectSummary>>(emptyList()) }
    LaunchedEffect(container.projects.version) {
        projects = container.projects.list()
    }

    fun triggerJsonImport() {
        AppPlatform.importJsonText { fileName, text ->
            scope.launch {
                if (text == null) {
                    snackbarHostState.showSnackbar("Could not read $fileName", duration = SnackbarDuration.Short)
                    return@launch
                }
                val imported = container.projects.import(text)
                if (imported != null) {
                    Haptics.success()
                    snackbarHostState.showSnackbar(
                        "Imported project: ${imported.title}",
                        duration = SnackbarDuration.Short
                    )
                } else {
                    snackbarHostState.showSnackbar(
                        "Invalid Logic Labs project file: $fileName",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    LaunchedEffect(importJsonTrigger) {
        if (importJsonTrigger > 0) {
            triggerJsonImport()
        }
    }

    val entered = true

    val mainPane: @Composable (Modifier) -> Unit = { paneModifier ->
        Box(modifier = paneModifier) {
            when (selectedTab) {
                HomeTab.PROJECTS -> {
                    val projectsListState = rememberLazyListState()
                    val quickStartLabs = remember(classicLabs) {
                        val preferred = listOf(1, 4, 7, 10)
                        preferred.mapNotNull { num -> classicLabs.firstOrNull { it.labNumber == num } }
                            .ifEmpty { classicLabs.take(4) }
                    }
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .keyboardPageScroll(projectsListState)
                    ) {
                        val isWide = maxWidth >= 780.dp
                        LazyColumn(
                            state = projectsListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 12.dp),
                            contentPadding = PaddingValues(
                                start = if (maxWidth >= 600.dp) Dimens.Space5 else Dimens.Space3,
                                end = if (maxWidth >= 600.dp) Dimens.Space4 else Dimens.Space3,
                                top = Dimens.Space4,
                                bottom = Dimens.Space4
                            ),
                            verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
                        ) {
                            item(key = "hero_row") {
                                if (isWide) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
                                    ) {
                                        ContinueHero(
                                            session = session,
                                            projects = projects,
                                            entered = entered,
                                            onClick = { onContinue(BenchRequest.Continue) },
                                            modifier = Modifier.weight(1.45f).fillMaxHeight()
                                        )
                                        SandboxCard(
                                            entered = entered,
                                            onClick = { onContinue(BenchRequest.Sandbox) },
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
                                        ContinueHero(
                                            session = session,
                                            projects = projects,
                                            entered = entered,
                                            onClick = { onContinue(BenchRequest.Continue) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        SandboxCard(
                                            entered = entered,
                                            onClick = { onContinue(BenchRequest.Sandbox) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            item(key = "stats") {
                                StatsStrip(
                                    stats = stats,
                                    verifiedClassic = verifiedClassic,
                                    classicCount = classicCount,
                                    entered = entered,
                                    onShowBadges = { showBadges = true }
                                )
                            }

                            item(key = "projects-header") {
                                SectionHeader(
                                    title = "SAVED PROJECTS",
                                    countLabel = if (AppPlatform.isWeb) "${projects.size} SAVED" else "${projects.size} ON THIS PC",
                                    progress = -1f,
                                    entered = entered,
                                    trailing = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ImportChip(onClick = { triggerJsonImport() })
                                            ImportCodeChip(onClick = { showImportCodeDialog = true })
                                        }
                                    }
                                )
                            }

                            if (projects.isEmpty()) {
                                item(key = "projects-empty") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(Dimens.RadiusMd))
                                            .background(SurfaceCard.copy(alpha = 0.6f))
                                            .border(Dimens.Hairline, SurfaceCardBorder, RoundedCornerShape(Dimens.RadiusMd))
                                            .padding(Dimens.Space4)
                                    ) {
                                        Text(
                                            text = "No saved circuits yet. Build something on the bench and press SAVE (Ctrl+S), " +
                                                "or import a shared .logiclabs.json / LOGIC-... code above.",
                                            style = LogicLabsType.BodySm,
                                            color = TextTertiary
                                        )
                                    }
                                }
                            } else if (isWide) {
                                val rows = projects.chunked(2)
                                itemsIndexed(rows, key = { idx, pair -> "row_${idx}_${pair.first().id}" }) { rowIndex, pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
                                    ) {
                                        pair.forEachIndexed { colIndex, project ->
                                            ProjectCard(
                                                project = project,
                                                entered = entered,
                                                index = rowIndex * 2 + colIndex,
                                                onClick = { onContinue(BenchRequest.Project(project.id)) },
                                                onDelete = { deletingProject = project },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (pair.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                itemsIndexed(projects, key = { _, p -> "p_${p.id}" }) { index, project ->
                                    ProjectCard(
                                        project = project,
                                        entered = entered,
                                        index = index,
                                        onClick = { onContinue(BenchRequest.Project(project.id)) },
                                        onDelete = { deletingProject = project },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Quick-Start Classic Labs shelf fills first-launch workspace with 1-click circuits
                            item(key = "quick-start-header") {
                                SectionHeader(
                                    title = "QUICK-START CLASSIC LABS",
                                    countLabel = "$verifiedClassic / $classicCount VERIFIED",
                                    progress = if (classicCount > 0) verifiedClassic.toFloat() / classicCount else 0f,
                                    entered = entered,
                                    trailing = {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(Dimens.RadiusPill))
                                                .background(AmberCore.copy(alpha = 0.12f))
                                                .border(Dimens.Hairline, AmberCore.copy(alpha = 0.55f), RoundedCornerShape(Dimens.RadiusPill))
                                                .desktopPointerHand()
                                                .clickable {
                                                    Haptics.tick()
                                                    onTabSelected(HomeTab.COURSEWORK)
                                                }
                                                .padding(horizontal = Dimens.Space3, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "ALL 49 LABS ›",
                                                style = LogicLabsType.TechnicalXs,
                                                color = AmberCore,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                )
                            }

                            item(key = "quick-start-grid") {
                                QuickStartLabsGrid(
                                    labs = quickStartLabs,
                                    verifiedIds = stats.verifiedLabIds,
                                    isWide = isWide,
                                    onLaunch = { labId -> onContinue(BenchRequest.Lab(labId)) }
                                )
                            }

                            item(key = "footer") {
                                Footer()
                            }
                        }

                        DesktopVerticalScrollbar(
                            listState = projectsListState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = Dimens.Space3, horizontal = 3.dp)
                        )
                    }
                }

                HomeTab.COURSEWORK -> {
                    CourseworkTab(
                        verifiedLabIds = stats.verifiedLabIds,
                        onLaunchLab = { labId -> onContinue(BenchRequest.Lab(labId)) }
                    )
                }

                HomeTab.REFERENCE -> {
                    IcReferenceTab()
                }

                HomeTab.SETTINGS -> {
                    SettingsScreen(
                        container = container,
                        onBack = { onTabSelected(HomeTab.PROJECTS) },
                        showBackButton = false,
                        onOpenControlsGuide = onOpenControlsGuide
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimens.Space4)
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBackground)
    ) {
        if (maxWidth >= 760.dp) {
            Row(modifier = Modifier.fillMaxSize()) {
                DesktopHomeSidebar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    onNewCircuit = { onContinue(BenchRequest.Sandbox) },
                    onOpenControlsGuide = onOpenControlsGuide,
                    isPortableMode = container.isPortableMode
                )
                mainPane(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                MobileHomeTopBar(
                    onNewCircuit = { onContinue(BenchRequest.Sandbox) },
                    onOpenControlsGuide = onOpenControlsGuide
                )
                mainPane(Modifier.weight(1f).fillMaxWidth())
                MobileHomeBottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        }
    }

    if (showBadges) {
        BadgesSheet(
            stats = stats,
            classicLabCount = classicCount,
            onDismiss = { showBadges = false }
        )
    }

    if (showImportCodeDialog) {
        ImportCodeDialog(
            onDismiss = { showImportCodeDialog = false },
            onImplement = { code ->
                scope.launch {
                    try {
                        val project = CircuitShareBytecode.decodeToProject(code)
                        container.projects.saveDraft(project)
                        container.session.recordSandbox()
                        showImportCodeDialog = false
                        Haptics.success()
                        onContinue(BenchRequest.Continue)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            "Failed to import code: ${e.message}",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        )
    }

    deletingProject?.let { project ->
        BenchDialog(
            onDismissRequest = { deletingProject = null },
            icon = LogicIcons.Trash,
            headerTone = BenchDialogTone.DESTRUCTIVE,
            title = "Delete project?",
            message = "“${project.title}” — ${project.chipCount} ICs and ${project.wireCount} wires — " +
                "will be removed from ${if (AppPlatform.isWeb) "browser storage" else "this PC"}. This cannot be undone.",
            actions = {
                BenchDialogAction("CANCEL", { deletingProject = null }, tone = BenchDialogTone.NEUTRAL)
                BenchDialogActionDivider()
                BenchDialogAction(
                    "DELETE", {
                        scope.launch { container.projects.delete(project.id) }
                        deletingProject = null
                    },
                    tone = BenchDialogTone.DESTRUCTIVE,
                    leadingIcon = LogicIcons.Trash
                )
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Cards
// ---------------------------------------------------------------------------

private data class HeroContent(
    val label: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

private fun getUnitAndExp(lab: LabExperiment): Pair<String, String> {
    if (lab.id.startsWith("exp")) {
        val num = lab.id.removePrefix("exp").take(2).toIntOrNull() ?: lab.labNumber
        return "EXTENDED BENCH" to "EXP ${com.logiclabs.core.bridge.platform.padZero2(num)}"
    }
    return when (lab.labNumber) {
        1, 2, 3, 4, 5, 6 -> "UNIT 01: LOGIC GATES" to "EXP ${com.logiclabs.core.bridge.platform.padZero2(lab.labNumber)}"
        7 -> "UNIT 03: ARITHMETIC ARCHITECTURE" to "EXP 07"
        8, 9 -> "UNIT 02: COMBINATIONAL SYSTEMS" to "EXP ${com.logiclabs.core.bridge.platform.padZero2(lab.labNumber)}"
        10, 11 -> "UNIT 04: SEQUENTIAL REGISTERS" to "EXP ${com.logiclabs.core.bridge.platform.padZero2(lab.labNumber)}"
        12 -> "UNIT 03: ARITHMETIC ARCHITECTURE" to "EXP 12"
        else -> "EXTENDED BENCH" to "EXP ${com.logiclabs.core.bridge.platform.padZero2(lab.labNumber)}"
    }
}

@Composable
private fun ContinueHero(
    session: Session?,
    projects: List<ProjectSummary>,
    entered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (chips, wires) = remember(session?.id, session?.kind, projects) {
        when (session?.kind) {
            SessionKind.LAB -> {
                val lab = session.id?.let { ExperimentCatalog.findById(it) }
                if (lab != null) {
                    val circuit = BreadboardCircuit()
                    runCatching { lab.buildCircuit(circuit) }
                    val c = circuit.placedChips.map {
                        ChipSpot(
                            partNumber = it.placedIc.partNumber,
                            trench = it.placedIc.trench,
                            startColumn = it.placedIc.startColumn,
                            isRotated180 = it.placedIc.isRotated180
                        )
                    }
                    val w = circuit.wires.map {
                        WireSpot(
                            startSocket = it.startSocket,
                            endSocket = it.endSocket,
                            colorArgb = it.color.hexArgb
                        )
                    }
                    c to w
                } else {
                    emptyList<ChipSpot>() to emptyList<WireSpot>()
                }
            }
            SessionKind.PROJECT -> {
                val proj = projects.find { it.id == session.id }
                (proj?.chipSpots ?: emptyList()) to (proj?.wireSpots ?: emptyList())
            }
            else -> emptyList<ChipSpot>() to emptyList<WireSpot>()
        }
    }

    val content = when (session?.kind) {
        SessionKind.LAB -> {
            val lab = ExperimentCatalog.findById(session.id ?: "")
            if (lab != null) {
                val (unitName, expName) = getUnitAndExp(lab)
                val shortUnit = unitName.substringBefore(":")
                HeroContent(
                    label = "CONTINUE · $shortUnit: $expName",
                    title = lab.title,
                    subtitle = lab.subtitle,
                    icon = LogicIcons.Labs,
                    accent = PhosphorCore
                )
            } else {
                introContent()
            }
        }
        SessionKind.PROJECT -> {
            val proj = projects.find { it.id == session.id }
            HeroContent(
                label = "CONTINUE PROJECT",
                title = proj?.title ?: "Saved circuit",
                subtitle = proj?.let { "${it.chipCount} ICs · ${it.wireCount} wires" }
                    ?: "Reload your last build onto the breadboard",
                icon = LogicIcons.Chip,
                accent = AccentCyan
            )
        }
        SessionKind.SANDBOX -> HeroContent(
            label = "CONTINUE SANDBOX",
            title = "Open bench",
            subtitle = "Back to your free-build bench",
            icon = LogicIcons.Breadboard,
            accent = AccentCyan
        )
        null -> introContent()
    }

    PressableCard(
        modifier = modifier.enterStagger(entered, index = 0),
        accent = content.accent,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.Space4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (chips.isNotEmpty() || wires.isNotEmpty()) {
                    BoardThumbnail(
                        chips = chips,
                        wires = wires,
                        modifier = Modifier
                            .size(width = 96.dp, height = 64.dp)
                            .clip(RoundedCornerShape(Dimens.RadiusSm))
                            .background(SurfaceCard)
                            .border(Dimens.Hairline, content.accent.copy(alpha = 0.35f), RoundedCornerShape(Dimens.RadiusSm))
                    )
                } else {
                    IconWell(icon = content.icon, accent = content.accent, size = 54.dp)
                }
                Spacer(Modifier.width(Dimens.Space3))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = content.label,
                        style = LogicLabsType.SwitchPlate,
                        color = content.accent,
                        maxLines = 1
                    )
                    Text(
                        text = content.title,
                        style = LogicLabsType.TitleMd,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = content.subtitle,
                        style = LogicLabsType.BodySm,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(Dimens.Space2))
                Icon(
                    imageVector = LogicIcons.ChevronRight,
                    contentDescription = null,
                    tint = content.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Dimens.Space4, end = Dimens.Space4, bottom = Dimens.Space3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.RadiusPill))
                        .background(content.accent.copy(alpha = 0.14f))
                        .border(
                            Dimens.Hairline,
                            content.accent.copy(alpha = 0.8f),
                            RoundedCornerShape(Dimens.RadiusPill)
                        )
                        .padding(horizontal = Dimens.Space3, vertical = Dimens.Space1)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = LogicIcons.Power,
                            contentDescription = null,
                            tint = content.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = " POWER ON",
                            style = LogicLabsType.SwitchPlateLg,
                            color = content.accent,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun introContent(): HeroContent = HeroContent(
    label = "START HERE · UNIT 01: EXP 01",
    title = "EXP 01 · Inverter / NOT Gate Logic",
    subtitle = "7404 Hex Inverter IC — your first powered experiment",
    icon = LogicIcons.Labs,
    accent = PhosphorCore
)

@Composable
private fun IconWell(icon: ImageVector, accent: Color, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(accent.copy(alpha = 0.12f))
            .border(Dimens.Hairline, accent.copy(alpha = 0.4f), RoundedCornerShape(Dimens.RadiusMd)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(size / 2.1f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsStrip(
    stats: BenchStats,
    verifiedClassic: Int,
    classicCount: Int,
    entered: Boolean,
    onShowBadges: () -> Unit
) {
    val earned = BadgeCatalog.earnedBy(stats, classicCount).size
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .enterStagger(entered, index = 1),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        TelemetryPill(label = "LABS", value = "$verifiedClassic/$classicCount", accent = PhosphorCore)
        TelemetryPill(label = "CIRCUITS", value = stats.circuitsBuilt.toString(), accent = AccentCyan)
        TelemetryPill(label = "BENCH", value = formatBenchTime(stats.benchTimeMillis), accent = AmberCore)

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.RadiusPill))
                .background(SurfaceCard)
                .border(Dimens.Hairline, AmberCore.copy(alpha = 0.35f), RoundedCornerShape(Dimens.RadiusPill))
                .clickable(onClick = onShowBadges)
                .semantics { contentDescription = "Show achievements" }
                .padding(horizontal = Dimens.Space2, vertical = Dimens.Space1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space1)
        ) {
            Icon(
                imageVector = LogicIcons.Rosette,
                contentDescription = null,
                tint = AmberCore,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "$earned/${BadgeCatalog.all.size} BADGES",
                style = LogicLabsType.TechnicalSm,
                color = AmberCore,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    countLabel: String,
    progress: Float,
    entered: Boolean,
    trailing: (@Composable () -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .enterStagger(entered, index = 2)
            .padding(top = Dimens.Space2, bottom = Dimens.Space1)
    ) {
        if (maxWidth >= 520.dp || trailing == null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = LogicLabsType.SwitchPlateLg,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (trailing != null) {
                    trailing()
                    Spacer(Modifier.width(Dimens.Space2))
                }
                Text(
                    text = countLabel,
                    style = LogicLabsType.TechnicalSm,
                    color = if (progress >= 0f) PhosphorCore else TextTertiary,
                    maxLines = 1
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = LogicLabsType.SwitchPlateLg,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(Dimens.Space2))
                    Text(
                        text = countLabel,
                        style = LogicLabsType.TechnicalSm,
                        color = if (progress >= 0f) PhosphorCore else TextTertiary,
                        maxLines = 1
                    )
                }
                trailing()
            }
        }
    }
}

@Composable
private fun SandboxCard(
    entered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PressableCard(
        modifier = modifier.enterStagger(entered, index = 1),
        accent = AccentCyan,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(Dimens.Space4),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconWell(icon = LogicIcons.Breadboard, accent = AccentCyan, size = 48.dp)
                Spacer(Modifier.width(Dimens.Space3))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "SANDBOX",
                        style = LogicLabsType.SwitchPlateLg,
                        color = AccentCyan,
                        maxLines = 1
                    )
                    Text(
                        text = "Empty powered bench · free build",
                        style = LogicLabsType.BodyMd,
                        color = TextPrimary,
                        maxLines = 2
                    )
                }
                Icon(
                    imageVector = LogicIcons.ChevronRight,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(Dimens.Space2))
            Text(
                text = "Place any 74xx IC, route jumpers, probe with oscilloscope, or verify against 49 experiments.",
                style = LogicLabsType.BodySm,
                color = TextTertiary,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun QuickStartLabsGrid(
    labs: List<LabExperiment>,
    verifiedIds: Set<String>,
    isWide: Boolean,
    onLaunch: (String) -> Unit
) {
    val rows = if (isWide) listOf(labs) else labs.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
        rows.forEach { rowLabs ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                rowLabs.forEach { lab ->
                    val verified = lab.id in verifiedIds
                    val accent = if (verified) PhosphorCore else AccentCyan
                    PressableCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        accent = accent,
                        onClick = { onLaunch(lab.id) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.Space3),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EXP ${com.logiclabs.core.bridge.platform.padZero2(lab.labNumber)}",
                                    style = LogicLabsType.TechnicalXs,
                                    color = if (verified) PhosphorCore else AmberCore,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accent.copy(alpha = 0.14f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (verified) "PASS" else lab.targetChips.joinToString("/"),
                                        style = LogicLabsType.TechnicalXs,
                                        color = accent,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            Text(
                                text = lab.title,
                                style = LogicLabsType.TitleSm,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = lab.subtitle,
                                style = LogicLabsType.TechnicalXs,
                                color = TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                val expectedCols = if (isWide) labs.size else 2
                repeat((expectedCols - rowLabs.size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Footer() {
    Text(
        text = "LOGIC LABS v1.3.0 · ${AppPlatform.platformEditionLabel} EDITION · OFFLINE BENCH · NO ACCOUNTS · NO TRACKING",
        style = LogicLabsType.TechnicalXs,
        color = TextTertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.Space3),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PressableCard(
    modifier: Modifier = Modifier,
    accent: Color,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = Motion.TactilePress,
        label = "cardPress"
    )

    ChamferedPanel(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .desktopPointerHand()
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        Haptics.tick()
                        onClick()
                    }
                )
        ) {
            content()
        }
    }
}

@Composable
private fun Modifier.enterStagger(entered: Boolean, index: Int): Modifier = this

private fun formatBenchTime(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours <= 0) "${minutes}m" else "${hours}h ${com.logiclabs.core.bridge.platform.padZero2(minutes.toInt())}m"
}

@Composable
private fun ProjectCard(
    project: ProjectSummary,
    entered: Boolean,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (categoryLabel, categoryColor) = when {
        project.id == "_draft" -> "AUTOSAVED DRAFT" to AmberCore
        project.activeLabId != null -> "LAB ATTEMPT" to AccentCyan
        else -> "SAVED PROJECT" to PhosphorCore
    }
    val distinctChips = remember(project.chipSpots) {
        project.chipSpots.map { it.partNumber }.distinct()
    }

    PressableCard(
        modifier = modifier.enterStagger(entered, index),
        accent = categoryColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoardThumbnail(
                chips = project.chipSpots,
                wires = project.wireSpots,
                modifier = Modifier
                    .size(width = 104.dp, height = 76.dp)
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .background(SurfaceCard)
                    .border(Dimens.Hairline, categoryColor.copy(alpha = 0.35f), RoundedCornerShape(Dimens.RadiusSm))
            )
            Spacer(Modifier.width(Dimens.Space3))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.RadiusPill))
                        .background(categoryColor.copy(alpha = 0.12f))
                        .border(Dimens.Hairline, categoryColor.copy(alpha = 0.45f), RoundedCornerShape(Dimens.RadiusPill))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = categoryLabel,
                        style = LogicLabsType.TechnicalSm.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                        color = categoryColor,
                        maxLines = 1
                    )
                }
                Text(
                    text = project.title,
                    style = LogicLabsType.TitleSm,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.chipCount} ICs · ${project.wireCount} wires · ${relativeTime(project.updatedAtMillis)}",
                    style = LogicLabsType.TechnicalSm,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (distinctChips.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        distinctChips.take(3).forEach { chip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SurfaceRaised)
                                    .border(Dimens.Hairline, categoryColor.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = chip,
                                    style = LogicLabsType.TechnicalSm.copy(fontSize = 9.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                        if (distinctChips.size > 3) {
                            Text(
                                text = "+${distinctChips.size - 3}",
                                style = LogicLabsType.TechnicalSm.copy(fontSize = 9.sp),
                                color = TextTertiary
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(Dimens.MinTouchTarget)
                    .clip(RoundedCornerShape(Dimens.RadiusMd))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onDelete
                    )
                    .semantics { contentDescription = "Delete project ${project.title}" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LogicIcons.Trash,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ImportChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusPill))
            .background(AccentCyan.copy(alpha = 0.10f))
            .border(Dimens.Hairline, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(Dimens.RadiusPill))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Import a shared JSON project" }
            .padding(horizontal = Dimens.Space3, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space1)
    ) {
        Icon(
            imageVector = LogicIcons.Import,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "IMPORT JSON",
            style = LogicLabsType.TechnicalXs,
            color = AccentCyan,
            maxLines = 1
        )
    }
}

@Composable
private fun ImportCodeChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusPill))
            .background(AccentCyan.copy(alpha = 0.10f))
            .border(Dimens.Hairline, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(Dimens.RadiusPill))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Import a shared circuit code" }
            .padding(horizontal = Dimens.Space3, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space1)
    ) {
        Icon(
            imageVector = LogicIcons.Share,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "IMPORT CODE",
            style = LogicLabsType.TechnicalXs,
            color = AccentCyan,
            maxLines = 1
        )
    }
}

@Composable
private fun ImportCodeDialog(
    onDismiss: () -> Unit,
    onImplement: (String) -> Unit
) {
    var codeText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val extractedCode = remember(codeText) {
        val raw = codeText.trim()
        val idx = raw.indexOf(CircuitShareBytecode.PREFIX)
        if (idx >= 0) raw.substring(idx).takeWhile { !it.isWhitespace() && it != '&' && it != '#' } else raw
    }
    val validationResult = remember(extractedCode) {
        if (extractedCode.isEmpty()) null else CircuitShareBytecode.validate(extractedCode)
    }

    BenchDialog(
        onDismissRequest = onDismiss,
        icon = LogicIcons.Import,
        title = "Import circuit code",
        message = "Paste a shared “LOGIC-...” bytecode string to load onto the breadboard.",
        content = {
            Spacer(Modifier.height(Dimens.Space3))
            OutlinedTextField(
                value = codeText,
                onValueChange = { codeText = it },
                placeholder = {
                    Text("LOGIC-...", style = LogicLabsType.BodySm, color = TextTertiary)
                },
                trailingIcon = {
                    TextButton(
                        onClick = {
                            AppPlatform.readFromClipboardAsync { pasted ->
                                val t = pasted.orEmpty().trim()
                                if (t.isNotBlank()) {
                                    codeText = t
                                    Haptics.tick()
                                }
                            }
                        }
                    ) {
                        Text("PASTE", style = LogicLabsType.SwitchPlate, color = AccentCyan)
                    }
                },
                singleLine = true,
                textStyle = LogicLabsType.TechnicalSm.copy(color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentCyan,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = SurfaceCardBorder,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )

            Spacer(Modifier.height(Dimens.Space2))

            if (validationResult != null) {
                if (validationResult.isValid) {
                    Text(
                        text = "✓ Valid circuit: ${validationResult.chipCount} ICs · ${validationResult.wireCount} wires",
                        style = LogicLabsType.TechnicalSm,
                        color = PhosphorCore
                    )
                } else {
                    Text(
                        text = "✗ ${validationResult.errorMessage ?: "Invalid circuit code"}",
                        style = LogicLabsType.TechnicalSm,
                        color = ShortCircuitAlert
                    )
                }
            } else {
                Text(
                    text = "Requires valid LOGIC- binary bytecode format.",
                    style = LogicLabsType.TechnicalXs,
                    color = TextTertiary
                )
            }
        },
        actions = {
            BenchDialogAction(
                "IMPLEMENT",
                onClick = { onImplement(codeText.trim()) },
                enabled = validationResult?.isValid == true,
                leadingIcon = LogicIcons.Check,
                tone = BenchDialogTone.ACCENT
            )
            BenchDialogAction("CANCEL", onDismiss, tone = BenchDialogTone.NEUTRAL)
        }
    )
}

private fun relativeTime(millis: Long): String {
    val delta = com.logiclabs.core.bridge.platform.currentTimeMillis() - millis
    return when {
        delta < 60_000L -> "just now"
        delta < 3_600_000L -> "${delta / 60_000L}m ago"
        delta < 86_400_000L -> "${delta / 3_600_000L}h ago"
        else -> "${delta / 86_400_000L}d ago"
    }
}
