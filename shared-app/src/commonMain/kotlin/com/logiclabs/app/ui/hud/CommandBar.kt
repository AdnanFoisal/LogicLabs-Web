package com.logiclabs.app.ui.hud

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logiclabs.core.bridge.model.SimulationMode
import com.logiclabs.core.designsystem.component.DesktopTooltip
import com.logiclabs.core.designsystem.component.GlassSurface
import com.logiclabs.core.designsystem.component.LogicIcons
import com.logiclabs.core.designsystem.component.desktopPointerHand
import com.logiclabs.core.designsystem.component.horizontalWheelScroll
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.AmberCore
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.Motion
import com.logiclabs.core.designsystem.theme.PhosphorCore
import com.logiclabs.core.designsystem.theme.ShortCircuitAlert
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.SurfaceCardBorder
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextSecondary
import com.logiclabs.core.designsystem.theme.TextTertiary

/**
 * Floating top command bar spanning the bench pane.
 *
 * Splits cleanly into a left identity/telemetry group and a right-aligned desktop action group
 * (`Undo`, `Redo`, `Save`, `Share`, `Verify`, `?`) with native hover tooltips and zero idle
 * 60 FPS animations.
 */
@Composable
fun CommandBar(
    chipCount: Int,
    wireCount: Int,
    masterPower: Boolean,
    verificationPassed: Boolean?,
    onVerifyClick: () -> Unit,
    modifier: Modifier = Modifier,
    projectTitle: String? = null,
    onSaveClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    simulationMode: SimulationMode = SimulationMode.IDEAL,
    onToggleSimulationMode: (() -> Unit)? = null,
    hasBurnedChips: Boolean = false,
    onRestoreChips: (() -> Unit)? = null,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndoClick: (() -> Unit)? = null,
    onRedoClick: (() -> Unit)? = null,
    onControlsGuideClick: (() -> Unit)? = null,
    consoleCollapsed: Boolean = false,
    onToggleConsoleClick: (() -> Unit)? = null
) {
    val barScroll = rememberScrollState()
    GlassSurface(
        modifier = modifier
            .padding(horizontal = Dimens.CommandBarInset, vertical = Dimens.Space1)
            .fillMaxWidth()
            .heightIn(min = Dimens.CommandBarHeight),
        shape = RoundedCornerShape(Dimens.RadiusPill)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 600.dp
            val minRowWidth = maxWidth
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalWheelScroll(barScroll)
                    .horizontalScroll(barScroll)
                    .widthIn(min = minRowWidth)
                    .padding(
                        start = if (onBackClick != null) (if (isCompact) 6.dp else Dimens.Space2) else Dimens.Space4,
                        end = if (isCompact) 6.dp else Dimens.Space2
                    )
                    .heightIn(min = Dimens.CommandBarHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    DesktopTooltip(text = "Return to Home Hub") {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 30.dp else 32.dp)
                                .clip(CircleShape)
                                .desktopPointerHand()
                                .clickable(onClick = onBackClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = LogicIcons.Back,
                                contentDescription = "Leave bench",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(if (isCompact) 2.dp else Dimens.Space1))
                }

                Wordmark(
                    machineLine = if (projectTitle != null) projectTitle.uppercase() else "IDL-800A",
                    modifier = Modifier.widthIn(max = if (isCompact) 86.dp else 156.dp)
                )

                Spacer(Modifier.width(if (isCompact) 6.dp else Dimens.Space2))

                if (onToggleSimulationMode != null) {
                    DesktopTooltip(text = "Switch Ideal / Practical IC physics") {
                        SimulationModePill(mode = simulationMode, onClick = onToggleSimulationMode)
                    }
                }

                if (hasBurnedChips && onRestoreChips != null) {
                    Spacer(Modifier.width(Dimens.Space1))
                    DesktopTooltip(text = "Replace destroyed ICs") {
                        RestoreIcPill(onClick = onRestoreChips)
                    }
                }

                if (!isCompact) {
                    Spacer(Modifier.width(Dimens.Space2))

                    // Telemetry readouts (status bar below already displays IC/wire count on mobile)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space1),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelemetryPill(label = "IC", value = chipCount.toString())
                        TelemetryPill(label = "WIRE", value = wireCount.toString())
                        RailPill(live = masterPower)
                    }
                }

                Spacer(Modifier.weight(1f).widthIn(min = if (isCompact) 4.dp else Dimens.Space2))

                // Right-aligned desktop action group
                if (!isCompact && onUndoClick != null && onRedoClick != null) {
                    DesktopTooltip(text = "Undo last board change", shortcut = "Ctrl+Z") {
                        DesktopActionPill(
                            label = "UNDO",
                            enabled = canUndo,
                            accent = AccentCyan,
                            onClick = onUndoClick
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    DesktopTooltip(text = "Redo board change", shortcut = "Ctrl+Y") {
                        DesktopActionPill(
                            label = "REDO",
                            enabled = canRedo,
                            accent = AccentCyan,
                            onClick = onRedoClick
                        )
                    }
                } else if (isCompact && onUndoClick != null && canUndo) {
                    DesktopActionPill(
                        label = "UNDO",
                        enabled = true,
                        accent = AccentCyan,
                        onClick = onUndoClick
                    )
                    Spacer(Modifier.width(4.dp))
                }

                if (!isCompact && consoleCollapsed && onToggleConsoleClick != null) {
                    Spacer(Modifier.width(Dimens.Space2))
                    DesktopTooltip(text = "Restore right-hand IDL-800A console") {
                        DesktopActionPill(
                            label = "SHOW CONSOLE",
                            enabled = true,
                            accent = AmberCore,
                            onClick = onToggleConsoleClick
                        )
                    }
                }

                if (!isCompact) {
                    Spacer(Modifier.width(Dimens.Space2))
                }

                if (onSaveClick != null) {
                    SaveAction(title = if (isCompact) null else projectTitle, onClick = onSaveClick)
                    Spacer(Modifier.width(if (isCompact) 2.dp else Dimens.Space1))
                }

                if (onShareClick != null) {
                    ShareAction(onClick = onShareClick)
                    Spacer(Modifier.width(if (isCompact) 2.dp else Dimens.Space1))
                }

                VerifyAction(passed = verificationPassed, onClick = onVerifyClick)

                if (onControlsGuideClick != null) {
                    Spacer(Modifier.width(if (isCompact) 2.dp else Dimens.Space1))
                    ControlsHelpButton(onClick = onControlsGuideClick)
                }
            }
        }
    }
}

