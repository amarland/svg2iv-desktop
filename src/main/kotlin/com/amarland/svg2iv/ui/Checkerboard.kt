package com.amarland.svg2iv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp

private const val SQUARE_SIZE_IN_DP = 8

@Composable
fun Checkerboard(
    modifier: Modifier = Modifier,
    oddSquareColor: Color = Color.Unspecified,
    evenSquareColor: Color = Color.Unspecified
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val maxWidth = constraints.maxWidth
        val maxHeight = constraints.maxHeight

        val oddSquareActualColor =
            if (oddSquareColor.isSpecified) oddSquareColor
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5F)
        val evenSquareActualColor =
            if (evenSquareColor.isSpecified) evenSquareColor
            else MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.5F)

        Canvas(
            modifier = Modifier.size(
                width = (maxWidth - maxWidth % SQUARE_SIZE_IN_DP).dp,
                height = (maxHeight - maxHeight % SQUARE_SIZE_IN_DP).dp
            )
        ) {
            val squareSizeInPixels = SQUARE_SIZE_IN_DP.dp.toPx()
            var x = 0F
            var y = 0F
            var odd = true

            while (y < size.height) {
                drawRect(
                    topLeft = Offset(x, y),
                    size = Size(squareSizeInPixels, squareSizeInPixels),
                    color = if (odd) oddSquareActualColor else evenSquareActualColor,
                )

                if (x + squareSizeInPixels < size.width) {
                    x += squareSizeInPixels
                } else {
                    x = (y + squareSizeInPixels) % (squareSizeInPixels)
                    y += squareSizeInPixels
                }
                odd = !odd
            }
        }
    }
}
