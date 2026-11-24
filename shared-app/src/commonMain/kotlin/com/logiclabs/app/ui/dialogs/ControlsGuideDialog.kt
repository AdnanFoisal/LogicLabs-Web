package com.logiclabs.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.logiclabs.core.designsystem.component.DesktopModalDialog
import com.logiclabs.core.designsystem.component.DesktopVerticalScrollbar
import com.logiclabs.core.designsystem.component.LogicIcons
import com.logiclabs.core.designsystem.component.SheetScaffold
import com.logiclabs.core.designsystem.component.keyboardPageScroll
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.AmberCore
import com.logiclabs.core.designsystem.theme.ChassisDivider
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.PhosphorCore
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.SurfaceCardBorder
import com.logiclabs.core.designsystem.theme.SurfaceRaised
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextSecondary

private data class ControlRow(val keys: String, val action: String)

private val PointerAndTouchControls = listOf(
    ControlRow("Pinch / Scroll", "Two-finger pinch (phone/trackpad) or mouse wheel zooms smoothly"),
    ControlRow("Double-Tap Empty", "Toggle 2.2x inspection zoom <-> 100% Fit at tap location"),
    ControlRow("Middle / Space+Drag", "Pan the breadboard in any mode (or 1-finger drag in Roam)"),
    ControlRow("Click / Tap (Wire)", "Tap hole A then hole B, or drag hole-to-hole to route jumper"),
    ControlRow("Right-Click", "Context menu on chip/wire/board — or cancel armed wire"),
    ControlRow("Drag IC / Endpoint", "Move placed IC across columns, or re-terminate wire"),
    ControlRow("Double-Click/Tap IC", "Open live IC Pinout & Truth Table Inspector"),
    ControlRow("Hold Alt (or L)", "Show 2.2x optical magnifying loupe at pointer")
)

private val BreadboardHotkeys = listOf(
    ControlRow("W / V / S", "Switch Wire Draw / Roam / Wire Select modes"),
    ControlRow("Esc", "Cancel armed wire, clear selection, or close dialog"),
    ControlRow("R", "Rotate selected IC 180°"),
    ControlRow("M", "Toggle Catenary / Manhattan routing on wire"),
    ControlRow("Arrow Keys", "Nudge selected IC (or pan board when idle)"),
    ControlRow("Delete / Backspace", "Delete selected wire or IC"),
    ControlRow("Ctrl + Z / Ctrl + Y", "Multi-step Undo / Redo"),
    ControlRow("F  or  0", "Fit circuit / breadboard to screen (also Ctrl+0)"),
    ControlRow("+ / - (or Ctrl +/-)", "Zoom in / Zoom out · L toggles 2.2x loupe")
)

private val ConsoleHotkeys = listOf(
    ControlRow("1 .. 8", "Toggle Data Switches SW0 – SW7"),
    ControlRow("Q  /  E", "Momentary Pulser A / Pulser B (hold & release)"),
    ControlRow("P", "Toggle Master +5V Power Supply"),
    ControlRow("K", "Start / Pause Stepped Clock Generator"),
    ControlRow(".", "Single-Step Clock one half-cycle"),
    ControlRow("Shift + Wheel", "Step Clock frequency on RATE knob (or click - / +)")
)

private val WorkspaceHotkeys = listOf(
    ControlRow("C  /  B", "Open 74xx IC Catalog [C] or 49 Guided Labs [B]"),
    ControlRow("O  /  D", "Dual-Channel CRT Oscilloscope [O] · Live Schematic [D]"),
    ControlRow("F5", "Run Exhaustive 2^N Truth-Table Verification"),
    ControlRow("Ctrl + S / E", "Quick-save project / Export .logiclabs.json"),
    ControlRow("Ctrl + Shift + P", "Download High-DPI Breadboard PNG Snapshot"),
    ControlRow("Drag & Drop .json", "Drop any .logiclabs.json file onto browser to open"),
    ControlRow("F11", "Toggle edge-to-edge Fullscreen Workstation mode"),
    ControlRow("Ctrl + 1 .. 4", "Switch Home tabs (Projects, Labs, ICs, Settings)"),
    ControlRow("F1  or  ?", "Open this Controls & Shortcuts Guide")
)

