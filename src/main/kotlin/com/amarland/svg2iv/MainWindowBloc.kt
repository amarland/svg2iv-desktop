package com.amarland.svg2iv

import androidx.compose.ui.graphics.vector.ImageVector
import com.amarland.svg2iv.outerworld.callCliTool
import com.amarland.svg2iv.ui.CustomIcons
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

@ObsoleteCoroutinesApi
class MainWindowBloc {

    private val coroutineScope = MainScope()

    /*
    private val _effects = Channel<MainWindowEffect>(Channel.UNLIMITED)
    val effects = _effects.consumeAsFlow()
    */

    private val _state = MutableStateFlow(MainWindowState.INITIAL)
    val state = _state.asStateFlow()

    private var currentPreviewIndex = 0
    private var imageVectorsAndErrorMessages: Pair<List<ImageVector?>, List<String>> =
        emptyList<ImageVector>() to emptyList()

    private val eventSink = coroutineScope.actor<MainWindowEvent> {
        for (event in channel) {
            // mapEventToEffect(event)?.also { effect -> _effects.send(effect) }
            mapEventToState(event, _state.value).also { state -> _state.value = state }
        }
    }

    fun addEvent(event: MainWindowEvent) {
        coroutineScope.launch { eventSink.send(event) }
    }

    /*
    private fun mapEventToEffect(event: MainWindowEvent): MainWindowEffect? =
        when (event) {
            MainWindowEvent.ConvertButtonClicked ->
                ShowSnackbar("Can't do that right now", "Wait", SnackbarDuration.Short)
            else -> null
        }
    */

    private fun mapEventToState(event: MainWindowEvent, currentState: MainWindowState) =
        when (event) {
            is MainWindowEvent.ToggleThemeButtonClicked ->
                currentState.copy(isThemeDark = !currentState.isThemeDark)

            is MainWindowEvent.SourceFilesSelected ->
                currentState.copy(
                    sourceFilesSelectionTextFieldState = TextFieldState(
                        value = event.files.joinToString(),
                        isErrorValue = event.files.any { !it.exists() }
                    )
                ).also { parseSourceFiles(event.files) }

            is MainWindowEvent.SourceFilesParsed -> {
                currentPreviewIndex = 0
                val (imageVectors, errorMessages) = event.imageVectorsAndErrorMessages
                    .also { imageVectorsAndErrorMessages = it }
                val firstImageVector = imageVectors.firstOrNull()
                currentState.copy(
                    preview = firstImageVector ?: MainWindowState.INITIAL.preview,
                    extensionReceiverTextFieldState = currentState.extensionReceiverTextFieldState
                        .copy(placeholder = firstImageVector?.name),
                    isPreviousPreviewButtonEnabled = false,
                    isNextPreviewButtonEnabled = imageVectors.size > 1
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

            is MainWindowEvent.PreviousPreviewButtonClicked ->
                currentState.copy(
                    preview = imageVectorsAndErrorMessages.first[currentPreviewIndex--]
                        ?: CustomIcons.ErrorCircle,
                    isPreviousPreviewButtonEnabled = currentPreviewIndex == 0
                )

            is MainWindowEvent.NextPreviewButtonClicked ->
                currentState.copy(
                    preview = imageVectorsAndErrorMessages.first[currentPreviewIndex++]
                        ?: CustomIcons.ErrorCircle,
                    isNextPreviewButtonEnabled = currentPreviewIndex ==
                            imageVectorsAndErrorMessages.first.lastIndex
                )

            is MainWindowEvent.ConvertButtonClicked -> TODO()
        }

    private fun parseSourceFiles(files: Collection<File>) {
        coroutineScope.launch {
            try {
                callCliTool(files)
            } catch (e: Exception) {
                TODO()
            }.also { imageVectorsAndErrorMessages ->
                addEvent(MainWindowEvent.SourceFilesParsed(imageVectorsAndErrorMessages))
            }
        }
    }
}
