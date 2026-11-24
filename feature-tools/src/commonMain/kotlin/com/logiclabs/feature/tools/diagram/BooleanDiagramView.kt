package com.logiclabs.feature.tools.diagram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.designsystem.component.ChamferedPanel
import com.logiclabs.core.designsystem.component.DesktopVerticalScrollbar
import com.logiclabs.core.designsystem.component.LogicIcons
import com.logiclabs.core.designsystem.component.TelemetryPill
import com.logiclabs.core.designsystem.component.webNativeZoomTarget
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.CanvasVignette
import com.logiclabs.core.designsystem.theme.ChassisBevelHighlight
import com.logiclabs.core.designsystem.theme.ChassisDivider
import com.logiclabs.core.designsystem.theme.ChassisSilkscreen
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.FloatingWarning
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.PhosphorCore
import com.logiclabs.core.designsystem.theme.ShortCircuitAlert
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextSecondary
import com.logiclabs.core.designsystem.theme.TextTertiary
import com.logiclabs.feature.tools.feedback.Haptics
import kotlinx.coroutines.delay

/**
 * Interactive boolean / gate-level view of the circuit built on the breadboard.
 *
 * Derives the equivalent clean schematic straight from the physical netlist:
 * input terminal tags (SW0…, CLK, +5V) on the left, the placed ICs' internal
 * gate units as ANSI/IEEE distinctive-shape symbols (and MSI/sequential parts
 * like the 7483/7448/7474/7476 as labelled rectangular blocks) in the middle
 * columns, and LED / 7-segment output tags on the right. Nets are coloured
 * live from the engine: cyan = HIGH, dim = LOW, amber = floating, red =
 * contention; feedback paths are dashed. Per-output boolean expressions are
 * derived and listed under the canvas (tap a line to copy it).
 *
 * Gestures: one-finger drag pans, pinch zooms (centroid-anchored), double-tap
 * refits. The whole diagram is pre-built off the draw path — the render loop
 * performs zero allocations (project 60fps rule).
 *
 * ## Wiring for integration (bench swap or overlay)
 *
 * `BreadboardCircuit` is a plain class the snapshot system cannot observe, so
 * the view takes two version stamps — the same contract `BreadboardCanvas`
 * uses:
 *
 * ```kotlin
 * BooleanDiagramView(
 *     circuit = circuit,
 *     modifier = Modifier.fillMaxSize(),     // full-screen swap
 *     circuitVersion = bench.topologyVersion, // chips/wires changed → re-derive
 *     renderVersion = bench.renderVersion     // switches/LEDs/clock changed → repaint
 * )
 * ```
 *
 * - [circuitVersion] rebuilds the derivation and layout (remember key). It
 *   should be the workbench's topology version, bumped after any add/remove/
 * move of chips or wires.
 * - [renderVersion] only invalidates the draw so live net levels, switch tags
 *   and LED tags refresh; pass the workbench's render version (bumped after
 *   every `circuit.step()` sync). Leave at the default when the host already
 *   recomposes this call site on every step.
 *
 * When the circuit is empty the view renders a friendly empty state instead
 * of a blank canvas.
 *
 * @param circuit the live breadboard circuit to derive from (read-only).
 * @param modifier sizing; use `fillMaxSize()` for a full-screen swap or a
 *   bounded height inside a sheet.
 * @param circuitVersion topology stamp — 0 when the host never changes the
 *   circuit while this view is shown (e.g. a modal sheet).
 * @param renderVersion repaint stamp for live levels.
 */
