package com.logiclabs.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.ChassisBase
import com.logiclabs.core.designsystem.theme.ChassisDivider
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.LogicLabsType
import com.logiclabs.core.designsystem.theme.SurfaceCardBorder
import com.logiclabs.core.designsystem.theme.TextPrimary
import com.logiclabs.core.designsystem.theme.TextSecondary

/**
 * Standard content root for desktop modals and sheets on the bench.
 */
@Composable
fun SheetScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleIcon: ImageVector? = null,
    onDismiss: (() -> Unit)? = null,
    accent: Color = AccentCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space4, vertical = Dimens.Space3)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (titleIcon != null) {
                Icon(
                    imageVector = titleIcon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(Dimens.Space2))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = LogicLabsType.TitleSm,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = LogicLabsType.TechnicalSm,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onDismiss != null) {
                Spacer(Modifier.width(Dimens.Space2))
                DesktopTooltip(text = "Close", shortcut = "Esc") {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).desktopPointerHand()
                    ) {
                        Icon(
                            imageVector = LogicIcons.Close,
                            contentDescription = "Close (Esc)",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Dimens.Space2))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.Hairline)
                .clip(RoundedCornerShape(Dimens.RadiusPill))
                .background(ChassisDivider)
        )

        Spacer(Modifier.height(Dimens.Space3))

        content()
    }
}

/**
 * Centered desktop modal dialog container replacing Android's ModalBottomSheet.
 *
 * Responsively caps width and height against the actual viewport so laptop displays at
 * 1366x768 @ 125% DPI never push modal footers or scrollbars off-screen.
 */
@Composable
fun DesktopModalDialog(
    onDismissRequest: () -> Unit,
    maxWidth: Dp = 680.dp,
    maxHeight: Dp = 640.dp,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.Space4, vertical = Dimens.Space3),
            contentAlignment = Alignment.Center
        ) {
            val boundMaxW = minOf(maxWidth, this.maxWidth).coerceAtLeast(320.dp)
            val boundMinW = minOf(420.dp, boundMaxW)
            val boundMaxH = minOf(maxHeight, this.maxHeight * 0.94f).coerceAtLeast(300.dp)

            Box(
                modifier = Modifier
                    .widthIn(min = boundMinW, max = boundMaxW)
                    .heightIn(max = boundMaxH)
                    .clip(RoundedCornerShape(Dimens.RadiusXl))
                    .background(ChassisBase)
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(Dimens.RadiusXl))
            ) {
                content()
            }
        }
    }
}

@Composable
fun SheetDragHandle(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.height(Dimens.Space1))
}
