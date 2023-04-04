package com.amarland.svg2iv.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font

private val WORK_SANS: FontFamily = FontFamily(Font(resource = "font/work_sans_variable.ttf"))

val MaterialTheme.workSansTypography
    @Composable
    @ReadOnlyComposable
    get() = with(typography) {
        copy(
            displayLarge = displayLarge.copy(fontFamily = WORK_SANS),
            displayMedium = displayMedium.copy(fontFamily = WORK_SANS),
            displaySmall = displaySmall.copy(fontFamily = WORK_SANS),
            headlineLarge = headlineLarge.copy(fontFamily = WORK_SANS),
            headlineMedium = headlineMedium.copy(fontFamily = WORK_SANS),
            headlineSmall = headlineSmall.copy(fontFamily = WORK_SANS),
            titleLarge = titleLarge.copy(fontFamily = WORK_SANS),
            titleMedium = titleMedium.copy(fontFamily = WORK_SANS),
            titleSmall = titleSmall.copy(fontFamily = WORK_SANS),
            bodyLarge = bodyLarge.copy(fontFamily = WORK_SANS),
            bodyMedium = bodyMedium.copy(fontFamily = WORK_SANS),
            bodySmall = bodySmall.copy(fontFamily = WORK_SANS),
            labelLarge = labelLarge.copy(fontFamily = WORK_SANS),
            labelMedium = labelMedium.copy(fontFamily = WORK_SANS),
            labelSmall = labelSmall.copy(fontFamily = WORK_SANS)
        )
    }