@Composable
private fun DesktopActionPill(
    label: String,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.48f)
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .background(if (enabled) accent.copy(alpha = 0.12f) else SurfaceCard)
            .border(
                Dimens.Hairline,
                if (enabled) accent.copy(alpha = 0.55f) else SurfaceCardBorder,
                RoundedCornerShape(Dimens.RadiusSm)
            )
            .then(if (enabled) Modifier.desktopPointerHand() else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = LogicLabsType.TechnicalXs,
            color = if (enabled) accent else TextSecondary,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ControlsHelpButton(onClick: () -> Unit) {
    DesktopTooltip(text = "Controls & Keyboard Shortcuts", shortcut = "F1") {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(AmberCore.copy(alpha = 0.14f))
                .border(Dimens.Hairline, AmberCore.copy(alpha = 0.65f), CircleShape)
                .desktopPointerHand()
                .clickable(onClick = onClick)
                .semantics { contentDescription = "Controls and keyboard shortcuts guide (F1)" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "?",
                style = LogicLabsType.SwitchPlateLg,
                color = AmberCore
            )
        }
    }
}

@Composable
private fun Wordmark(machineLine: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "LOGIC LABS",
            style = LogicLabsType.SwitchPlateLg,
            color = TextPrimary,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = machineLine,
            style = LogicLabsType.TechnicalXs,
            color = TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )
    }
}

