package com.amarland.svg2iv.ui

import androidx.compose.desktop.LocalAppWindow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amarland.svg2iv.outerworld.FileSystemEntitySelectionMode
import com.amarland.svg2iv.outerworld.openFileChooser
import com.amarland.svg2iv.state.MainWindowBloc
import com.amarland.svg2iv.state.MainWindowEffect
import com.amarland.svg2iv.state.MainWindowEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File

private val ANDROID_GREEN = Color(0xFF00DE7A)
private val ANDROID_BLUE = Color(0xFF2196F3)

private val JETBRAINS_MONO: FontFamily =
    FontFamily(Font(file = File("font/jetbrains_mono_regular.ttf")))

private val LocalBloc = compositionLocalOf<MainWindowBloc> {
    error("CompositionLocal LocalBloc not provided!")
}

@Composable
@ExperimentalCoroutinesApi
@ExperimentalMaterialApi
@Suppress("FunctionName")
fun MainWindowContent(bloc: MainWindowBloc) {
    val state = bloc.state.collectAsState().value

    MaterialTheme(
        colors = if (state.isThemeDark) {
            darkColors(primary = ANDROID_GREEN, secondary = ANDROID_BLUE)
        } else {
            lightColors(primary = ANDROID_BLUE, secondary = ANDROID_GREEN)
        }
    ) {
        Column {
            if (state.areErrorMessagesShown) {
                Dialog(
                    onDismissRequest = {
                        bloc.addEvent(MainWindowEvent.ErrorMessagesDialogDismissed)
                    },
                    properties = DialogProperties(size = IntSize(550, 350))
                ) {
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        items(state.errorMessages) { line ->
                            Text(line, fontFamily = JETBRAINS_MONO)
                        }
                    }
                }
            }

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
                    // although it might just be me who hasn't figured out how to achieve this
                    Scaffold(
                        modifier = Modifier.fillMaxWidth(2F / 3F).fillMaxHeight(),
                        scaffoldState = scaffoldState
                    ) {
                        LaunchedEffect(bloc) {
                            bloc.effects.onEach { effect ->
                                launchEffect(effect, bloc, scaffoldState)
                            }.launchIn(this)
                        }

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            FileSystemEntitySelectionField(
                                FileSystemEntitySelectionMode.SOURCE,
                                state.sourceFilesSelectionTextFieldState.value,
                                state.sourceFilesSelectionTextFieldState.isError,
                            )
                            FileSystemEntitySelectionField(
                                FileSystemEntitySelectionMode.DESTINATION,
                                state.destinationDirectorySelectionTextFieldState.value,
                                state.destinationDirectorySelectionTextFieldState.isError,
                            )
                            OutlinedTextField(
                                value = state.extensionReceiverTextFieldState.value,
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Extension receiver (optional)") },
                                singleLine = true,
                                placeholder = { Text(state.extensionReceiverTextFieldState.value) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = state.isAllInOneCheckboxChecked,
                                    onCheckedChange = {
                                        bloc.addEvent(MainWindowEvent.AllInOneCheckboxClicked)
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Generate all assets in a single file")
                            }
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
                            text = { Text("Convert") },
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
    }
}

@Composable
@Suppress("FunctionName")
private fun PreviewSelectionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    isEnabled: Boolean
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        enabled = isEnabled,
        shape = RoundedCornerShape(percent = 50),
        contentPadding = PaddingValues(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null)
    }
}

@Composable
@Suppress("FunctionName")
private fun FileSystemEntitySelectionField(
    mode: FileSystemEntitySelectionMode,
    value: String,
    isError: Boolean
) {
    Row(verticalAlignment = Alignment.Bottom) {
        val label: String
        val leadingIcon: ImageVector
        when (mode) {
            FileSystemEntitySelectionMode.SOURCE -> {
                label = "Source file(s)"
                leadingIcon = CustomIcons.SourceFiles
            }
            FileSystemEntitySelectionMode.DESTINATION -> {
                label = "Destination directory"
                leadingIcon = CustomIcons.DestinationDirectory
            }
        }

        OutlinedTextField(
            value = TextFieldValue(value, selection = TextRange(value.length)),
            onValueChange = { /* it is read-only, we have control over the value */ },
            modifier = Modifier.weight(1F),
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            leadingIcon = { Icon(imageVector = leadingIcon, contentDescription = null) },
            isError = isError
        )

        val window = LocalAppWindow.current.window
        val bloc = LocalBloc.current
        OutlinedButton(
            onClick = {
                openFileChooser(window, mode).also { files ->
                    val event = when (mode) {
                        FileSystemEntitySelectionMode.SOURCE ->
                            files.takeIf { it.isNotEmpty() }
                                ?.let { MainWindowEvent.SourceFilesSelected(files.toList()) }
                        FileSystemEntitySelectionMode.DESTINATION -> {
                            files.singleOrNull()
                                ?.let { directory ->
                                    MainWindowEvent.DestinationDirectorySelected(directory)
                                }
                        }
                    }
                    if (event != null) bloc.addEvent(event)
                }
            },
            modifier = Modifier.height(56.dp).padding(start = 8.dp)
        ) {
            Icon(imageVector = CustomIcons.ExploreFiles, contentDescription = null)
        }
    }
}

@Composable
private fun Checkerboard(
    modifier: Modifier,
    squareColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.12F)
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val squareSizeInDp = 8
        val maxWidth = constraints.maxWidth
        val maxHeight = constraints.maxHeight
        Canvas(
            modifier = Modifier.size(
                width = (maxWidth - maxWidth % squareSizeInDp).dp,
                height = (maxHeight - maxHeight % squareSizeInDp).dp
            )
        ) {
            val squareSizeInPixels = squareSizeInDp.dp.toPx()
            var x = 0F
            var y = 0F
            while (y < size.height) {
                drawRect(
                    topLeft = Offset(x, y),
                    size = Size(squareSizeInPixels, squareSizeInPixels),
                    color = squareColor,
                )
                x = if (x < size.width - squareSizeInPixels * 2) {
                    x + squareSizeInPixels * 2
                } else {
                    (y + squareSizeInPixels) % (squareSizeInPixels * 2)
                }
                if (x <= squareSizeInPixels) y += squareSizeInPixels
            }
        }
    }
}

@ExperimentalMaterialApi
private suspend fun launchEffect(
    effect: MainWindowEffect,
    bloc: MainWindowBloc,
    scaffoldState: ScaffoldState
) {
    when (effect) {
        is MainWindowEffect.ShowSnackbar -> {
            val (snackbarId, message, actionLabel, duration) = effect
            scaffoldState.snackbarHostState.showSnackbar(message, actionLabel, duration)
                .also { result ->
                    if (result == SnackbarResult.ActionPerformed) {
                        bloc.addEvent(MainWindowEvent.SnackbarActionButtonClicked(snackbarId))
                    }
                }
        }
    }
}
