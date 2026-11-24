package com.logiclabs.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.core.designsystem.theme.GlassHairline
import com.logiclabs.core.designsystem.theme.GlassSpecular
import com.logiclabs.core.designsystem.theme.GlassSurfaceColor

/**
 * Sleek translucent hardware surface used for floating HUD chrome (command bar, zoom bar, ribbons).
 *
 * Intentionally avoids per-frame Skia Gaussian backdrop blurs (`ImageFilter.makeBlur`) so low-RAM
 * desktops and integrated/software GPUs render toolbars with zero frame-time penalty while preserving
 * the crisp top specular sheen and hairline bezel.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Dimens.RadiusLg),
    content: @Composable BoxScope.() -> Unit
) {
    val specular = Brush.verticalGradient(
        colors = listOf(GlassSpecular, Color.Transparent)
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(GlassSurfaceColor)
            .border(Dimens.Hairline, GlassHairline, shape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(specular)
        )
        content()
    }
}

val GlassSpecularHeight = 24.dp
