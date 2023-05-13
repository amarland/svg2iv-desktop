package com.amarland.svg2iv.state

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.res.useResource
import com.amarland.svg2iv.outerworld.callCliTool
import com.amarland.svg2iv.outerworld.openLogFileInPreferredApplication
import com.amarland.svg2iv.outerworld.readErrorMessages
import com.amarland.svg2iv.outerworld.writeImageVectorsToFile
import com.amarland.svg2iv.ui.SnackbarInfo
import com.amarland.svg2iv.util.LicenseReport
import com.amarland.svg2iv.util.ShortcutKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.buffer
import okio.source
import java.io.IOException
import java.util.Collections.emptyList
import kotlin.io.path.Path
import kotlin.io.path.exists
import com.amarland.svg2iv.state.MainWindowBloc as Bloc

class MainWindowBloc(private val coroutineScope: CoroutineScope) {

    private val imageVectors = mutableListOf<ImageVector?>()
    private var previewIndex = 0

    private val _state = MutableStateFlow(
        MainWindowState.initial(isThemeDark = useDarkMode)
    )
    val state = _state.asStateFlow()

    private val currentState get() = _state.value

    fun addEvent(event: MainWindowEvent) {
        coroutineScope.launch {
            mapEventToState(event, _state::emit)
        }
    }

    private suspend fun mapEventToState(
        event: MainWindowEvent,
        emit: suspend (MainWindowState) -> Unit
    ) {
        when (event) {
            MainWindowEvent.ToggleThemeButtonClicked ->
                emit(
                    currentState.copy(isThemeDark = !currentState.isThemeDark)
                        .also { newState ->
                            useDarkMode = newState.isThemeDark
                        }
                )

            MainWindowEvent.AboutButtonClicked ->
                emit(
                    currentState.copy(
                        informationDialog = InformationDialog.About(
                            listDependencies()
                        )
                    )
                )

            MainWindowEvent.SelectSourceFilesButtonClicked ->
                if (currentState.areFileSystemEntitySelectionButtonsEnabled) {
                    emit(
                        currentState.copy(
                            areFileSystemEntitySelectionButtonsEnabled = false,
                            selectionDialog = SelectionDialog.Source
                        )
                    )
                }

            MainWindowEvent.SelectDestinationDirectoryButtonClicked ->
                if (currentState.areFileSystemEntitySelectionButtonsEnabled) {
                    emit(
                        currentState.copy(
                            areFileSystemEntitySelectionButtonsEnabled = false,
                            selectionDialog = SelectionDialog.Destination
                        )
                    )
                }

            is MainWindowEvent.SourceFilesSelectionDialogClosed ->
                onSourceFilesSelectionDialogClosed(event.paths, emit)

            is MainWindowEvent.DestinationDirectorySelectionDialogClosed -> {
                val path = event.path
                emit(
                    currentState.copy(
                        selectionDialog = null,
                        destinationDirectorySelectionTextFieldState = TextFieldState(
                            value = path.orEmpty(),
                            isError = path != null && !Path(path).exists()
                        ),
                        areFileSystemEntitySelectionButtonsEnabled = true
                    )
                )
            }

            is MainWindowEvent.AllInOneCheckboxClicked -> {
                val isAllInOneCheckboxChecked = !currentState.isAllInOneCheckboxChecked
                emit(currentState.copy(isAllInOneCheckboxChecked = isAllInOneCheckboxChecked))
            }

            MainWindowEvent.PreviousPreviewButtonClicked -> {
                emit(
                    currentState.copy(
                        imageVector = imageVectors[--previewIndex],
                        isPreviousPreviewButtonVisible = previewIndex > 0,
                        isNextPreviewButtonVisible = true
                    )
                )
            }

            MainWindowEvent.NextPreviewButtonClicked -> {
                emit(
                    currentState.copy(
                        imageVector = imageVectors[++previewIndex],
                        isPreviousPreviewButtonVisible = true,
                        isNextPreviewButtonVisible = previewIndex < imageVectors.lastIndex
                    )
                )
            }

            MainWindowEvent.ConvertButtonClicked -> {
                try {
                    writeImageVectorsToFile(
                        currentState.destinationDirectorySelectionTextFieldState.value,
                        imageVectors.filterNotNull(),
                        currentState.extensionReceiverTextFieldState.value
                    )
                } catch (ioe: IOException) {
                    TODO()
                }
                emit(currentState.copy(isWorkInProgress = true))
            }

            is MainWindowEvent.SnackbarActionButtonClicked -> {
                when (val snackbarId = event.snackbarId) {
                    SNACKBAR_ID_PREVIEW_ERRORS -> {
                        val (errorMessages, hasMoreThanLimit) =
                            readErrorMessages(MAX_ERROR_MESSAGE_COUNT)
                        emit(
                            currentState.copy(
                                informationDialog = InformationDialog.ErrorMessages(
                                    errorMessages,
                                    isReadMoreButtonVisible = hasMoreThanLimit
                                )
                            )
                        )
                    }

                    else -> {
                        throw IllegalArgumentException("Unrecognized snackbar ID: $snackbarId")
                    }
                }
            }

            MainWindowEvent.InformationDialogCloseRequested ->
                emit(currentState.copy(informationDialog = null))

            MainWindowEvent.ReadMoreErrorMessagesActionClicked -> {
                openLogFileInPreferredApplication()
                emit(currentState.copy(informationDialog = null))
            }
        }
    }

