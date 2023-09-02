package com.amarland.svg2iv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.amarland.svg2iv.APPLICATION_NAME
import com.amarland.svg2iv.APPLICATION_VERSION
import com.amarland.svg2iv.PROJECT_REPOSITORY_URL
import com.amarland.svg2iv.outerworld.DirectorySelectionDialog
import com.amarland.svg2iv.outerworld.FileSelectionDialog
import com.amarland.svg2iv.outerworld.FileSystemEntitySelectionMode
import com.amarland.svg2iv.outerworld.getAccentColorInt
import com.amarland.svg2iv.state.InformationDialog
import com.amarland.svg2iv.state.MainWindowBloc
import com.amarland.svg2iv.state.MainWindowEvent
import com.amarland.svg2iv.state.MainWindowState
import com.amarland.svg2iv.state.SelectionDialog
import com.amarland.svg2iv.util.ColorScheme
import io.material.color.utilities.scheme.Scheme

private val LocalBloc = compositionLocalOf<MainWindowBloc> {
    error("CompositionLocal LocalBloc not provided!")
}

val LocalComposeWindow = compositionLocalOf<ComposeWindow> {
    error("CompositionLocal LocalComposeWindow not provided!")
}

private val ANDROID_GREEN = 0xFF00DE7AU.toInt()
private val ANDROID_BLUE = 0xFF2196F3U.toInt()

private val JETBRAINS_MONO: FontFamily =
    FontFamily(Font(resource = "font/JetBrainsMono/JetBrainsMono-Regular.ttf"))

