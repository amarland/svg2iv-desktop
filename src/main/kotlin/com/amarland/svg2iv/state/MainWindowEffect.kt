package com.amarland.svg2iv.state

import androidx.compose.material.SnackbarDuration

sealed class MainWindowEffect {

    data class ShowSnackbar(
        val id: Int,
        val message: String,
        val actionLabel: String?,
        val duration: SnackbarDuration
    ) : MainWindowEffect()

    // TODO: OpenFileChooser
}