/**
 * Responsive Controls, Touch Gestures & Keyboard Shortcuts Reference modal (`F1` / `?`).
 * Renders a 2-column grid on desktop/tablets (`>= 720dp`) and a clean single-column stack on phones.
 */
@Composable
fun ControlsGuideDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()

    DesktopModalDialog(
        onDismissRequest = onDismiss,
        maxWidth = 980.dp,
        maxHeight = 720.dp
    ) {
        SheetScaffold(
            title = "CONTROLS, TOUCH & KEYBOARD SHORTCUTS",
            subtitle = "pinch & wheel zoom, breadboard CAD gestures, trainer console & workspace hotkeys",
            titleIcon = LogicIcons.Info,
            onDismiss = onDismiss
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .keyboardPageScroll(scrollState)
            ) {
                val twoColumns = maxWidth >= 680.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(end = Dimens.Space3),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    if (twoColumns) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
                        ) {
                            ControlSectionCard(
                                title = "TOUCH, TRACKPAD & MOUSE",
                                accent = AccentCyan,
                                rows = PointerAndTouchControls,
                                compact = false,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            ControlSectionCard(
                                title = "BREADBOARD CAD HOTKEYS",
                                accent = PhosphorCore,
                                rows = BreadboardHotkeys,
                                compact = false,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
                        ) {
                            ControlSectionCard(
                                title = "TRAINER CONSOLE KEYS",
                                accent = AmberCore,
                                rows = ConsoleHotkeys,
                                compact = false,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            ControlSectionCard(
                                title = "INSTRUMENTS, EXPORT & WEB",
                                accent = AccentCyan,
                                rows = WorkspaceHotkeys,
                                compact = false,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    } else {
                        ControlSectionCard(
                            title = "TOUCH, TRACKPAD & MOUSE",
                            accent = AccentCyan,
                            rows = PointerAndTouchControls,
                            compact = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ControlSectionCard(
                            title = "TRAINER CONSOLE KEYS",
                            accent = AmberCore,
                            rows = ConsoleHotkeys,
                            compact = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ControlSectionCard(
                            title = "BREADBOARD CAD HOTKEYS",
                            accent = PhosphorCore,
                            rows = BreadboardHotkeys,
                            compact = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ControlSectionCard(
                            title = "INSTRUMENTS, EXPORT & WEB",
                            accent = AccentCyan,
                            rows = WorkspaceHotkeys,
                            compact = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = "Tip: Pinch two fingers on phones/trackpads or scroll your mouse wheel anywhere on the breadboard to zoom smoothly, or double-tap empty board space to toggle 2.2x closeup!",
                        style = LogicLabsType.BodySm,
                        color = TextSecondary
                    )
                }

                DesktopVerticalScrollbar(
                    scrollState = scrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun ControlSectionCard(
    title: String,
    accent: androidx.compose.ui.graphics.Color,
    rows: List<ControlRow>,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(SurfaceCard)
            .border(Dimens.Hairline, SurfaceCardBorder, RoundedCornerShape(Dimens.RadiusMd))
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = LogicLabsType.SwitchPlateLg,
            color = accent
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.Hairline)
                .background(ChassisDivider)
        )
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = if (compact) 108.dp else 132.dp)
                        .clip(RoundedCornerShape(Dimens.RadiusSm))
                        .background(SurfaceRaised)
                        .border(Dimens.Hairline, accent.copy(alpha = 0.45f), RoundedCornerShape(Dimens.RadiusSm))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = row.keys,
                        style = LogicLabsType.TechnicalXs,
                        color = accent,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(Dimens.Space2))
                Text(
                    text = row.action,
                    style = LogicLabsType.BodySm,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
