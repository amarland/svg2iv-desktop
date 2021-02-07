package com.amarland.svg2iv

import androidx.compose.ui.graphics.vector.ImageVector
import com.amarland.svg2iv.ui.CustomIcons

data class MainWindowState(
    val isThemeDark: Boolean,
    val sourceFilesSelectionTextFieldState: TextFieldState,
    val destinationDirectorySelectionTextFieldState: TextFieldState,
    val extensionReceiverTextFieldState: TextFieldState,
    val isAllInOneCheckboxChecked: Boolean,
    val preview: ImageVector,
    val isPreviousPreviewButtonEnabled: Boolean,
    val isNextPreviewButtonEnabled: Boolean
) {

    companion object {

        @JvmField
        val INITIAL = MainWindowState(
            isThemeDark = false,
            sourceFilesSelectionTextFieldState = TextFieldState.DEFAULT,
            destinationDirectorySelectionTextFieldState = TextFieldState.DEFAULT,
            extensionReceiverTextFieldState = TextFieldState.DEFAULT,
            isAllInOneCheckboxChecked = false,
            preview = CustomIcons.Polygon,
            isPreviousPreviewButtonEnabled = false,
            isNextPreviewButtonEnabled = false
        )
    }
}

data class TextFieldState(
    val value: String,
    val isErrorValue: Boolean = false,
    val placeholder: String? = null
) {

    companion object {

        @JvmField
        val DEFAULT = TextFieldState(value = "")
    }
}
