data class MainWindowState(
    val sourceFilesSelectionTextFieldState: TextFieldState,
    val destinationDirectorySelectionTextFieldState: TextFieldState,
    val isAllInOneCheckboxChecked: Boolean
) {

    companion object {

        @JvmField
        val INITIAL = MainWindowState(
            sourceFilesSelectionTextFieldState = TextFieldState.DEFAULT,
            destinationDirectorySelectionTextFieldState = TextFieldState.DEFAULT,
            isAllInOneCheckboxChecked = false
        )
    }
}

data class TextFieldState(val value: String, val isErrorValue: Boolean = false) {

    companion object {

        @JvmField
        val DEFAULT = TextFieldState(value = "")
    }
}
