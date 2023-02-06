package com.amarland.svg2iv.ui

import androidx.compose.material3.SnackbarDuration

data class SnackbarInfo(
    val id: Int,
    val message: String,
    val actionLabel: String?,
    val duration: SnackbarDuration
)
