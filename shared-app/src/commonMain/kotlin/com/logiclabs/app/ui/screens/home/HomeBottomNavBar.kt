package com.logiclabs.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logiclabs.core.designsystem.component.LogicIcons
import com.logiclabs.core.designsystem.component.desktopPointerHand
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.AmberCore
import com.logiclabs.core.designsystem.theme.ChassisBase
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.PhosphorCore
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.SurfaceCardBorder
import com.logiclabs.core.designsystem.theme.SurfaceRaised
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextSecondary
import com.logiclabs.core.designsystem.theme.TextTertiary
import com.logiclabs.feature.tools.feedback.Haptics

enum class HomeTab(
    val title: String,
    val icon: ImageVector,
    val shortcutHint: String
) {
    PROJECTS("Projects", LogicIcons.Breadboard, "Ctrl+1"),
    COURSEWORK("Coursework", LogicIcons.Labs, "Ctrl+2"),
    REFERENCE("74xx ICs", LogicIcons.Chip, "Ctrl+3"),
    SETTINGS("Settings", LogicIcons.Gear, "Ctrl+4")
}

/**
 * Left desktop workstation sidebar rail replacing the mobile bottom navigation bar.
 * Displays brand identity, quick New Circuit launch, keyboard-accessible tabs (`Ctrl+1..4`),
 * and direct access to the Controls & Shortcuts Guide (`F1`).
 *
 * Zero continuous 60 FPS animations — stays at 0% idle CPU when resting on the Home Hub.
 */
@Composable
fun DesktopHomeSidebar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onNewCircuit: () -> Unit,
    onOpenControlsGuide: () -> Unit,
    isPortableMode: Boolean,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxHeight().width(228.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(ChassisBase)
                .padding(horizontal = Dimens.Space3, vertical = Dimens.Space4),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            // Scrollable upper navigation section prevents clipping on short windows (560dp height)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                // Brand header
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "LOGIC LABS",
                            style = LogicLabsType.TitleMd,
                            color = TextPrimary,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Dimens.RadiusPill))
                                .background(PhosphorCore.copy(alpha = 0.14f))
                                .border(
                                    1.dp,
                                    PhosphorCore.copy(alpha = 0.55f),
                                    RoundedCornerShape(Dimens.RadiusPill)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(PhosphorCore)
                            )
                            Text(
                                text = "READY",
                                style = LogicLabsType.TechnicalXs,
                                color = PhosphorCore,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "IDL-800A WORKSTATION",
                        style = LogicLabsType.TechnicalXs,
                        color = TextTertiary
                    )
                }

                Spacer(Modifier.height(Dimens.Space1))

                // Primary CTA: New Circuit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.RadiusMd))
                        .background(AmberCore)
                        .desktopPointerHand()
                        .clickable(role = Role.Button) {
                            Haptics.thud()
                            onNewCircuit()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = LogicIcons.Breadboard,
                        contentDescription = null,
                        tint = ChassisBase,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "NEW CIRCUIT",
                        style = LogicLabsType.SwitchPlateLg,
                        color = ChassisBase,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(Dimens.Space2))

                Text(
                    text = "WORKBENCH",
                    style = LogicLabsType.TechnicalXs,
                    color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Navigation tabs
                HomeTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val accent = if (isSelected) AmberCore else TextSecondary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.RadiusMd))
                            .background(if (isSelected) AmberCore.copy(alpha = 0.14f) else SurfaceCard.copy(alpha = 0.35f))
                            .border(
                                width = if (isSelected) 1.dp else Dimens.Hairline,
                                color = if (isSelected) AmberCore.copy(alpha = 0.65f) else SurfaceCardBorder.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(Dimens.RadiusMd)
                            )
                            .desktopPointerHand()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClick = {
                                    if (!isSelected) {
                                        Haptics.tick()
                                        onTabSelected(tab)
                                    }
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = accent
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = tab.title,
                            style = LogicLabsType.SwitchPlateLg,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            text = tab.shortcutHint,
                            style = LogicLabsType.TechnicalXs,
                            color = if (isSelected) AmberCore else TextTertiary,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Controls & Shortcuts Guide button — always one click or F1 away
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusMd))
                    .background(AccentCyan.copy(alpha = 0.10f))
                    .border(Dimens.Hairline, AccentCyan.copy(alpha = 0.45f), RoundedCornerShape(Dimens.RadiusMd))
                    .desktopPointerHand()
                    .clickable(role = Role.Button) {
                        Haptics.tick()
                        onOpenControlsGuide()
                    }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = LogicIcons.Info,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "CONTROLS & KEYS",
                    style = LogicLabsType.SwitchPlate,
                    color = AccentCyan,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceRaised)
                        .border(Dimens.Hairline, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "F1",
                        style = LogicLabsType.TechnicalXs,
                        color = AccentCyan,
                        fontSize = 9.sp
                    )
                }
            }

            // Storage & version footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.Space1, start = 4.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (isPortableMode) {
                        "STORAGE · PORTABLE"
                    } else {
                        "STORAGE · ${com.logiclabs.app.platform.AppPlatform.standardStorageLabel}"
                    },
                    style = LogicLabsType.TechnicalXs,
                    color = TextTertiary,
                    fontSize = 9.sp
                )
                Text(
                    text = "v1.1.3 · ${com.logiclabs.app.platform.AppPlatform.platformEditionLabel}",
                    style = LogicLabsType.TechnicalXs,
                    color = TextTertiary,
                    fontSize = 9.sp
                )
            }
        }

        // Right rail hairline divider
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(SurfaceCardBorder)
        )
    }
}

