package com.logiclabs.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.logiclabs.core.bridge.model.PinRole
import com.logiclabs.core.designsystem.canvas.DesktopTextPaint
import com.logiclabs.core.designsystem.canvas.TextAlignMode
import com.logiclabs.core.designsystem.canvas.drawText
import com.logiclabs.core.designsystem.theme.AccentCyan
import com.logiclabs.core.designsystem.theme.AmberCore
import com.logiclabs.core.designsystem.theme.BusGnd
import com.logiclabs.core.designsystem.theme.BusVcc
import com.logiclabs.core.designsystem.theme.ChipBodyChamfer
import com.logiclabs.core.designsystem.theme.ChipEpoxy
import com.logiclabs.core.designsystem.theme.ChipEpoxyTop
import com.logiclabs.core.designsystem.theme.ChipLeadShadow
import com.logiclabs.core.designsystem.theme.ChipLeadSilver
import com.logiclabs.core.designsystem.theme.ChipNotchShadow
import com.logiclabs.core.designsystem.theme.ChipPin1Dot
import com.logiclabs.core.designsystem.theme.ChipSilkscreenEmboss
import com.logiclabs.core.designsystem.theme.ChipSilkscreenText
import com.logiclabs.core.designsystem.theme.TextTertiary
import org.jetbrains.skia.Canvas as SkiaCanvas

@Composable
fun DipPackageDiagram(
    partNumber: String,
    pinCount: Int,
    pinLabels: List<String>,
    pinRoles: List<PinRole>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val half = pinCount / 2

    val slotW = 44.dp
    val leadLen = 13.dp
    val bodyH = 86.dp
    val labelZone = 30.dp
    val pad = 10.dp
    val firstLeadInset = 26.dp
    val totalW = pad * 2 + firstLeadInset + slotW * half + 12.dp
    val totalH = labelZone * 2 + leadLen * 2 + bodyH + pad * 2

    val paints = remember { DipDiagramPaints() }

    Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Canvas(
            modifier = Modifier
                .width(totalW)
                .height(totalH)
        ) {
            val u = 1.dp.toPx()
            val bodyLeft = pad.toPx() + 2.dp.toPx()
            val bodyTop = (pad + labelZone + leadLen).toPx()
            val bodyBottom = bodyTop + bodyH.toPx()
            val firstLeadX = bodyLeft + firstLeadInset.toPx()
            val leadStep = slotW.toPx()
            val bodyRight = firstLeadX + leadStep * (half - 1) + 24.dp.toPx()
            val bodyW = bodyRight - bodyLeft
            val cy = (bodyTop + bodyBottom) / 2f

            for (i in 0 until half) {
                val x = firstLeadX + leadStep * i
                drawLead(x, bodyTop, leadLen.toPx(), u, top = true)
                drawLead(x, bodyBottom, leadLen.toPx(), u, top = false)
            }

            drawRoundRect(
                color = Color(0x40000000),
                topLeft = Offset(bodyLeft + 3 * u, bodyTop + 4 * u),
                size = Size(bodyW, bodyH.toPx()),
                cornerRadius = CornerRadius(5 * u, 5 * u)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to ChipEpoxyTop,
                    0.5f to ChipEpoxy,
                    1f to ChipEpoxyTop
                ),
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, bodyH.toPx()),
                cornerRadius = CornerRadius(5 * u, 5 * u)
            )
            drawLine(
                ChipBodyChamfer.copy(alpha = 0.8f),
                Offset(bodyLeft + 6 * u, bodyTop + 2.5f * u),
                Offset(bodyRight - 6 * u, bodyTop + 2.5f * u),
                1.6f * u
            )
            drawLine(
                ChipNotchShadow.copy(alpha = 0.7f),
                Offset(bodyLeft + 6 * u, bodyBottom - 2.5f * u),
                Offset(bodyRight - 6 * u, bodyBottom - 2.5f * u),
                1.6f * u
            )
            drawRoundRect(
                color = ChipLeadShadow.copy(alpha = 0.8f),
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, bodyH.toPx()),
                cornerRadius = CornerRadius(5 * u, 5 * u),
                style = Stroke(1.2f * u)
            )

            drawCircle(ChipNotchShadow, radius = 8 * u, center = Offset(bodyLeft, cy))
            drawCircle(
                ChipLeadShadow.copy(alpha = 0.9f), radius = 8 * u,
                center = Offset(bodyLeft, cy), style = Stroke(1.2f * u)
            )
            drawCircle(ChipNotchShadow, radius = 3.4f * u, center = Offset(bodyLeft + 16 * u, bodyBottom - 10 * u))
            drawCircle(ChipPin1Dot.copy(alpha = 0.6f), radius = 1.2f * u, center = Offset(bodyLeft + 16 * u, bodyBottom - 10 * u))

            val nc = drawContext.canvas.nativeCanvas
            val centerX = (bodyLeft + bodyRight) / 2f
            val partPaint = paints.silkscreenPart(density.density).apply { setColor(ChipSilkscreenText) }
            val brandPaint = paints.silkscreenBrand(density.density).apply { setColor(ChipSilkscreenEmboss) }
            nc.drawText("SN${partNumber}N", centerX, cy - 2 * u, partPaint)
            nc.drawText("LOGIC LABS", centerX, cy + 10 * u, brandPaint)

            val numPaint = paints.pinNumber(density.density).apply { setColor(ChipSilkscreenText.copy(alpha = 0.9f)) }
            for (i in 0 until half) {
                val x = firstLeadX + leadStep * i
                val bottomPin = i + 1
                val topPin = pinCount - i

                nc.drawText("$bottomPin", x, bodyBottom - 8 * u, numPaint)
                nc.drawText("$topPin", x, bodyTop + 14 * u, numPaint)

                drawPinLabel(
                    nc, paints, density.density, x, bodyBottom + leadLen.toPx() + 6 * u,
                    bottomPin, pinLabels, pinRoles, top = false
                )
                drawPinLabel(
                    nc, paints, density.density, x, bodyTop - leadLen.toPx() - 4 * u,
                    topPin, pinLabels, pinRoles, top = true
                )
            }
        }
    }
}

