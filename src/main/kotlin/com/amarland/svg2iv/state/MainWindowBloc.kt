package com.amarland.svg2iv.state

import androidx.compose.material.SnackbarDuration
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import com.amarland.svg2iv.outerworld.callCliTool
import com.amarland.svg2iv.outerworld.openLogFileInPreferredApplication
import com.amarland.svg2iv.outerworld.readErrorMessages
import com.amarland.svg2iv.util.ShortcutKey
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalComposeUiApi
class MainWindowBloc {

    private val imageVectors = mutableListOf<ImageVector?>()
    private var previewIndex = 0

    private val coroutineScope = MainScope()

    private val _effects = Channel<MainWindowEffect>(Channel.UNLIMITED)
    val effects = _effects.receiveAsFlow()

    private val _state =
        MutableStateFlow(MainWindowState.initial(isThemeDark = isDarkModeEnabled))
    val state = _state.asStateFlow()

    private val eventSink: SendChannel<MainWindowEvent>

    init {
        eventSink = Channel(Channel.UNLIMITED)
        coroutineScope.launch {
            eventSink.consumeAsFlow().collect { event ->
                val currentState = _state.value
                mapEventToEffect(event, currentState)?.also { effect -> _effects.send(effect) }
                mapEventToState(event, currentState).also { state -> _state.value = state }
            }
        }
    }

    fun addEvent(event: MainWindowEvent) {
        coroutineScope.launch { eventSink.send(event) }
    }

    private val didErrorsOccur: Boolean
        get() = imageVectors.any { imageVector -> imageVector == null }

    private fun mapEventToEffect(
        event: MainWindowEvent,
        currentState: MainWindowState
    ): MainWindowEffect? =
        when (event) {
            MainWindowEvent.SelectSourceFilesButtonClicked ->
                MainWindowEffect.OpenFileSelectionDialog.takeIf {
                    currentState.areFileSystemEntitySelectionButtonsEnabled
                }

            MainWindowEvent.SelectDestinationDirectoryButtonClicked ->
                MainWindowEffect.OpenDirectorySelectionDialog.takeIf {
                    currentState.areFileSystemEntitySelectionButtonsEnabled
                }

            is MainWindowEvent.SourceFilesParsed -> {
                if (didErrorsOccur) {
                    val message =
                        "Error(s) occurred while trying to display a preview of the source(s)"
                    MainWindowEffect.ShowSnackbar(
                        id = SNACKBAR_ID_PREVIEW_ERRORS,
                        message = message,
                        actionLabel = "View errors",
                        duration = SnackbarDuration.Indefinite
                    )
                } else null
            }

            else -> null
        }

