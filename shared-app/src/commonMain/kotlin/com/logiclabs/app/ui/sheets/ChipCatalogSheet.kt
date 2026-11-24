package com.logiclabs.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logiclabs.core.bridge.catalog.TTLChipCatalog
import com.logiclabs.core.designsystem.component.DesktopModalDialog
import com.logiclabs.core.designsystem.component.DesktopTooltip
import com.logiclabs.core.designsystem.component.DesktopVerticalScrollbar
import com.logiclabs.core.designsystem.component.InstantSearchBar
import com.logiclabs.core.designsystem.component.LogicIcons
import com.logiclabs.core.designsystem.component.SheetScaffold
import com.logiclabs.core.designsystem.component.desktopPointerHand
import com.logiclabs.core.designsystem.component.keyboardPageScroll
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.GlassHairline
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.SurfaceRaised
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextTertiary

/**
 * Desktop TTL 74xx IC Catalog with auto-focused search, visible scrollbar, and keyboard selection
 * (Up/Down/PgUp/PgDn to navigate, Enter to place on the breadboard, Esc to dismiss).
 */
@Composable
fun ChipCatalogSheet(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val parts = remember { TTLChipCatalog.getAllPartNumbers() }
    var inspectingPart by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var highlightedIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val searchFocus = remember { FocusRequester() }

    val filteredParts = remember(searchQuery, parts) {
        if (searchQuery.isBlank()) {
            parts
        } else {
            val q = searchQuery.trim().lowercase()
            parts.filter { part ->
                val info = TTLChipCatalog.getInfo(part)
                part.lowercase().contains(q) ||
                    (info?.description?.lowercase()?.contains(q) == true)
            }
        }
    }

    LaunchedEffect(filteredParts.size) {
        highlightedIndex = highlightedIndex.coerceIn(0, (filteredParts.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(Unit) {
        runCatching { searchFocus.requestFocus() }
    }

    LaunchedEffect(highlightedIndex, filteredParts.size) {
        if (filteredParts.isNotEmpty() && highlightedIndex in filteredParts.indices) {
            listState.animateScrollToItem(highlightedIndex)
        }
    }

    DesktopModalDialog(
        onDismissRequest = onDismiss,
        maxWidth = 660.dp,
        maxHeight = 640.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { ev ->
                    if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (ev.key) {
                        Key.Escape -> {
                            onDismiss()
                            true
                        }
                        Key.DirectionDown -> {
                            if (filteredParts.isNotEmpty()) {
                                highlightedIndex = (highlightedIndex + 1) % filteredParts.size
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (filteredParts.isNotEmpty()) {
                                highlightedIndex = (highlightedIndex - 1 + filteredParts.size) % filteredParts.size
                            }
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            val chosen = filteredParts.getOrNull(highlightedIndex)
                            if (chosen != null) {
                                onPick(chosen)
                                true
                            } else false
                        }
                        else -> false
                    }
                }
        ) {
            SheetScaffold(
                title = "TTL 74XX COMPONENT CATALOG",
                subtitle = if (searchQuery.isBlank()) {
                    "${parts.size} logic families · click or press ↑/↓ + Enter to place"
                } else {
                    "${filteredParts.size} of ${parts.size} logic families · Enter places highlighted chip"
                },
                titleIcon = LogicIcons.Chip,
                onDismiss = onDismiss
            ) {
                InstantSearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        highlightedIndex = 0
                    },
                    placeholder = "TYPE PART # OR GATE (e.g. 7408, NAND, FLIP-FLOP)...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocus)
                )
                Spacer(Modifier.height(Dimens.Space2))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .keyboardPageScroll(listState)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = Dimens.Space3),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                    ) {
                        if (filteredParts.isEmpty()) {
                            item {
                                Text(
                                    text = "No ICs matching \"$searchQuery\"",
                                    style = LogicLabsType.BodySm,
                                    color = TextTertiary,
                                    modifier = Modifier.padding(vertical = Dimens.Space3)
                                )
                            }
                        } else {
                            itemsIndexed(filteredParts, key = { _, p -> p }) { index, part ->
                                val info = TTLChipCatalog.getInfo(part)
                                PartRow(
                                    part = part,
                                    description = info?.description ?: "",
                                    pinCount = info?.pinCount ?: 14,
                                    highlighted = index == highlightedIndex,
                                    onClick = { onPick(part) },
                                    onInfoClick = { inspectingPart = part }
                                )
                            }
                        }
                    }

                    DesktopVerticalScrollbar(
                        listState = listState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    inspectingPart?.let { part ->
        IcPinoutInspectorSheet(
            partNumber = part,
            onDismiss = { inspectingPart = null }
        )
    }
}

@Composable
private fun PartRow(
    part: String,
    description: String,
    pinCount: Int,
    highlighted: Boolean,
    onClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(if (highlighted) SurfaceRaised else SurfaceCard)
            .border(
                width = if (highlighted) Dimens.BorderMd else Dimens.Hairline,
                color = if (highlighted) AccentCyan else GlassHairline,
                shape = RoundedCornerShape(Dimens.RadiusMd)
            )
            .desktopPointerHand()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = part,
                style = LogicLabsType.TechnicalMd,
                color = AccentCyan,
                maxLines = 1
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = LogicLabsType.BodySm,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(start = Dimens.Space2)
                .clip(RoundedCornerShape(Dimens.RadiusSm))
                .border(Dimens.Hairline, GlassHairline, RoundedCornerShape(Dimens.RadiusSm))
                .padding(horizontal = Dimens.Space2, vertical = 2.dp)
        ) {
            Text(
                text = "$pinCount PIN",
                style = LogicLabsType.TechnicalXs,
                color = TextTertiary,
                maxLines = 1
            )
        }

        DesktopTooltip(text = "Inspect $part pinout & gates") {
            Box(
                modifier = Modifier
                    .padding(start = Dimens.Space2)
                    .size(30.dp)
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .border(Dimens.Hairline, AccentCyan.copy(alpha = 0.45f), RoundedCornerShape(Dimens.RadiusSm))
                    .desktopPointerHand()
                    .clickable(onClick = onInfoClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LogicIcons.Info,
                    contentDescription = "Pinout info for $part",
                    tint = AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