/**
 * Compact mobile top header bar displayed on narrow viewports (< 760dp) in place of the left sidebar.
 */
@Composable
fun MobileHomeTopBar(
    onNewCircuit: () -> Unit,
    onOpenControlsGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(ChassisBase)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LOGIC LABS",
                    style = LogicLabsType.TitleSm,
                    color = TextPrimary,
                    letterSpacing = 1.0.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.RadiusPill))
                        .background(PhosphorCore.copy(alpha = 0.14f))
                        .border(1.dp, PhosphorCore.copy(alpha = 0.55f), RoundedCornerShape(Dimens.RadiusPill))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(PhosphorCore))
                    Text(
                        text = if (com.logiclabs.app.platform.AppPlatform.isWeb) "WEB" else "READY",
                        style = LogicLabsType.TechnicalXs,
                        color = PhosphorCore,
                        fontSize = 8.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusMd))
                    .background(AmberCore)
                    .desktopPointerHand()
                    .clickable(role = Role.Button) {
                        Haptics.tick()
                        onNewCircuit()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(LogicIcons.Breadboard, null, tint = ChassisBase, modifier = Modifier.size(14.dp))
                Text(
                    text = "NEW",
                    style = LogicLabsType.SwitchPlate,
                    color = ChassisBase,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(Dimens.RadiusMd))
                    .background(SurfaceCard)
                    .border(Dimens.Hairline, AccentCyan.copy(alpha = 0.45f), RoundedCornerShape(Dimens.RadiusMd))
                    .desktopPointerHand()
                    .clickable(role = Role.Button) {
                        Haptics.tick()
                        onOpenControlsGuide()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    style = LogicLabsType.TitleSm,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceCardBorder))
    }
}

/**
 * Hardware-styled bottom navigation bar for phones and portrait tablets (< 760dp).
 */
@Composable
fun MobileHomeBottomNavBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(ChassisBase)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceCardBorder))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.Space2, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                val accent = if (tab == HomeTab.PROJECTS || tab == HomeTab.COURSEWORK) AmberCore else AccentCyan
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Dimens.RadiusMd))
                        .background(if (selected) accent.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
                        .border(
                            width = if (selected) Dimens.Hairline else 0.dp,
                            color = if (selected) accent.copy(alpha = 0.55f) else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(Dimens.RadiusMd)
                        )
                        .desktopPointerHand()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) {
                            Haptics.tick()
                            onTabSelected(tab)
                        }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (selected) accent else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = tab.title,
                        style = LogicLabsType.TechnicalXs,
                        color = if (selected) accent else TextSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

