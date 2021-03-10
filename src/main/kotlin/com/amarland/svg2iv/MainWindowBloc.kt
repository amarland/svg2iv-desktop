package com.amarland.svg2iv

import androidx.compose.material.SnackbarDuration
import com.amarland.svg2iv.outerworld.callCliTool
import com.amarland.svg2iv.ui.CustomIcons
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import java.io.File

class MainWindowBloc {

    private val coroutineScope = MainScope()

    private val _effects = Channel<MainWindowEffect>(Channel.UNLIMITED)
    val effects = _effects.consumeAsFlow()

    private val _state = MutableStateFlow(MainWindowState.INITIAL)
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

    private fun mapEventToEffect(
        event: MainWindowEvent,
        currentState: MainWindowState
    ): MainWindowEffect? =
        when (event) {
            is MainWindowEvent.SourceFilesParsed -> {
                if (event.errorMessages.isNotEmpty()) {
                    MainWindowEffect.ShowSnackbar(
                        id = SNACKBAR_ID_PREVIEW_ERRORS,
                        message = "Error(s) occurred while trying to display a preview" +
                                " of the source(s)",
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
    ) = when (event) {
        MainWindowEvent.ToggleThemeButtonClicked ->
            currentState.copy(isThemeDark = !currentState.isThemeDark)

        is MainWindowEvent.SourceFilesSelected -> {
            val files = event.files
            currentState.copy(
                sourceFilesSelectionTextFieldState = TextFieldState(
                    value = files.singleOrNull()?.path ?: files.joinToString { it.name },
                    isErrorValue = files.any { !it.exists() }
                )
            ).also { parseSourceFiles(files) }
        }

        is MainWindowEvent.SourceFilesParsed -> {
            currentState.copy(
                extensionReceiverTextFieldState = currentState.extensionReceiverTextFieldState
                    .copy(placeholder = event.imageVectors.firstOrNull()?.name),
                imageVectors = event.imageVectors.map { it ?: CustomIcons.ErrorCircle }
                    .takeUnless { it.isEmpty() } ?: currentState.imageVectors,
                errorMessages = event.errorMessages,
                currentPreviewIndex = 0,
                isPreviousPreviewButtonEnabled = false,
                isNextPreviewButtonEnabled = event.imageVectors.size > 1
            )
        }

        is MainWindowEvent.DestinationDirectorySelected ->
            currentState.copy(
                destinationDirectorySelectionTextFieldState = TextFieldState(
                    value = event.directory.path.orEmpty(),
                    isErrorValue = !event.directory.exists()
                )
            )

        is MainWindowEvent.AllInOneCheckboxClicked ->
            currentState.copy(
                isAllInOneCheckboxChecked = !currentState.isAllInOneCheckboxChecked
            )

        MainWindowEvent.PreviousPreviewButtonClicked -> {
            val previewIndex = currentState.currentPreviewIndex - 1
            currentState.copy(
                currentPreviewIndex = previewIndex,
                isPreviousPreviewButtonEnabled = previewIndex > 0,
                isNextPreviewButtonEnabled = true
            )
        }

        MainWindowEvent.NextPreviewButtonClicked -> {
            val previewIndex = currentState.currentPreviewIndex + 1
            currentState.copy(
                currentPreviewIndex = previewIndex,
                isPreviousPreviewButtonEnabled = true,
                isNextPreviewButtonEnabled = previewIndex < currentState.imageVectors.lastIndex
            )
        }

        is MainWindowEvent.SnackbarActionButtonClicked ->
            when (event.snackbarId) {
                SNACKBAR_ID_PREVIEW_ERRORS ->
                    currentState.copy(areErrorMessagesShown = true)

                else -> throw IllegalArgumentException(
                    "Unrecognized snackbarId: ${event.snackbarId}"
                )
            }

        MainWindowEvent.ErrorMessagesDialogDismissed ->
            currentState.copy(areErrorMessagesShown = false)

        else -> currentState
    }

    private fun parseSourceFiles(files: Collection<File>) {
        coroutineScope.launch {
            try {
                callCliTool(files)
            } catch (e: Exception) {
                TODO()
            }.also { (imageVectors, errorMessages) ->
                addEvent(MainWindowEvent.SourceFilesParsed(imageVectors, errorMessages))
            }
        }
    }

    private companion object {

        private const val SNACKBAR_ID_PREVIEW_ERRORS = 0x3B9ACA00
    }
}
