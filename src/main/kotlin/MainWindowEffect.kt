import androidx.compose.material.SnackbarDuration

sealed class MainWindowEffect

data class ShowSnackbar(
    val message: String,
    val actionLabel: String?,
    val duration: SnackbarDuration
) : MainWindowEffect()

// TODO: OpenFileChooser
