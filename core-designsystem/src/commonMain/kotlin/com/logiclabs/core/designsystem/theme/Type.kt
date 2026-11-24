package com.logiclabs.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Engineering monospace font family: JetBrains Mono (loaded via [LogicLabsFontStore]).
 */
val JetBrainsMono: FontFamily
    get() = LogicLabsFontStore.monoFamily()

/**
 * Hardware interface sans font family: Inter (loaded via [LogicLabsFontStore]).
 */
val Inter: FontFamily
    get() = LogicLabsFontStore.interFamily()

/**
 * Type scale for the Logic Labs bench.
 * Properties resolve lazily through [LogicLabsFontStore] so Web builds bind pre-fetched TTF bytes.
 */
object LogicLabsType {

    val TechnicalXs: TextStyle
        get() = TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            lineHeight = 11.sp
        )

    val TechnicalSm: TextStyle
        get() = TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 13.sp
        )

    val TechnicalMd: TextStyle
        get() = TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

    val TechnicalLg: TextStyle
        get() = TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp
        )

    val HashMono: TextStyle
        get() = TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.5.sp
        )

    val DisplayLg: TextStyle
        get() = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.01).em
        )

    val TitleMd: TextStyle
        get() = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.01).em
        )

    val TitleSm: TextStyle
        get() = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.01).em
        )

    val BodyMd: TextStyle
        get() = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.01).em
        )

    val BodySm: TextStyle
        get() = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = (-0.01).em
        )

    val LabelSm: TextStyle
        get() = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )

    val SwitchPlate: TextStyle
        get() = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.12.em
        )

    val SwitchPlateLg: TextStyle
        get() = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.12.em
        )
}

val LogicLabsTypography: Typography
    get() = Typography(
        displayLarge = LogicLabsType.DisplayLg,
        titleLarge = LogicLabsType.TitleMd,
        titleMedium = LogicLabsType.TitleSm,
        titleSmall = LogicLabsType.LabelSm,
        bodyLarge = LogicLabsType.BodyMd,
        bodyMedium = LogicLabsType.BodyMd,
        bodySmall = LogicLabsType.BodySm,
        labelLarge = LogicLabsType.LabelSm,
        labelMedium = LogicLabsType.SwitchPlateLg,
        labelSmall = LogicLabsType.SwitchPlate
    )
