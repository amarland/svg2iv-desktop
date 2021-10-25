package com.amarland.svg2iv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import com.amarland.svg2iv.outerworld.FileSystemEntitySelectionMode
import com.amarland.svg2iv.outerworld.openDirectorySelectionDialog
import com.amarland.svg2iv.outerworld.openFileSelectionDialog
import com.amarland.svg2iv.state.MainWindowBloc
import com.amarland.svg2iv.state.MainWindowEffect
import com.amarland.svg2iv.state.MainWindowEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@ExperimentalComposeUiApi
val LocalBloc = compositionLocalOf<MainWindowBloc> {
    error("CompositionLocal LocalBloc not provided!")
}

// TODO: this is meant to be temporary (until dialogs are rewritten to use the new Window API)
val LocalComposeWindow = compositionLocalOf<ComposeWindow> {
    error("CompositionLocal LocalComposeWindow not provided!")
}

private val ANDROID_GREEN = Color(0xFF00DE7A)
private val ANDROID_BLUE = Color(0xFF2196F3)

private val JETBRAINS_MONO: FontFamily =
    FontFamily(Font(resource = "font/jetbrains_mono_regular.ttf"))

private val WORK_SANS: FontFamily =
    FontFamily(Font(resource = "font/work_sans_variable.ttf"))

