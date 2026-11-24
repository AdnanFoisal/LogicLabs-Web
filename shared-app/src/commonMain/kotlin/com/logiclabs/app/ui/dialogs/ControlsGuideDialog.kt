package com.logiclabs.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

private val MouseControls = listOf(
    ControlRow("Scroll Wheel", "Zoom in / out smoothly toward mouse pointer"),
    ControlRow("Middle / Space+Drag", "Pan the breadboard in any mode"),
    ControlRow("Left-Drag Board", "Pan the breadboard in Roam / Select mode"),
    ControlRow("Click / Drag (Wire)", "Click hole A then hole B, or click-drag to connect"),
    ControlRow("Right-Click", "Context menu on chip/wire/board — or cancel wire"),
    ControlRow("Drag IC / Endpoint", "Move placed IC across columns, or re-terminate wire"),
    ControlRow("Double-Click IC", "Open live IC Pinout & Truth Table Inspector"),
    ControlRow("Hold Alt (or L)", "Show 2.2x optical magnifying loupe at cursor")
)

private val BreadboardHotkeys = listOf(
    ControlRow("W / V / S", "Switch Wire Draw / Roam / Wire Select modes"),
    ControlRow("Esc", "Cancel armed wire, clear selection, or close dialog"),
    ControlRow("R", "Rotate selected IC 180°"),
    ControlRow("M", "Toggle Catenary / Manhattan routing on wire"),
    ControlRow("Arrow Keys", "Nudge selected IC (or pan board when idle)"),
    ControlRow("Delete / Backspace", "Delete selected wire or IC"),
    ControlRow("Ctrl + Z / Ctrl + Y", "Multi-step Undo / Redo"),
    ControlRow("F  or  0", "Fit circuit / breadboard to screen"),
    ControlRow("+  /  -  /  L", "Zoom in / Zoom out / Toggle 2.2x loupe")
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
    ControlRow("C", "Open 74xx IC Chip Catalog (type, ↑/↓ + Enter)"),
    ControlRow("B", "Open Guided Lab Coursework Catalog (49 builds)"),
    ControlRow("O", "Open Dual-Channel CRT Oscilloscope"),
    ControlRow("D", "Toggle Live Gate-Level Boolean Schematic"),
    ControlRow("F5", "Run Exhaustive 2^N Truth-Table Verification"),
    ControlRow("Ctrl + S / O / E", "Save / Import / Export Project (.json)"),
    ControlRow("Ctrl + 1 .. 4", "Switch Home tabs (Projects, Labs, ICs, Settings)"),
    ControlRow("PgUp / PgDn", "Scroll pages, catalogs, settings, and console"),
    ControlRow("F1  or  ?", "Open this Controls & Shortcuts Guide")
)

/**
 * Dedicated Controls & Keyboard Shortcuts Reference modal, reachable via F1, ?,
 * the top toolbar '?' button, the Help menu, and Settings.
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
            title = "CONTROLS & KEYBOARD SHORTCUTS",
            subtitle = "mouse pointer, breadboard CAD hotkeys, trainer console & keyboard navigation",
            titleIcon = LogicIcons.Info,
            onDismiss = onDismiss
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .keyboardPageScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(end = Dimens.Space3),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
                    ) {
                        ControlSectionCard(
                            title = "MOUSE POINTER",
                            accent = AccentCyan,
                            rows = MouseControls,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        ControlSectionCard(
                            title = "BREADBOARD HOTKEYS",
                            accent = PhosphorCore,
                            rows = BreadboardHotkeys,
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
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        ControlSectionCard(
                            title = "TOOLS, FILES & KEYBOARD SELECTION",
                            accent = AccentCyan,
                            rows = WorkspaceHotkeys,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }

                    Text(
                        text = "Tip: Press C to open the Chip Catalog, type a part like '7408', use Up/Down arrows, and press Enter to place it immediately.",
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(SurfaceCard)
            .border(Dimens.Hairline, SurfaceCardBorder, RoundedCornerShape(Dimens.RadiusMd))
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalArrangement = Arrangement.spacedBy(5.dp)
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
                        .widthIn(min = 128.dp)
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