private fun DrawScope.drawLead(x: Float, bodyEdgeY: Float, leadLen: Float, u: Float, top: Boolean) {
    val dir = if (top) -1f else 1f
    val leadW = 5.5f * u
    val shoulderW = 10f * u

    val shoulder = Path().apply {
        moveTo(x - leadW / 2f, bodyEdgeY + dir * 4.5f * u)
        lineTo(x + leadW / 2f, bodyEdgeY + dir * 4.5f * u)
        lineTo(x + shoulderW / 2f, bodyEdgeY - dir * 1f * u)
        lineTo(x - shoulderW / 2f, bodyEdgeY - dir * 1f * u)
        close()
    }
    drawPath(shoulder, ChipLeadSilver.copy(alpha = 0.85f))
    drawPath(shoulder, ChipLeadShadow.copy(alpha = 0.5f), style = Stroke(0.8f * u))

    val wingTop = if (top) bodyEdgeY - leadLen else bodyEdgeY
    drawRoundRect(
        brush = Brush.verticalGradient(
            if (top) listOf(ChipLeadSilver, ChipLeadShadow) else listOf(ChipLeadShadow, ChipLeadSilver)
        ),
        topLeft = Offset(x - leadW / 2f, wingTop),
        size = Size(leadW, leadLen),
        cornerRadius = CornerRadius(1.5f * u, 1.5f * u)
    )
    drawLine(
        ChipLeadShadow.copy(alpha = 0.6f),
        Offset(x + leadW / 2f, wingTop),
        Offset(x + leadW / 2f, wingTop + leadLen),
        0.8f * u
    )
}

private fun drawPinLabel(
    nc: SkiaCanvas,
    paints: DipDiagramPaints,
    density: Float,
    x: Float,
    edgeY: Float,
    pin: Int,
    labels: List<String>,
    roles: List<PinRole>,
    top: Boolean
) {
    val name = labels.getOrNull(pin - 1) ?: "$pin"
    val role = roles.getOrNull(pin - 1) ?: PinRole.INPUT
    val (badge, color) = dipPinRoleInfo(role)

    val namePaint = paints.pinName(density).apply { this.color = color.toArgb() }
    val badgePaint = paints.pinBadge(density).apply { this.color = color.copy(alpha = 0.75f).toArgb() }

    val nameY = if (top) edgeY - 6 * density else edgeY + 12 * density
    val badgeY = if (top) edgeY - 17 * density else edgeY + 23 * density

    nc.drawText(name, x, nameY, namePaint)
    if (role != PinRole.INPUT) {
        nc.drawText(badge, x, badgeY, badgePaint)
    }
}

fun dipPinRoleInfo(role: PinRole): Pair<String, Color> = when (role) {
    PinRole.POWER_VCC -> "VCC" to BusVcc
    PinRole.POWER_GND -> "GND" to BusGnd
    PinRole.OUTPUT -> "OUT" to AmberCore
    PinRole.NO_CONNECT -> "NC" to TextTertiary
    PinRole.CLOCK -> "CLK" to AccentCyan
    else -> "IN" to AccentCyan
}

private class DipDiagramPaints {
    private val part = DesktopTextPaint(ChipSilkscreenText, 11f, bold = true, monospace = true, align = TextAlignMode.CENTER)
    private val brand = DesktopTextPaint(ChipSilkscreenEmboss, 7f, bold = false, monospace = true, align = TextAlignMode.CENTER)
    private val number = DesktopTextPaint(ChipSilkscreenText, 8f, bold = false, monospace = true, align = TextAlignMode.CENTER)
    private val name = DesktopTextPaint(AccentCyan, 9.5f, bold = true, monospace = true, align = TextAlignMode.CENTER)
    private val badge = DesktopTextPaint(AccentCyan, 7f, bold = false, monospace = true, align = TextAlignMode.CENTER)

    fun silkscreenPart(density: Float) = part.apply { textSize = 11f * density }
    fun silkscreenBrand(density: Float) = brand.apply { textSize = 7f * density }
    fun pinNumber(density: Float) = number.apply { textSize = 8f * density }
    fun pinName(density: Float) = name.apply { textSize = 9.5f * density }
    fun pinBadge(density: Float) = badge.apply { textSize = 7f * density }
}
