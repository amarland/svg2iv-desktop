package com.amarland.svg2iv.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.amarland.svg2iv.outerworld.FileSystemEntitySelectionMode
import com.amarland.svg2iv.outerworld.openDirectorySelectionDialog
import com.amarland.svg2iv.outerworld.openFileSelectionDialog
import com.amarland.svg2iv.state.Dialog
import com.amarland.svg2iv.state.MainWindowBloc
import com.amarland.svg2iv.state.MainWindowEffect
import com.amarland.svg2iv.state.MainWindowEvent
import com.amarland.svg2iv.state.MainWindowState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private val LocalBloc = compositionLocalOf<MainWindowBloc> {
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

@Composable
fun MainWindowContent(bloc: MainWindowBloc) {
    val state = bloc.state.collectAsState().value

    CompositionLocalProvider(LocalBloc provides bloc) {
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
                        AppBar()

                        Row(
                            modifier = Modifier.background(color = MaterialTheme.colors.background)
                        ) {
                            LeftPanel(state)
                            RightPanel(state)
                        }
                    }

                    when (val dialog = state.dialog) {
                        is Dialog.About -> AboutDialog(dialog)
                        is Dialog.ErrorMessages -> ErrorMessagesDialog(dialog)
                        Dialog.None -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBar() {
    val bloc = LocalBloc.current
    TopAppBar(
        title = {
            val title = "svg2iv | SVG to ImageVector conversion tool"
            Text(
                AnnotatedString(
                    title,
                    spanStyles = listOf(
                        AnnotatedString.Range(
                            item = MaterialTheme.typography.body2.toSpanStyle(),
                            start = 9,
                            end = title.length
                        )
                    )
                )
            )
        },
        actions = {
            IconButton(onClick = { bloc.addEvent(MainWindowEvent.ToggleThemeButtonClicked) }) {
                Icon(
                    imageVector = CustomIcons.ToggleTheme,
                    contentDescription = null
                )
            }
            IconButton(onClick = { bloc.addEvent(MainWindowEvent.AboutButtonClicked) }) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
private fun Dialog(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                // barrier/scrim
                color = Color.Black.copy(alpha = 0.74F)
            )
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 320.dp, max = 680.dp)
                .padding(16.dp)
                .align(alignment = Alignment.Center),
            shape = MaterialTheme.shapes.medium,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AboutDialog(dialog: Dialog.About) {
    Dialog {
        LazyColumn {
            items(dialog.dependencies) { dependency ->
                val licenseCount = dependency.moduleLicenses?.size ?: 0
                ListItem(
                    overlineText = {
                        Text("$licenseCount ${if (licenseCount == 1) "license" else "licenses"}")
                    },
                    text = { Text(dependency.moduleName) },
                    secondaryText = { Text(dependency.moduleVersion) }
                )
            }
        }
    }
}

@Composable
private fun ErrorMessagesDialog(dialog: Dialog.ErrorMessages) {
    Dialog {
        Column(modifier = Modifier.padding(top = 24.dp)) {
            for (message in dialog.messages) {
                Text(
                    message,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    fontFamily = JETBRAINS_MONO,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.align(Alignment.End)) {
                val bloc = LocalBloc.current

                if (dialog.isReadMoreButtonVisible) {
                    TextButton(
                        onClick = {
                            bloc.addEvent(MainWindowEvent.ReadMoreErrorMessagesActionClicked)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) { Text("Read more") }
                }

                TextButton(
                    onClick = {
                        bloc.addEvent(MainWindowEvent.ErrorMessagesDialogCloseRequested)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) { Text("Close") }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun LeftPanel(state: MainWindowState) {
    val bloc = LocalBloc.current
    val scaffoldState = rememberScaffoldState()

    // not an absolute necessity, but makes handling Snackbars easier,
    // and allows "customization" of their width without visual "glitches",
    // although it might just be me who couldn't figure out
    // how to achieve this
    Scaffold(
        modifier = Modifier.fillMaxWidth(2F / 3F).fillMaxHeight(),
        scaffoldState = scaffoldState
    ) {
        // TODO: use `State` for "effects"?
        val window = LocalComposeWindow.current
        val coroutineScope = rememberCoroutineScope()
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
                onButtonClicked = {
                    bloc.addEvent(MainWindowEvent.SelectSourceFilesButtonClicked)
                },
                selectionMode = FileSystemEntitySelectionMode.SOURCE,
                value = sourceFilesSelectionTextFieldState.value,
                isError = sourceFilesSelectionTextFieldState.isError,
                isButtonEnabled = areButtonsEnabled
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(
                    LocalMinimumTouchTargetEnforcement provides false
                ) {
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
                onButtonClicked = {
                    bloc.addEvent(MainWindowEvent.SelectDestinationDirectoryButtonClicked)
                },
                selectionMode = FileSystemEntitySelectionMode.DESTINATION,
                value = destinationDirectorySelectionTextFieldState.value,
                isError =
                destinationDirectorySelectionTextFieldState.isError,
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
}

@Composable
private fun RightPanel(state: MainWindowState) {
    val bloc = LocalBloc.current

    Box(
        modifier = Modifier.fillMaxSize()
            .padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(0.65F)
                .aspectRatio(1F)
        ) {
            Checkerboard()

            // resorting to drawing manually because of unexplained issues when going back and forth
            // between different IVs using `VectorPainter`
            val notPainter = remember { ImageVectorNotPainter() } // not a sub-class of `Painter`
            val size = DpSize(maxWidth, maxHeight)
            Canvas(modifier = Modifier.size(size)) {
                notPainter.drawImageVectorInto(
                    this,
                    state.imageVector ?: CustomIcons.ErrorCircle,
                    IntSize(size.width.toPx().toInt(), size.height.toPx().toInt())
                )
            }
        }

        if (state.isPreviousPreviewButtonVisible) {
            PreviewSelectionButton(
                icon = Icons.Outlined.KeyboardArrowLeft,
                onClick = {
                    bloc.addEvent(MainWindowEvent.PreviousPreviewButtonClicked)
                },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        if (state.isNextPreviewButtonVisible) {
            PreviewSelectionButton(
                icon = Icons.Outlined.KeyboardArrowRight,
                onClick = {
                    bloc.addEvent(MainWindowEvent.NextPreviewButtonClicked)
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        ExtendedFloatingActionButton(
            text = {
                Text(
                    "Convert",
                    style = MaterialTheme.typography
                        .button
                        .copy(fontWeight = FontWeight.SemiBold)
                )
            },
            onClick = {
                bloc.addEvent(MainWindowEvent.ConvertButtonClicked)
            },
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
