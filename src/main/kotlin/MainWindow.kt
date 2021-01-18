import androidx.compose.desktop.AppWindowAmbient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.rememberScaffoldState
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val BlocAmbient = ambientOf<MainWindowBloc>()

private val ScaffoldStateAmbient = ambientOf<ScaffoldState>()

@Composable
@ExperimentalCoroutinesApi
@ExperimentalLayout
@ExperimentalMaterialApi
@ObsoleteCoroutinesApi
@Suppress("FunctionName")
fun MainWindowContent(mainWindowBloc: MainWindowBloc) {
    MaterialTheme(
        colors = darkColors(primary = Color(0xFF00DE7A), secondary = Color(0xFF2196F3))
    ) {
        Column {
            TopAppBar(
                title = { Text("SVG to ImageVector conversion tool") },
                actions = { IconButton(onClick = {}) { Icon(imageVector = Icons.Outlined.Info) } }
            )
            Row(modifier = Modifier.background(color = MaterialTheme.colors.background)) {
                val scaffoldState = rememberScaffoldState()
                val state = mainWindowBloc.state.collectAsState().value

                Providers(
                    BlocAmbient provides mainWindowBloc,
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
                                value = "",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Extension receiver (optional)") },
                                singleLine = true,
                                placeholder = { Text("") }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = state.isAllInOneCheckboxChecked,
                                    onCheckedChange = { isChecked ->
                                        mainWindowBloc.addEvent(
                                            AllInOneCheckboxClicked(isChecked)
                                        )
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
                            imageVector = Icons.Outlined.Build,
                            modifier = Modifier.fillMaxSize(0.7F),
                            colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground)
                        )
                        PreviewSelectionButton(
                            icon = Icons.Outlined.ArrowBack,
                            modifier = Modifier.align(Alignment.CenterStart),
                            isEnabled = false
                        )
                        PreviewSelectionButton(
                            icon = Icons.Outlined.ArrowForward,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            isEnabled = false
                        )
                        ExtendedFloatingActionButton(
                            text = { Text("Convert") },
                            onClick = { mainWindowBloc.addEvent(ConvertButtonClicked) },
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
    modifier: Modifier,
    isEnabled: Boolean = true
) {
    OutlinedButton(
        onClick = {},
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
                                ?.let { SourceFilesSelected(files) }
                        FileSystemEntitySelectionMode.DESTINATION -> {
                            files.singleOrNull()
                                ?.let { directory -> DestinationDirectorySelected(directory) }
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

private fun openFileChooser(
    parent: Frame,
    mode: FileSystemEntitySelectionMode
): Array<File> {
    return if (mode == FileSystemEntitySelectionMode.SOURCE)
        FileDialog(parent).apply {
            file = "*.svg" // for Windows
            setFilenameFilter { _, name -> name.endsWith(".svg") } // for other OSes
            isMultipleMode = true
            isVisible = true
        }.files ?: emptyArray()
    else {
        with(JFileChooser()) {
            when (mode) {
                // not reachable, the multi-selection "chooser" is too confusing IMO
                FileSystemEntitySelectionMode.SOURCE -> {
                    fileFilter = FileNameExtensionFilter("SVG Files", "svg")
                    isMultiSelectionEnabled = true
                }
                FileSystemEntitySelectionMode.DESTINATION -> {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                }
            }
            showDialog(parent, "Select")
            return@with selectedFiles?.takeIf { it.isNotEmpty() }
                ?: selectedFile?.let { arrayOf(it) } ?: emptyArray()
        }
    }
}

private enum class FileSystemEntitySelectionMode { SOURCE, DESTINATION }