@ExperimentalComposeUiApi
@ExperimentalCoroutinesApi
@ExperimentalMaterialApi
@Suppress("FunctionName")
@Composable
fun MainWindowContent(bloc: MainWindowBloc) {
    val coroutineScope = rememberCoroutineScope()
    val state = bloc.state.collectAsState().value

    CircularReveal(targetState = state.isThemeDark) { isThemeDark ->
        MaterialTheme(
            colors = if (isThemeDark) {
                darkColors(primary = ANDROID_GREEN, secondary = ANDROID_BLUE)
            } else {
                lightColors(primary = ANDROID_BLUE, secondary = ANDROID_GREEN)
            },
            typography = Typography(defaultFontFamily = WORK_SANS)
        ) {
            Box {
                Column {
                    TopAppBar(
                        title = { Text("SVG to ImageVector conversion tool") },
                        actions = {
                            IconButton(
                                onClick = { bloc.addEvent(MainWindowEvent.ToggleThemeButtonClicked) }
                            ) { Icon(imageVector = CustomIcons.ToggleTheme, contentDescription = null) }
                            IconButton(
                                onClick = { /* TODO */ }
                            ) { Icon(imageVector = Icons.Outlined.Info, contentDescription = null) }
                        }
                    )

                    Row(modifier = Modifier.background(color = MaterialTheme.colors.background)) {
                        val scaffoldState = rememberScaffoldState()

                        CompositionLocalProvider(LocalBloc provides bloc) {
                            // not an absolute necessity, but makes handling Snackbars easier,
                            // and allows "customization" of their width without visual "glitches",
                            // although it might just be me who couldn't figure out how to achieve this
                            Scaffold(
                                modifier = Modifier.fillMaxWidth(2F / 3F).fillMaxHeight(),
                                scaffoldState = scaffoldState
                            ) {
                                // TODO: use `State` for "effects"?
                                val window = LocalComposeWindow.current
                                var job: Job? by remember { mutableStateOf(null) }
                                DisposableEffect(scaffoldState) {
                                    job = bloc.effects.onEach { effect ->
                                        launchEffect(effect, bloc, window, scaffoldState)
                                    }.launchIn(coroutineScope)

                                    onDispose { job?.cancel() }
                                }

                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val sourceFilesSelectionTextFieldState =
                                        state.sourceFilesSelectionTextFieldState
                                    val areButtonsEnabled =
                                        state.areFileSystemEntitySelectionButtonsEnabled
                                    FileSystemEntitySelectionField(
                                        selectionMode = FileSystemEntitySelectionMode.SOURCE,
                                        value = sourceFilesSelectionTextFieldState.value,
                                        isError = sourceFilesSelectionTextFieldState.isError,
                                        isButtonEnabled = areButtonsEnabled
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                                            Checkbox(
                                                checked = state.isAllInOneCheckboxChecked,
                                                onCheckedChange = {
                                                    bloc.addEvent(MainWindowEvent.AllInOneCheckboxClicked)
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generate all assets in a single file")
                                    }

                                    val destinationDirectorySelectionTextFieldState =
                                        state.destinationDirectorySelectionTextFieldState
                                    FileSystemEntitySelectionField(
                                        selectionMode = FileSystemEntitySelectionMode.DESTINATION,
                                        value = destinationDirectorySelectionTextFieldState.value,
                                        isError = destinationDirectorySelectionTextFieldState.isError,
                                        isButtonEnabled = areButtonsEnabled
                                    )

                                    OutlinedTextField(
                                        value = state.extensionReceiverTextFieldState.value,
                                        onValueChange = {},
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Extension receiver (optional)") },
                                        singleLine = true,
                                        placeholder = {
                                            Text(state.extensionReceiverTextFieldState.value)
                                        }
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val previewSizeFraction = 0.65F
                                Checkerboard(Modifier.fillMaxSize(previewSizeFraction))
                                Image(
                                    imageVector = state.imageVectors[state.currentPreviewIndex],
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(previewSizeFraction)
                                )

                                PreviewSelectionButton(
                                    icon = Icons.Outlined.KeyboardArrowLeft,
                                    onClick = {
                                        bloc.addEvent(MainWindowEvent.PreviousPreviewButtonClicked)
                                    },
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    isEnabled = state.isPreviousPreviewButtonEnabled
                                )

                                PreviewSelectionButton(
                                    icon = Icons.Outlined.KeyboardArrowRight,
                                    onClick = {
                                        bloc.addEvent(MainWindowEvent.NextPreviewButtonClicked)
                                    },
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    isEnabled = state.isNextPreviewButtonEnabled
                                )

                                ExtendedFloatingActionButton(
                                    text = {
                                        Text(
                                            "Convert",
                                            style = MaterialTheme.typography
                                                .button
                                                .copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    },
                                    onClick = { bloc.addEvent(MainWindowEvent.ConvertButtonClicked) },
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                    icon = {
                                        Icon(
                                            imageVector = CustomIcons.ConvertVector,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                if (state.areErrorMessagesShown) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(
                                color = MaterialTheme.colors.onBackground
                                    .copy(alpha = ContentAlpha.disabled)
                            )
                    )

                    Surface(
                        modifier = Modifier.widthIn(max = 600.dp)
                            .padding(16.dp)
                            .align(alignment = Alignment.Center),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            LazyColumn {
                                items(state.errorMessages) { line ->
                                    Text(line, fontFamily = JETBRAINS_MONO)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    bloc.addEvent(MainWindowEvent.ErrorMessagesDialogCloseButtonClicked)
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
private suspend fun launchEffect(
    effect: MainWindowEffect,
    bloc: MainWindowBloc,
    window: ComposeWindow,
    scaffoldState: ScaffoldState
) {
    when (effect) {
        is MainWindowEffect.ShowSnackbar -> {
            val (snackbarId, message, actionLabel, duration) = effect
            val result = scaffoldState.snackbarHostState
                .showSnackbar(message, actionLabel, duration)
            snackbarId.takeIf { result == SnackbarResult.ActionPerformed }
                ?.let(MainWindowEvent::SnackbarActionButtonClicked)
                ?.let(bloc::addEvent)
        }

        is MainWindowEffect.OpenFileSelectionDialog ->
            openFileSelectionDialog(window).also { selectedPaths ->
                bloc.addEvent(MainWindowEvent.SourceFilesSelectionDialogClosed(selectedPaths))
            }

        is MainWindowEffect.OpenDirectorySelectionDialog ->
            openDirectorySelectionDialog(window).also { selectedPath ->
                bloc.addEvent(
                    MainWindowEvent.DestinationDirectorySelectionDialogClosed(selectedPath)
                )
            }
    }
}