@Composable
fun BooleanDiagramView(
    circuit: BreadboardCircuit,
    modifier: Modifier = Modifier,
    circuitVersion: Int = 0,
    renderVersion: Int = 0,
    zoomCaptureEnabled: Boolean = true
) {
    val density = LocalDensity.current.density
    val clipboard = LocalClipboardManager.current

    val graph = remember(circuit, circuitVersion) { LogicGraphBuilder.build(circuit) }
    val layout = remember(graph) { DiagramLayout.build(graph) }
    val scratch = remember { DiagramScratch() }

    var scale by remember { mutableFloatStateOf(density) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var canvasW by remember { mutableIntStateOf(0) }
    var canvasH by remember { mutableIntStateOf(0) }
    var fittedFor by remember { mutableStateOf<Any?>(null) }
    var copiedKey by remember { mutableStateOf<String?>(null) }

    val minZoom = 0.35f * density
    val maxZoom = 3.5f * density

    fun fit() {
        val w = canvasW.toFloat()
        val h = canvasH.toFloat()
        if (w <= 0f || h <= 0f || layout.worldW <= 0f || layout.worldH <= 0f) return
        val z = (minOf(w / layout.worldW, h / layout.worldH) * 0.94f).coerceIn(minZoom, maxZoom)
        scale = z
        panX = (w - layout.worldW * z) / 2f
        panY = (h - layout.worldH * z) / 2f
    }

    fun clampPan() {
        val w = canvasW.toFloat()
        val h = canvasH.toFloat()
        if (w <= 0f || h <= 0f) return
        val cw = layout.worldW * scale
        val ch = layout.worldH * scale
        val slack = 96f * density
        panX = if (cw <= w) (w - cw) / 2f else panX.coerceIn(w - cw - slack, slack)
        panY = if (ch <= h) (h - ch) / 2f else panY.coerceIn(h - ch - slack, slack)
    }

    fun applyZoomAround(factor: Float, pivotX: Float, pivotY: Float, dx: Float = 0f, dy: Float = 0f) {
        val oldZ = scale
        if (oldZ <= 0f) return
        val newZ = (oldZ * factor).coerceIn(minZoom, maxZoom)
        val k = newZ / oldZ
        panX = pivotX - (pivotX - panX) * k + dx
        panY = pivotY - (pivotY - panY) * k + dy
        scale = newZ
        clampPan()
    }

    LaunchedEffect(layout, canvasW, canvasH) {
        if (canvasW > 0 && canvasH > 0 && fittedFor != layout) {
            fit()
            fittedFor = layout
        }
    }

    LaunchedEffect(copiedKey) {
        if (copiedKey != null) {
            delay(1400)
            copiedKey = null
        }
    }

    Column(modifier = modifier) {
        DiagramHeader(
            stats = layout.stats,
            zoomDisplay = scale / density,
            canZoomOut = scale > minZoom + 0.01f,
            canZoomIn = scale < maxZoom - 0.01f,
            onZoomOut = {
                Haptics.tick()
                applyZoomAround(1f / 1.25f, canvasW / 2f, canvasH / 2f)
            },
            onZoomIn = {
                Haptics.tick()
                applyZoomAround(1.25f, canvasW / 2f, canvasH / 2f)
            },
            onFit = {
                Haptics.tick()
                fit()
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2)
        ) {
            if (layout.isEmpty) {
                EmptyDiagram(Modifier.fillMaxSize())
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(Dimens.RadiusLg))
                        .background(CanvasVignette)
                        .border(Dimens.Hairline, ChassisBevelHighlight, RoundedCornerShape(Dimens.RadiusLg))
                        .onSizeChanged {
                            canvasW = it.width
                            canvasH = it.height
                        }
                        .webNativeZoomTarget(enabled = zoomCaptureEnabled) { zoom, dx, dy, lx, ly ->
                            applyZoomAround(zoom, lx, ly, dx, dy)
                        }
                        .pointerInput(layout, density) {
                            awaitPointerEventScope {
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    if (ev.type == androidx.compose.ui.input.pointer.PointerEventType.Scroll) {
                                        val ch = ev.changes.firstOrNull()
                                        val dy = ch?.scrollDelta?.y ?: 0f
                                        if (ch != null && dy != 0f) {
                                            val factor = if (dy < 0f) 1.15f else 1f / 1.15f
                                            applyZoomAround(factor, ch.position.x, ch.position.y)
                                            ch.consume()
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput(layout, density) {
                            detectTransformGestures { centroid, pan, zoomChange, _ ->
                                applyZoomAround(zoomChange, centroid.x, centroid.y, pan.x, pan.y)
                            }
                        }
                        .pointerInput(layout) {
                            detectTapGestures(onDoubleTap = { fit() })
                        }
                ) {
                    @Suppress("UNUSED_EXPRESSION") circuitVersion
                    @Suppress("UNUSED_EXPRESSION") renderVersion
                    drawDiagram(
                        layout = layout,
                        circuit = circuit,
                        zoom = scale,
                        panX = panX,
                        panY = panY,
                        userZoom = scale / density,
                        scratch = scratch
                    )
                }

                GestureHint(
                    Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        LegendPanel(
            layout = layout,
            copiedKey = copiedKey,
            onCopy = { key, text ->
                clipboard.setText(AnnotatedString(text))
                copiedKey = key
                Haptics.tick()
            }
        )
    }
}

@Composable
private fun DiagramHeader(
    stats: DiagramStats,
    zoomDisplay: Float,
    canZoomOut: Boolean,
    canZoomIn: Boolean,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onFit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space1),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TelemetryPill(label = "ICS", value = stats.chipCount.toString(), accent = AccentCyan)
        TelemetryPill(label = "UNITS", value = (stats.gateCount + stats.blockCount).toString(), accent = PhosphorCore)
        TelemetryPill(label = "NETS", value = stats.netCount.toString(), accent = ChassisSilkscreen)
        Spacer(Modifier.weight(1f))

        DiagramZoomButton(label = "−", enabled = canZoomOut, onClick = onZoomOut)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.RadiusPill))
                .background(SurfaceCard)
                .border(Dimens.Hairline, ChassisDivider, RoundedCornerShape(Dimens.RadiusPill))
                .clickable(onClick = onFit)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${kotlin.math.round(zoomDisplay * 100f).toInt()}%",
                style = LogicLabsType.TechnicalXs,
                color = AccentCyan,
                maxLines = 1
            )
        }
        DiagramZoomButton(label = "+", enabled = canZoomIn, onClick = onZoomIn)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.RadiusPill))
                .background(AccentCyan.copy(alpha = 0.14f))
                .border(Dimens.Hairline, AccentCyan.copy(alpha = 0.55f), RoundedCornerShape(Dimens.RadiusPill))
                .clickable(onClick = onFit)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "FIT",
                style = LogicLabsType.TechnicalXs,
                color = AccentCyan,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DiagramZoomButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(Dimens.RadiusPill))
            .background(if (enabled) SurfaceCard else SurfaceCard.copy(alpha = 0.4f))
            .border(
                Dimens.Hairline,
                if (enabled) AccentCyan.copy(alpha = 0.5f) else ChassisDivider,
                RoundedCornerShape(Dimens.RadiusPill)
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = LogicLabsType.TitleSm,
            color = if (enabled) AccentCyan else TextTertiary.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun GestureHint(modifier: Modifier = Modifier) {
    Text(
        text = "DRAG TO PAN · PINCH OR +/- TO ZOOM · DOUBLE-TAP TO FIT",
        style = LogicLabsType.SwitchPlate,
        color = TextTertiary,
        modifier = modifier.padding(bottom = Dimens.Space2)
    )
}

@Composable
private fun EmptyDiagram(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = LogicIcons.Chip,
            contentDescription = null,
            tint = ChassisDivider,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(Dimens.Space3))
        Text(
            text = "NO LOGIC WIRED",
            style = LogicLabsType.SwitchPlateLg,
            color = ChassisSilkscreen
        )
        Spacer(Modifier.height(Dimens.Space1))
        Text(
            text = "Place ICs on the breadboard and jumper them —\nthe equivalent gate-level schematic appears here.",
            style = LogicLabsType.BodySm,
            color = TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LegendPanel(
    layout: LayoutResult,
    copiedKey: String?,
    onCopy: (String, String) -> Unit
) {
    if (layout.isEmpty) return
    ChamferedPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space3)
            .padding(bottom = Dimens.Space3),
        fill = SurfaceCard
    ) {
        val exprScroll = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 208.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(exprScroll)
                    .padding(start = Dimens.Space3, end = Dimens.Space5, top = Dimens.Space3, bottom = Dimens.Space3)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DERIVED BOOLEAN EXPRESSIONS",
                        style = LogicLabsType.SwitchPlateLg,
                        color = ChassisSilkscreen
                    )
                    LevelKeys()
                }

                if (layout.expressions.isNotEmpty()) {
                    Spacer(Modifier.height(Dimens.Space2))
                    for (line in layout.expressions) {
                        ExpressionRow(
                            line = line,
                            isCopied = copiedKey == line.outputLabel,
                            onCopy = onCopy
                        )
                    }
                } else {
                    Spacer(Modifier.height(Dimens.Space2))
                    Text(
                        text = "No outputs wired — expressions appear once an LED or 7-segment input is driven.",
                        style = LogicLabsType.BodySm,
                        color = TextTertiary
                    )
                }

                if (layout.notes.isNotEmpty()) {
                    Spacer(Modifier.height(Dimens.Space2))
                    for (note in layout.notes) {
                        Text(
                            text = "▸ $note",
                            style = LogicLabsType.TechnicalSm,
                            color = TextTertiary
                        )
                    }
                }
            }
            DesktopVerticalScrollbar(
                scrollState = exprScroll,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = Dimens.Space2, horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun ExpressionRow(
    line: ExpressionLine,
    isCopied: Boolean,
    onCopy: (String, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .clickable { onCopy(line.outputLabel, "${line.outputLabel} = ${line.expression}") }
            .padding(vertical = Dimens.Space1, horizontal = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = line.outputLabel,
            style = LogicLabsType.TechnicalMd,
            color = PhosphorCore
        )
        Text(
            text = " = ",
            style = LogicLabsType.TechnicalMd,
            color = TextSecondary
        )
        Text(
            text = line.expression,
            style = LogicLabsType.TechnicalMd,
            color = if (line.isConflict) ShortCircuitAlert else TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(Dimens.Space2))
        Icon(
            imageVector = if (isCopied) LogicIcons.Check else LogicIcons.Copy,
            contentDescription = if (isCopied) "Copied" else "Copy expression",
            tint = if (isCopied) PhosphorCore else TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun LevelKeys() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KeySwatch(color = AccentCyan, label = "HIGH", dashed = false)
        KeySwatch(color = LowNetColor, label = "LOW", dashed = false)
        KeySwatch(color = FloatingWarning, label = "FLOAT", dashed = false)
        KeySwatch(color = ShortCircuitAlert, label = "CONFLICT", dashed = false)
        KeySwatch(color = LowNetColor, label = "FEEDBACK", dashed = true)
    }
}

/** Pre-built dash effect for the legend key swatches (kept off the draw path). */
private val LegendDashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))

@Composable
private fun KeySwatch(color: Color, label: String, dashed: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(width = 16.dp, height = 6.dp)) {
            if (dashed) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 2.4f,
                    pathEffect = LegendDashEffect
                )
            } else {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 2.4f
                )
            }
        }
        Text(
            text = label,
            style = LogicLabsType.TechnicalXs,
            color = TextTertiary
        )
    }
}
