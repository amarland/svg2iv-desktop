package com.amarland.svg2iv.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.amarland.svg2iv.util.Dependency

@Immutable
data class MainWindowState(
    val isThemeDark: Boolean,
    val isWorkInProgress: Boolean,
    val sourceFilesSelectionTextFieldState: TextFieldState,
    val destinationDirectorySelectionTextFieldState: TextFieldState,
    val areFileSystemEntitySelectionButtonsEnabled: Boolean,
    val extensionReceiverTextFieldState: TextFieldState,
    val isAllInOneCheckboxChecked: Boolean,
    val imageVector: ImageVector?,
    val dialog: Dialog,
    val isPreviousPreviewButtonVisible: Boolean,
    val isNextPreviewButtonVisible: Boolean
) {

    companion object {

        @JvmStatic
        fun initial(isThemeDark: Boolean) =
            MainWindowState(
                isThemeDark = isThemeDark,
                isWorkInProgress = false,
                sourceFilesSelectionTextFieldState = TextFieldState.DEFAULT,
                destinationDirectorySelectionTextFieldState = TextFieldState.DEFAULT,
                areFileSystemEntitySelectionButtonsEnabled = true,
                extensionReceiverTextFieldState = TextFieldState.DEFAULT,
                isAllInOneCheckboxChecked = false,
                imageVector = Icons.Outlined.Face,
                dialog = Dialog.None,
                isPreviousPreviewButtonVisible = false,
                isNextPreviewButtonVisible = false
            )
    }
}

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
sealed interface Dialog {

    object None : Dialog

    @Immutable
    data class About(val dependencies: List<Dependency>) : Dialog

    @Immutable
    data class ErrorMessages(
        val messages: List<String>,
        val isReadMoreButtonVisible: Boolean
    ) : Dialog
}
