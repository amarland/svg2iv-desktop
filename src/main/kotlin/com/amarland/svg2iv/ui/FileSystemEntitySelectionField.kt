package com.amarland.svg2iv.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.amarland.svg2iv.outerworld.FileSystemEntitySelectionMode
import com.amarland.svg2iv.state.MainWindowEvent
import com.amarland.svg2iv.util.asMnemonic

@Composable
@Suppress("FunctionName")
fun FileSystemEntitySelectionField(
    modifier: Modifier = Modifier,
    selectionMode: FileSystemEntitySelectionMode,
    value: String,
    isError: Boolean,
    isButtonEnabled: Boolean
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        val label: AnnotatedString
        val leadingIcon: ImageVector
        when (selectionMode) {
            FileSystemEntitySelectionMode.SOURCE -> {
                label = "Source file(s)".asMnemonic()
                leadingIcon = CustomIcons.SourceFiles
            }
            FileSystemEntitySelectionMode.DESTINATION -> {
                label = "Destination directory".asMnemonic()
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

        val bloc = LocalBloc.current
        OutlinedButton(
            onClick = {
                bloc.addEvent(
                    when (selectionMode) {
                        FileSystemEntitySelectionMode.SOURCE ->
                            MainWindowEvent.SelectSourceFilesButtonClicked

                        FileSystemEntitySelectionMode.DESTINATION ->
                            MainWindowEvent.SelectDestinationDirectoryButtonClicked
                    }
                )
            },
            modifier = Modifier.height(56.dp).padding(start = 8.dp),
            enabled = isButtonEnabled
        ) {
            Icon(imageVector = CustomIcons.ExploreFiles, contentDescription = null)
        }
    }
}
