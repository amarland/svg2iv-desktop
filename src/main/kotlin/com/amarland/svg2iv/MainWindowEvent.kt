package com.amarland.svg2iv

import androidx.compose.ui.graphics.vector.ImageVector
import java.io.File

sealed class MainWindowEvent {

    object ToggleThemeButtonClicked : MainWindowEvent()

    class SourceFilesSelected(val files: Collection<File>) : MainWindowEvent()

    class SourceFilesParsed(
        val imageVectors: List<ImageVector?>,
        val errorMessages: List<String>
    ) : MainWindowEvent()

    class DestinationDirectorySelected(val directory: File) : MainWindowEvent()

    object AllInOneCheckboxClicked : MainWindowEvent()

    object PreviousPreviewButtonClicked : MainWindowEvent()

    object NextPreviewButtonClicked : MainWindowEvent()

    object ConvertButtonClicked : MainWindowEvent()

    class SnackbarActionButtonClicked(val snackbarId: Int) : MainWindowEvent()

    object ErrorMessagesDialogDismissed : MainWindowEvent()
}