    private fun mapEventToState(
        event: MainWindowEvent,
        currentState: MainWindowState
    ): MainWindowState = when (event) {
        MainWindowEvent.ToggleThemeButtonClicked ->
            currentState.copy(isThemeDark = !currentState.isThemeDark).also { newState ->
                isDarkModeEnabled = newState.isThemeDark
            }

        MainWindowEvent.SelectSourceFilesButtonClicked,
        MainWindowEvent.SelectDestinationDirectoryButtonClicked ->
            currentState.copy(areFileSystemEntitySelectionButtonsEnabled = false)

        is MainWindowEvent.SourceFilesSelectionDialogClosed -> {
            val paths = event.paths
            currentState.copy(
                sourceFilesSelectionTextFieldState = TextFieldState(
                    value = paths.singleOrNull() ?: paths.joinToString(),
                    isError = paths.any { path -> !File(path).exists() }
                ),
                areFileSystemEntitySelectionButtonsEnabled = true
            ).also { parseSourceFiles(paths) }
        }

        is MainWindowEvent.SourceFilesParsed -> {
            val imageVector =
                if (imageVectors.isNotEmpty()) imageVectors[0]
                else currentState.imageVector
            currentState.copy(
                sourceFilesSelectionTextFieldState = currentState.sourceFilesSelectionTextFieldState
                    .copy(isError = didErrorsOccur),
                extensionReceiverTextFieldState = currentState.extensionReceiverTextFieldState
                    .copy(placeholder = imageVectors.firstOrNull()?.name),
                imageVector = imageVector,
                isPreviousPreviewButtonEnabled = false,
                isNextPreviewButtonEnabled = imageVectors.size > 1
            )
        }

        is MainWindowEvent.DestinationDirectorySelectionDialogClosed -> {
            val path = event.path
            currentState.copy(
                destinationDirectorySelectionTextFieldState = TextFieldState(
                    value = path.orEmpty(),
                    isError = path != null && !File(path).exists()
                ),
                areFileSystemEntitySelectionButtonsEnabled = true
            )
        }

        is MainWindowEvent.AllInOneCheckboxClicked ->
            currentState.copy(
                isAllInOneCheckboxChecked = !currentState.isAllInOneCheckboxChecked
            )

        MainWindowEvent.PreviousPreviewButtonClicked -> {
            currentState.copy(
                imageVector = imageVectors[--previewIndex],
                isPreviousPreviewButtonEnabled = previewIndex > 0,
                isNextPreviewButtonEnabled = true
            )
        }

        MainWindowEvent.NextPreviewButtonClicked -> {
            currentState.copy(
                imageVector = imageVectors[++previewIndex],
                isPreviousPreviewButtonEnabled = true,
                isNextPreviewButtonEnabled = previewIndex < imageVectors.lastIndex
            )
        }

        is MainWindowEvent.SnackbarActionButtonClicked -> {
            when (val snackbarId = event.snackbarId) {
                SNACKBAR_ID_PREVIEW_ERRORS -> {
                    val (errorMessages, hasMoreThanLimit) =
                        readErrorMessages(MAX_ERROR_MESSAGE_COUNT)
                    currentState.copy(
                        errorMessagesDialogState =
                        ErrorMessagesDialogState.Shown(
                            errorMessages,
                            isReadMoreButtonVisible = hasMoreThanLimit
                        )
                    )
                }

                else -> throw IllegalArgumentException("Unrecognized snackbar ID: $snackbarId")
            }
        }

        MainWindowEvent.ErrorMessagesDialogCloseRequested ->
            currentState.copy(errorMessagesDialogState = ErrorMessagesDialogState.NotShown)

        MainWindowEvent.ReadMoreErrorMessagesActionClicked -> {
            openLogFileInPreferredApplication()
            currentState.copy(errorMessagesDialogState = ErrorMessagesDialogState.NotShown)
        }

        else -> currentState
    }

    private fun parseSourceFiles(paths: List<String>) {
        if (paths.isEmpty()) return

        imageVectors.clear()
        previewIndex = 0

        coroutineScope.launch {
            try {
                @Suppress("BlockingMethodInNonBlockingContext")
                callCliTool(paths).also(imageVectors::addAll)
            } catch (e: Exception) {
                // ignored
            }
            addEvent(MainWindowEvent.SourceFilesParsed)
        }
    }

    companion object {

        @JvmField
        val SHORTCUT_BINDINGS = buildMap<ShortcutKey, (MainWindowBloc) -> Unit> {
            this[ShortcutKey.newInstance(Key.S, isAltPressed = true)] =
                { bloc -> bloc.addEvent(MainWindowEvent.SelectSourceFilesButtonClicked) }

            this[ShortcutKey.newInstance(Key.D, isAltPressed = true)] =
                { bloc -> bloc.addEvent(MainWindowEvent.SelectDestinationDirectoryButtonClicked) }

            this[ShortcutKey.newInstance(Key.Escape)] =
                { bloc ->
                    val state = bloc.state.value
                    if (state.errorMessagesDialogState is ErrorMessagesDialogState.Shown) {
                        bloc.addEvent(MainWindowEvent.ErrorMessagesDialogCloseRequested)
                    }
                }
        }

        private const val MAX_ERROR_MESSAGE_COUNT = 8

        private const val SNACKBAR_ID_PREVIEW_ERRORS = 0x3B9ACA00
    }
}
