package com.amarland.svg2iv.state

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key

sealed class MainWindowEvent {

    object ToggleThemeButtonClicked : MainWindowEvent()

    object SelectSourceFilesButtonClicked : MainWindowEvent()

    data class SourceFilesSelectionDialogClosed(val paths: List<String>) : MainWindowEvent()

    data class SourceFilesParsed(
        val imageVectors: List<ImageVector?>,
        val errorMessages: List<String>
    ) : MainWindowEvent()

    object SelectDestinationDirectoryButtonClicked : MainWindowEvent()

    data class DestinationDirectorySelectionDialogClosed(val path: String?) : MainWindowEvent()

    object AllInOneCheckboxClicked : MainWindowEvent()

    object PreviousPreviewButtonClicked : MainWindowEvent()

    object NextPreviewButtonClicked : MainWindowEvent()

    object ConvertButtonClicked : MainWindowEvent()

    data class SnackbarActionButtonClicked(val snackbarId: Int) : MainWindowEvent()

    object ErrorMessagesDialogCloseButtonClicked : MainWindowEvent()

    data class MnemonicPressed(val key: Key) : MainWindowEvent()

    object EscapeKeyPressed : MainWindowEvent()
}
