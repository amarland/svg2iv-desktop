package com.amarland.svg2iv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
@Suppress("FunctionName")
fun Checkerboard(
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

                x = if (x < size.width - squareSizeInPixels * 2) x + squareSizeInPixels * 2
                else (y + squareSizeInPixels) % (squareSizeInPixels * 2)

                if (x <= squareSizeInPixels) y += squareSizeInPixels
            }
        }
    }
}
