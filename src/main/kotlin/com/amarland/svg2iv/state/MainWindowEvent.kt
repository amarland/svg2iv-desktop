package com.amarland.svg2iv.state

sealed interface MainWindowEvent {

    object ToggleThemeButtonClicked : MainWindowEvent

    object AboutButtonClicked : MainWindowEvent

    object SelectSourceFilesButtonClicked : MainWindowEvent

    data class SourceFilesSelectionDialogClosed(val paths: List<String>) : MainWindowEvent

    object SelectDestinationDirectoryButtonClicked : MainWindowEvent

    data class DestinationDirectorySelectionDialogClosed(val path: String?) : MainWindowEvent

    object AllInOneCheckboxClicked : MainWindowEvent

    object PreviousPreviewButtonClicked : MainWindowEvent

    object NextPreviewButtonClicked : MainWindowEvent

    object ConvertButtonClicked : MainWindowEvent

    data class SnackbarActionButtonClicked(val snackbarId: Int) : MainWindowEvent

    object InformationDialogCloseRequested : MainWindowEvent

    object ProjectRepositoryUrlClicked : MainWindowEvent

    object ReadMoreErrorMessagesActionClicked : MainWindowEvent
}
