package com.logiclabs.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.logiclabs.core.data.progress.BadgeCatalog
import com.logiclabs.core.data.progress.BenchStats
import com.logiclabs.core.designsystem.component.DesktopModalDialog
import com.logiclabs.core.designsystem.component.DesktopVerticalScrollbar
import com.logiclabs.core.designsystem.component.LogicIcons
import com.logiclabs.core.designsystem.component.SheetScaffold
import com.logiclabs.core.designsystem.component.keyboardPageScroll
import com.logiclabs.core.designsystem.theme.AmberCore
import com.logiclabs.core.designsystem.theme.ChassisBase
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.PhosphorCore
import com.logiclabs.core.designsystem.theme.SurfaceCard
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextSecondary
import com.logiclabs.core.designsystem.theme.TextTertiary

@Composable
fun BadgesSheet(
    stats: BenchStats,
    classicLabCount: Int,
    onDismiss: () -> Unit
) {
    val badges = BadgeCatalog.derive(stats, classicLabCount)
    val earnedCount = badges.count { it.second }
    val listState = rememberLazyListState()

    DesktopModalDialog(
        onDismissRequest = onDismiss,
        maxWidth = 600.dp,
        maxHeight = 600.dp
    ) {
        SheetScaffold(
            title = "BENCH ACHIEVEMENTS",
            subtitle = "$earnedCount of ${badges.size} earned · verified labs, builds and bench time",
            titleIcon = LogicIcons.Rosette,
            onDismiss = onDismiss
        ) {
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
                    items(badges, key = { it.first.id }) { (badge, earned) ->
                        BadgeRow(title = badge.title, description = badge.description, earned = earned)
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

@Composable
private fun BadgeRow(title: String, description: String, earned: Boolean) {
    val accent = if (earned) AmberCore else TextTertiary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusLg))
            .background(if (earned) SurfaceCard else ChassisBase)
            .border(
                width = Dimens.Hairline,
                color = if (earned) AmberCore.copy(alpha = 0.45f) else TextTertiary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(Dimens.RadiusLg)
            )
            .padding(Dimens.Space3)
            .semantics {
                contentDescription = if (earned) "Achievement earned: $title" else "Locked achievement: $title"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = LogicIcons.Rosette,
            contentDescription = null,
            tint = if (earned) accent else TextTertiary.copy(alpha = 0.55f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(Dimens.Space3))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = LogicLabsType.TitleSm,
                color = if (earned) TextPrimary else TextTertiary,
                maxLines = 1
            )
            Text(
                text = description,
                style = LogicLabsType.BodySm,
                color = TextSecondary,
                maxLines = 2
            )
        }
        if (earned) {
            Spacer(Modifier.width(Dimens.Space2))
            Icon(
                imageVector = LogicIcons.Verify,
                contentDescription = null,
                tint = PhosphorCore,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