@Composable
private fun SaveAction(title: String?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = LogicLabsType.TechnicalSm,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 96.dp)
            )
        }
        DesktopTooltip(text = "Save Project", shortcut = "Ctrl+S") {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .desktopPointerHand()
                    .clickable(onClick = onClick)
                    .semantics { contentDescription = "Save project" },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.14f))
                        .border(Dimens.Hairline, AccentCyan.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = LogicIcons.Save,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareAction(onClick: () -> Unit) {
    DesktopTooltip(text = "Share Circuit Bytecode / JSON") {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .desktopPointerHand()
                .clickable(onClick = onClick)
                .semantics { contentDescription = "Share circuit code" },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AccentCyan.copy(alpha = 0.14f))
                    .border(Dimens.Hairline, AccentCyan.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LogicIcons.Share,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TelemetryPill(
    label: String,
    value: String,
    accent: Color = TextSecondary
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .background(SurfaceCard.copy(alpha = 0.72f))
            .border(
                Dimens.Hairline,
                SurfaceCardBorder,
                RoundedCornerShape(Dimens.RadiusSm)
            )
            .padding(horizontal = Dimens.Space2, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = LogicLabsType.TechnicalXs, color = TextTertiary, softWrap = false)
        Spacer(Modifier.width(4.dp))
        Text(text = value, style = LogicLabsType.TechnicalSm, color = accent, softWrap = false)
    }
}

@Composable
private fun RailPill(live: Boolean) {
    val target = if (live) PhosphorCore else ShortCircuitAlert
    val railColor by animateColorAsState(
        targetValue = target,
        animationSpec = Motion.VoltageFade,
        label = "railVoltageColor"
    )

    Row(
        modifier = Modifier
            .widthIn(min = 54.dp)
            .clip(RoundedCornerShape(Dimens.RadiusPill))
            .background(SurfaceCard)
            .border(Dimens.Hairline, SurfaceCardBorder, RoundedCornerShape(Dimens.RadiusPill))
            .padding(horizontal = Dimens.Space2, vertical = 3.dp)
            .semantics {
                contentDescription =
                    if (live) "Supply rail live, 5 volts" else "Supply rail off, 0 volts"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(railColor)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = if (live) "+5.0V" else "0.0V",
            style = LogicLabsType.TechnicalSm,
            color = railColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Verification action with static high-contrast state ring (zero idle 60 FPS animation overhead).
 */
@Composable
private fun VerifyAction(passed: Boolean?, onClick: () -> Unit) {
    val accent = when (passed) {
        true -> PhosphorCore
        false -> ShortCircuitAlert
        null -> AccentCyan
    }

    val tooltipText = when (passed) {
        true -> "Truth-Table Verified (Pass) — Re-run"
        false -> "Verification Failed — Inspect"
        null -> "Run 2^N Truth-Table Verification"
    }

    DesktopTooltip(text = tooltipText, shortcut = "F5") {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .desktopPointerHand()
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = when (passed) {
                        true -> "Verify circuit. Last run passed."
                        false -> "Verify circuit. Last run failed."
                        null -> "Verify circuit. No result yet."
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.72f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LogicIcons.Verify,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SimulationModePill(
    mode: SimulationMode,
    onClick: () -> Unit
) {
    val isPractical = mode == SimulationMode.PRACTICAL
    val accent = if (isPractical) AmberCore else AccentCyan
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .background(accent.copy(alpha = 0.15f))
            .border(
                Dimens.Hairline,
                accent.copy(alpha = 0.6f),
                RoundedCornerShape(Dimens.RadiusSm)
            )
            .desktopPointerHand()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space2, vertical = 3.dp)
            .semantics {
                contentDescription = "Simulation Mode: ${mode.name}. Tap to switch."
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (isPractical) "PRACTICAL" else "IDEAL",
            style = LogicLabsType.TechnicalSm,
            color = accent
        )
    }
}

@Composable
private fun RestoreIcPill(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .background(ShortCircuitAlert.copy(alpha = 0.20f))
            .border(
                1.dp,
                ShortCircuitAlert.copy(alpha = 0.85f),
                RoundedCornerShape(Dimens.RadiusSm)
            )
            .desktopPointerHand()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space2, vertical = 3.dp)
            .semantics {
                contentDescription = "Damaged IC detected. Tap to restore IC."
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(ShortCircuitAlert)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "RESTORE IC",
            style = LogicLabsType.TechnicalSm,
            color = ShortCircuitAlert
        )
    }
}
