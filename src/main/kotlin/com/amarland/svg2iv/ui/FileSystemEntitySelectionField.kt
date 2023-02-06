package com.amarland.svg2iv.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.amarland.svg2iv.outerworld.FileSystemEntitySelectionMode
import com.amarland.svg2iv.util.asMnemonic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSystemEntitySelectionField(
    onButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: FileSystemEntitySelectionMode,
    value: String = "",
    isError: Boolean = false,
    isButtonEnabled: Boolean = true
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

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedIconButton(
            onButtonClicked,
            modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp),
            enabled = isButtonEnabled,
            shape = TextFieldDefaults.outlinedShape,
            colors = IconButtonDefaults.outlinedIconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(imageVector = CustomIcons.ExploreFiles, contentDescription = null)
        }
    }
}
