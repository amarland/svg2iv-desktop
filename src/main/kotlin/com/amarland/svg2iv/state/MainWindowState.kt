package com.amarland.svg2iv.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.ui.graphics.vector.ImageVector

data class MainWindowState(
    val isThemeDark: Boolean,
    val sourceFilesSelectionTextFieldState: TextFieldState,
    val destinationDirectorySelectionTextFieldState: TextFieldState,
    val areFileSystemEntitySelectionButtonsEnabled: Boolean,
    val extensionReceiverTextFieldState: TextFieldState,
    val isAllInOneCheckboxChecked: Boolean,
    val imageVectors: List<ImageVector>,
    val errorMessages: List<String>,
    val areErrorMessagesShown: Boolean,
    val currentPreviewIndex: Int,
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
                imageVectors = listOf(Icons.Outlined.Face),
                errorMessages = emptyList(),
                areErrorMessagesShown = false,
                currentPreviewIndex = 0,
                isPreviousPreviewButtonEnabled = false,
                isNextPreviewButtonEnabled = false
            )
    }
}

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