    private suspend fun onSourceFilesSelectionDialogClosed(
        paths: List<String>,
        emit: suspend (MainWindowState) -> Unit
    ) {
        val hasPaths = paths.isNotEmpty()

        emit(
            currentState.copy(
                isWorkInProgress = hasPaths,
                selectionDialog = null,
                sourceFilesSelectionTextFieldState = TextFieldState(
                    value = paths.singleOrNull() ?: paths.joinToString(),
                    isError = paths.any { path -> !Path(path).exists() }
                ),
                areFileSystemEntitySelectionButtonsEnabled = true
            )
        )

        if (hasPaths) {
            parseSourceFiles(paths)

            // delay(3.seconds)

            val didErrorsOccur = imageVectors.any { imageVector -> imageVector == null }
            val imageVector =
                if (imageVectors.isNotEmpty()) imageVectors[0]
                else currentState.imageVector
            emit(
                currentState.copy(
                    isWorkInProgress = false,
                    sourceFilesSelectionTextFieldState = currentState
                        .sourceFilesSelectionTextFieldState.copy(isError = didErrorsOccur),
                    extensionReceiverTextFieldState = currentState
                        .extensionReceiverTextFieldState.copy(
                            placeholder = imageVectors.firstOrNull()?.name
                        ),
                    imageVector = imageVector,
                    isPreviousPreviewButtonVisible = false,
                    isNextPreviewButtonVisible = imageVectors.size > 1
                )
            )

            if (didErrorsOccur) {
                val message = "Error(s) occurred while trying to display a preview of the source(s)"
                emit(
                    currentState.copy(
                        snackbarInfo = SnackbarInfo(
                            id = SNACKBAR_ID_PREVIEW_ERRORS,
                            message = message,
                            actionLabel = "View errors",
                            duration = SnackbarDuration.Indefinite
                        )
                    )
                )
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun listDependencies() =
        useResource("license-report/license-report.json") {
            moshi.adapter<LicenseReport>()
                .fromJson(it.source().buffer())
                ?.dependencies ?: emptyList()
        }

    private suspend fun parseSourceFiles(paths: List<String>) {
        imageVectors.clear()
        previewIndex = 0

        callCliTool(paths).also(imageVectors::addAll)
    }

    companion object {

        @OptIn(ExperimentalComposeUiApi::class)
        @JvmField
        val SHORTCUT_BINDINGS = buildMap<ShortcutKey, (Bloc) -> Unit> {
            this[ShortcutKey.newInstance(Key.S, isAltPressed = true)] =
                { bloc -> bloc.addEvent(MainWindowEvent.SelectSourceFilesButtonClicked) }

            this[ShortcutKey.newInstance(Key.D, isAltPressed = true)] =
                { bloc -> bloc.addEvent(MainWindowEvent.SelectDestinationDirectoryButtonClicked) }

            this[ShortcutKey.newInstance(Key.Escape)] =
                { bloc -> bloc.addEvent(MainWindowEvent.InformationDialogCloseRequested) }
        }

        private const val MAX_ERROR_MESSAGE_COUNT = 8

        private const val SNACKBAR_ID_PREVIEW_ERRORS = 0x3B9ACA00

        private val moshi by lazy(
            mode = LazyThreadSafetyMode.NONE,
            initializer = Moshi.Builder()::build
        )
    }
}
