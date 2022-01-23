package com.amarland.svg2iv.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class MainWindowState(
    val isThemeDark: Boolean,
    val sourceFilesSelectionTextFieldState: TextFieldState,
    val destinationDirectorySelectionTextFieldState: TextFieldState,
    val areFileSystemEntitySelectionButtonsEnabled: Boolean,
    val extensionReceiverTextFieldState: TextFieldState,
    val isAllInOneCheckboxChecked: Boolean,
    val imageVector: ImageVector,
    val errorMessages: List<String>,
    val areErrorMessagesShown: Boolean,
    val isPreviousPreviewButtonEnabled: Boolean,
    val isNextPreviewButtonEnabled: Boolean
) {

    companion object {

        @JvmStatic
        fun initial(isThemeDark: Boolean) =
            MainWindowState(
                isThemeDark = isThemeDark,
                sourceFilesSelectionTextFieldState = TextFieldState.DEFAULT,
                destinationDirectorySelectionTextFieldState = TextFieldState.DEFAULT,
                areFileSystemEntitySelectionButtonsEnabled = true,
                extensionReceiverTextFieldState = TextFieldState.DEFAULT,
                isAllInOneCheckboxChecked = false,
                imageVector = Icons.Outlined.Face,
                errorMessages = emptyList(),
                areErrorMessagesShown = false,
                isPreviousPreviewButtonEnabled = false,
                isNextPreviewButtonEnabled = false
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
