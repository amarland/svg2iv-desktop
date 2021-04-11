package com.amarland.svg2iv.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
@Suppress("FunctionName")
fun PreviewSelectionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    isEnabled: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(48.dp).then(modifier),
        enabled = isEnabled,
        shape = RoundedCornerShape(percent = 50),
        contentPadding = PaddingValues(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null)
    }
}
