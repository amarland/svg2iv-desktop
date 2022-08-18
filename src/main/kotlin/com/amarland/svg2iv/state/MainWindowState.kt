package com.amarland.svg2iv.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.amarland.svg2iv.ui.SnackbarInfo
import com.amarland.svg2iv.util.Dependency

@Immutable
data class MainWindowState(
    val isThemeDark: Boolean,
    val isWorkInProgress: Boolean,
    val selectionDialog: SelectionDialog?,
    val sourceFilesSelectionTextFieldState: TextFieldState,
    val destinationDirectorySelectionTextFieldState: TextFieldState,
    val areFileSystemEntitySelectionButtonsEnabled: Boolean,
    val extensionReceiverTextFieldState: TextFieldState,
    val isAllInOneCheckboxChecked: Boolean,
    val imageVector: ImageVector?,
    val informationDialog: InformationDialog?,
    val snackbarInfo: SnackbarInfo?,
    val isPreviousPreviewButtonVisible: Boolean,
    val isNextPreviewButtonVisible: Boolean
) {

    companion object {

        @JvmStatic
        fun initial(isThemeDark: Boolean) =
            MainWindowState(
                isThemeDark = isThemeDark,
                isWorkInProgress = false,
                selectionDialog = null,
                sourceFilesSelectionTextFieldState = TextFieldState.DEFAULT,
                destinationDirectorySelectionTextFieldState = TextFieldState.DEFAULT,
                areFileSystemEntitySelectionButtonsEnabled = true,
                extensionReceiverTextFieldState = TextFieldState.DEFAULT,
                isAllInOneCheckboxChecked = false,
                imageVector = Icons.Outlined.Face,
                informationDialog = null,
                snackbarInfo = null,
                isPreviousPreviewButtonVisible = false,
                isNextPreviewButtonVisible = false
            )
    }
}

enum class SelectionDialog { Source, Destination }

@Immutable
data class TextFieldState(
    val value: String,
    val isError: Boolean = false,
    val placeholder: String? = null
) {

    companion object {

        @JvmField
        val DEFAULT = TextFieldState(value = "")
    }
}

@Immutable
sealed interface InformationDialog {

    @Immutable
    data class About(val dependencies: List<Dependency>) : InformationDialog

    @Immutable
    data class ErrorMessages(
        val messages: List<String>,
        val isReadMoreButtonVisible: Boolean
    ) : InformationDialog
}