@Composable
fun MainWindowContent(bloc: MainWindowBloc) {
    CompositionLocalProvider(LocalBloc provides bloc) {
        val state by bloc.state.collectAsState()

        CircularReveal(targetState = state.isThemeDark) { isThemeDark ->
            var accentColor by remember { mutableStateOf<Int?>(0) }

            LaunchedEffect(Unit) {
                accentColor = getAccentColorInt()
            }

            if (accentColor != 0) {
                MaterialTheme(
                    colorScheme = ColorScheme(
                        if (isThemeDark) Scheme.dark(accentColor ?: ANDROID_GREEN)
                        else Scheme.light(accentColor ?: ANDROID_BLUE)
                    ),
                    typography = MaterialTheme.notoSansTypography
                ) {
                    Box {
                        Column {
                            AppBar()

                            Row(
                                modifier = Modifier.background(MaterialTheme.colorScheme.background)
                            ) {
                                LeftPanel(state)
                                RightPanel(state)
                            }
                        }

                        when (val dialog = state.informationDialog) {
                            is InformationDialog.About -> AboutDialog()
                            is InformationDialog.ErrorMessages -> ErrorMessagesDialog(dialog)
                            null -> {
                                if (state.isWorkInProgress) {
                                    SimpleDialog(isTransparent = true) {
                                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                            item = MaterialTheme.typography.bodyMedium.toSpanStyle(),
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
private fun AboutDialog() {
    SimpleDialog {
        Column {
            val typography = MaterialTheme.typography

            Row {
                Image(
                    painter = painterResource("logo.svg"),
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 12.dp)
                        .size(32.dp)
                )

                Column(
                    modifier = Modifier.weight(1F, fill = false)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(APPLICATION_NAME, style = typography.headlineSmall)
                    Text(APPLICATION_VERSION, style = typography.bodyMedium)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("© 2023 Anthony Marland", style = typography.bodySmall)
                }
            }

            val bloc = LocalBloc.current

            Spacer(modifier = Modifier.height(24.dp))
            ClickableText(
                AnnotatedString(PROJECT_REPOSITORY_URL),
                modifier = Modifier.padding(horizontal = 8.dp)
                    .align(Alignment.CenterHorizontally),
                style = typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
            ) {
                bloc.addEvent(MainWindowEvent.ProjectRepositoryUrlClicked)
            }

            DialogCloseButton(modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun ErrorMessagesDialog(dialog: InformationDialog.ErrorMessages) {
    SimpleDialog {
        Column {
            for (message in dialog.messages) {
                Text(message, fontFamily = JETBRAINS_MONO)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.align(Alignment.End)) {
                val bloc = LocalBloc.current

                if (dialog.isReadMoreButtonVisible) {
                    TextButton(
                        onClick = {
                            bloc.addEvent(MainWindowEvent.ReadMoreErrorMessagesActionClicked)
                        },
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
                    ) { Text("Read more") }
                }

                DialogCloseButton()
            }
        }
    }
}

@Composable
private fun DialogCloseButton(modifier: Modifier = Modifier) {
    val bloc = LocalBloc.current

    TextButton(
        onClick = {
            bloc.addEvent(MainWindowEvent.InformationDialogCloseRequested)
        },
        modifier = modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
    ) {
        Text("Close")
    }
}

@Composable
private fun SimpleDialog(
    isTransparent: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                // barrier/scrim
                color = Color.Black.copy(alpha = 0.74F)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isTransparent) {
            content()
        } else {
            Surface(
                modifier = Modifier.widthIn(max = 680.dp)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                content = {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        content = content
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeftPanel(state: MainWindowState) {
    val bloc = LocalBloc.current
    val snackbarHostState = remember { SnackbarHostState() }

    // not an absolute necessity, but makes handling Snackbars easier,
    // and allows "customization" of their width without visual "glitches",
    // although it might just be me who couldn't figure out
    // how to achieve this
    Scaffold(
        modifier = Modifier.fillMaxWidth(2F / 3F).fillMaxHeight(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        if (state.selectionDialog != null) {
            val window = LocalComposeWindow.current
            when (state.selectionDialog) {
                SelectionDialog.Source -> {
                    FileSelectionDialog(window) { selectedPaths ->
                        bloc.addEvent(
                            MainWindowEvent.SourceFilesSelectionDialogClosed(selectedPaths)
                        )
                    }
                }

                SelectionDialog.Destination -> {
                    DirectorySelectionDialog(window) { selectedPath ->
                        bloc.addEvent(
                            MainWindowEvent.DestinationDirectorySelectionDialogClosed(selectedPath)
                        )
                    }
                }
            }
        }

        if (state.snackbarInfo != null) {
            LaunchedEffect(state.snackbarInfo) {
                val (id, message, actionLabel, duration) = state.snackbarInfo
                val result = snackbarHostState.showSnackbar(
                    message,
                    actionLabel,
                    duration = duration
                )
                if (result == SnackbarResult.ActionPerformed) {
                    bloc.addEvent(MainWindowEvent.SnackbarActionButtonClicked(id))
                }
            }
        }

        Column(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
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

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(6.dp))

            val destinationDirectorySelectionTextFieldState =
                state.destinationDirectorySelectionTextFieldState
            FileSystemEntitySelectionField(
                onButtonClicked = {
                    bloc.addEvent(MainWindowEvent.SelectDestinationDirectoryButtonClicked)
                },
                selectionMode = FileSystemEntitySelectionMode.DESTINATION,
                value = destinationDirectorySelectionTextFieldState.value,
                isError = destinationDirectorySelectionTextFieldState.isError,
                isButtonEnabled = areButtonsEnabled
            )

            Spacer(modifier = Modifier.height(6.dp))

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
                .aspectRatio(1F),
            contentAlignment = Alignment.Center
        ) {
            Checkerboard()

            // resorting to drawing manually because of unexplained issues when going back and forth
            // between different IVs using `VectorPainter`
            val notPainter = remember { ImageVectorNotPainter() } // not a sub-class of `Painter`
            val size = DpSize(maxWidth, maxHeight) * 0.925F
            val tint =
                if (state.imageVector == null) MaterialTheme.colorScheme.error
                else Color.Unspecified
            Canvas(modifier = Modifier.size(size)) {
                notPainter.drawImageVectorInto(
                    this,
                    state.imageVector ?: CustomIcons.ErrorCircle,
                    IntSize(size.width.toPx().toInt(), size.height.toPx().toInt()),
                    tint
                )
            }
        }

        if (state.isPreviousPreviewButtonVisible) {
            PreviewSelectionButton(
                icon = Icons.Outlined.KeyboardArrowLeft,
                onClick = { bloc.addEvent(MainWindowEvent.PreviousPreviewButtonClicked) },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        if (state.isNextPreviewButtonVisible) {
            PreviewSelectionButton(
                icon = Icons.Outlined.KeyboardArrowRight,
                onClick = { bloc.addEvent(MainWindowEvent.NextPreviewButtonClicked) },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        ExtendedFloatingActionButton(
            text = {
                Text(
                    "Convert",
                    style = MaterialTheme.typography
                        .labelLarge
                        .copy(fontWeight = FontWeight.W600)
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
