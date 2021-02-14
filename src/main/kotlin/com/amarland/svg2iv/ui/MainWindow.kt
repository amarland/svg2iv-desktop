package com.amarland.svg2iv.ui

import androidx.compose.desktop.AppWindowAmbient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.ambientOf
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.amarland.svg2iv.MainWindowBloc
import com.amarland.svg2iv.MainWindowEvent
import com.amarland.svg2iv.outerworld.FileSystemEntitySelectionMode
import com.amarland.svg2iv.outerworld.openFileChooser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi

private val androidGreen = Color(0xFF00DE7A)
private val androidBlue = Color(0xFF2196F3)

@ObsoleteCoroutinesApi
private val BlocAmbient = ambientOf<MainWindowBloc>()

private val ScaffoldStateAmbient = ambientOf<ScaffoldState>()

@Composable
@ExperimentalCoroutinesApi
@ExperimentalLayout
@ExperimentalMaterialApi
@ObsoleteCoroutinesApi
@Suppress("FunctionName")
fun MainWindowContent(bloc: MainWindowBloc) {
    val state = bloc.state.collectAsState().value

    MaterialTheme(
        colors = if (state.isThemeDark) {
            darkColors(primary = androidGreen, secondary = androidBlue)
        } else {
            lightColors(primary = androidBlue, secondary = androidGreen)
        }
    ) {
        Column {
            TopAppBar(
                title = { Text("SVG to ImageVector conversion tool") },
                actions = {
                    IconButton(
                        onClick = { bloc.addEvent(MainWindowEvent.ToggleThemeButtonClicked) }
                    ) { Icon(imageVector = CustomIcons.ToggleTheme) }
                    IconButton(
                        onClick = { /* TODO */ }
                    ) { Icon(imageVector = Icons.Outlined.Info) }
                }
            )
            Row(modifier = Modifier.background(color = MaterialTheme.colors.background)) {
                val scaffoldState = rememberScaffoldState()

                Providers(
                    BlocAmbient provides bloc,
                    ScaffoldStateAmbient provides scaffoldState
                ) {
                    // not an absolute necessity, but makes handling Snackbars easier,
                    // and allows "customization" of their width without visual "glitches",
                    // although it might just be me who hasn't figured out how to achieve this
                    Scaffold(
                        modifier = Modifier.fillMaxWidth(2F / 3F).fillMaxHeight(),
                        scaffoldState = scaffoldState
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            FileSystemEntitySelectionField(
                                FileSystemEntitySelectionMode.SOURCE,
                                state.sourceFilesSelectionTextFieldState.value,
                                state.sourceFilesSelectionTextFieldState.isErrorValue,
                            )
                            FileSystemEntitySelectionField(
                                FileSystemEntitySelectionMode.DESTINATION,
                                state.destinationDirectorySelectionTextFieldState.value,
                                state.destinationDirectorySelectionTextFieldState.isErrorValue,
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
                        Image(
                            imageVector = state.preview,
                            modifier = Modifier.fillMaxSize(0.7F),
                            colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground)
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
                            icon = { Icon(imageVector = CustomIcons.ConvertVector) }
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
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.preferredSize(48.dp),
        enabled = isEnabled,
        shape = RoundedCornerShape(percent = 50),
        contentPadding = PaddingValues(12.dp)
    ) {
        Icon(imageVector = icon)
    }
}

@Composable
@ObsoleteCoroutinesApi
@Suppress("FunctionName")
private fun FileSystemEntitySelectionField(
    mode: FileSystemEntitySelectionMode,
    value: String,
    isErrorValue: Boolean
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
            value = value,
            onValueChange = { /* it is read-only, we have control over the value */ },
            modifier = Modifier.weight(1F),
            readOnly = true,
            label = { Text(label) },
            isErrorValue = isErrorValue,
            singleLine = true,
            leadingIcon = { Icon(imageVector = leadingIcon) }
        )

        val window = AppWindowAmbient.current!!.window
        val bloc = BlocAmbient.current
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
            modifier = Modifier.preferredHeight(56.dp).padding(start = 8.dp)
        ) {
            Icon(imageVector = CustomIcons.ExploreFiles)
        }
    }
}

/*
@Composable
@ExperimentalMaterialApi
suspend fun launchEffect(effect: MainWindowEffect) {
    when (effect) {
        is ShowSnackbar -> {
            val (message, actionLabel, duration) = effect
            ScaffoldStateAmbient.current.snackbarHostState
                .showSnackbar(message, actionLabel, duration)
        }
    }
}
*/
