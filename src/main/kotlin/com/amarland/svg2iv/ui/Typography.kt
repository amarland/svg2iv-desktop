package com.amarland.svg2iv.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font

private val NOTO_SANS: FontFamily = FontFamily(
    Font(resource = "font/NotoSans/NotoSans-Regular.ttf"),
    Font(resource = "font/NotoSans/NotoSans-Medium.ttf")
)

val MaterialTheme.notoSansTypography
    @Composable
    @ReadOnlyComposable
    get() = with(typography) {
        copy(
            displayLarge = displayLarge.copy(fontFamily = NOTO_SANS),
            displayMedium = displayMedium.copy(fontFamily = NOTO_SANS),
            displaySmall = displaySmall.copy(fontFamily = NOTO_SANS),
            headlineLarge = headlineLarge.copy(fontFamily = NOTO_SANS),
            headlineMedium = headlineMedium.copy(fontFamily = NOTO_SANS),
            headlineSmall = headlineSmall.copy(fontFamily = NOTO_SANS),
            titleLarge = titleLarge.copy(fontFamily = NOTO_SANS),
            titleMedium = titleMedium.copy(fontFamily = NOTO_SANS),
            titleSmall = titleSmall.copy(fontFamily = NOTO_SANS),
            bodyLarge = bodyLarge.copy(fontFamily = NOTO_SANS),
            bodyMedium = bodyMedium.copy(fontFamily = NOTO_SANS),
            bodySmall = bodySmall.copy(fontFamily = NOTO_SANS),
            labelLarge = labelLarge.copy(fontFamily = NOTO_SANS),
            labelMedium = labelMedium.copy(fontFamily = NOTO_SANS),
            labelSmall = labelSmall.copy(fontFamily = NOTO_SANS)
        )
    }
