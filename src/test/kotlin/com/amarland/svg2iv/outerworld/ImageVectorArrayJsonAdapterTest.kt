package com.amarland.svg2iv.outerworld

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ImageVectorArrayJsonAdapterTest {

    @Test
    fun fromJson() {
        @Language("JSON")
        val sourceJson = """
            |[
            |    {
            |        "vectorName": "does_not_matter",
            |        "viewportWidth": 304.0,
            |        "viewportHeight": 290.0,
            |        "nodes": [
            |            {
            |                "groupName": "still_does_not",
            |                "rotation": 45.0,
            |                "pivotX": 120.0,
            |                "pivotY": 120.0,
            |                "scaleX": 0.6,
            |                "scaleY": 0.6,
            |                "translationX": 50.0,
            |                "translationY": 50.0,
            |                "clipPathData": [
            |                   {
            |                       "command": "moveTo",
            |                       "arguments": [27.5, 27.5]
            |                   },
            |                   {
            |                       "command": "horizontalLineTo",
            |                       "arguments": [276.5]
            |                   },
            |                   {
            |                       "command": "verticalLineTo",
            |                       "arguments": [262.5]
            |                   },
            |                   {
            |                       "command": "lineTo",
            |                       "arguments": [27.5, 262.5]
            |                   },
            |                   {
            |                       "command": "close",
            |                       "arguments": null
            |                   }
            |                ],
            |                "nodes": [
            |                   {
            |                       "pathNodes": [
            |                           {
            |                               "command": "moveTo",
            |                               "arguments": [2, 111]
            |                           },
            |                           {
            |                               "command": "horizontalLineTo",
            |                               "arguments": [300]
            |                           },
            |                           {
            |                               "command": "relativeLineTo",
            |                               "arguments": [-242.7, 176.3]
            |                           },
            |                           {
            |                               "command": "relativeLineTo",
            |                               "arguments": [92.7, -285.3]
            |                           },
            |                           {
            |                               "command": "relativeLineTo",
            |                               "arguments": [92.7, 285.3]
            |                           },
            |                           {
            |                               "command": "close",
            |                               "arguments": null
            |                           }
            |                       ],
            |                       "fillType": "evenOdd",
            |                       "pathName": "neither_does_it_here",
            |                       "fill": {
            |                           "type": "radial",
            |                           "colors": [
            |                               [255, 255, 0, 0],
            |                               [255, 0, 255, 0],
            |                               [255, 0, 0, 255]
            |                           ],
            |                           "stops": [0.25, 0.5, 0.75],
            |                           "centerX": 100.001,
            |                           "centerY": 100.001,
            |                           "radius": 20.0
            |                       },
            |                       "fillAlpha": 0.9125,
            |                       "stroke": [225, 128, 128, 128],
            |                       "strokeAlpha": 1.0,
            |                       "strokeLineWidth": 15.0,
            |                       "strokeLineCap": "butt",
            |                       "strokeLineJoin": "miter",
            |                       "strokeLineMiter": 4.6667
            |                   }
            |                ]
            |            },
            |            {
            |               "pathNodes": [
            |                   {
            |                       "command": "moveTo",
            |                       "arguments": [122.55, 80.325]
            |                   },
            |                   {
            |                       "command": "arcTo",
            |                       "arguments": [45, 45, 0, true, false, 167.25, 125.325]
            |                   },
            |                   {
            |                       "command": "lineTo",
            |                       "arguments": [167.25, 80.325]
            |                   },
            |                   {
            |                       "command": "close",
            |                       "arguments": []
            |                   }
            |               ],
            |               "fill": [127, 85, 170, 255]
            |            }
            |        ]
            |    },
            |    null
            |]
        """.trimMargin()

        @Suppress("BooleanLiteralArgument")
        val expectedImageVectors = listOf(
            ImageVector.Builder(
                name = "does_not_matter",
                defaultWidth = 304.dp,
                defaultHeight = 290.dp,
                viewportWidth = 304F,
                viewportHeight = 290F
            ).group(
                name = "still_does_not",
                rotate = 45F,
                pivotX = 120F,
                pivotY = 120F,
                scaleX = 0.6F,
                scaleY = 0.6F,
                translationX = 50F,
                translationY = 50F,
                clipPathData = listOf(
                    PathNode.MoveTo(27.5F, 27.5F),
                    PathNode.HorizontalTo(276.5F),
                    PathNode.VerticalTo(262.5F),
                    PathNode.LineTo(27.5F, 262.5F),
                    PathNode.Close
                )
            ) {
                path(
                    name = "neither_does_it_here",
                    fill = Brush.radialGradient(
                        0.25F to Color.Red,
                        0.5F to Color.Green,
                        0.75F to Color.Blue,
                        center = Offset(100.001F, 100.001F),
                        radius = 20F
                    ),
                    fillAlpha = 0.9125F,
                    stroke = SolidColor(Color(0xE1808080)),
                    strokeLineWidth = 15F,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 4.6667F,
                    pathFillType = PathFillType.EvenOdd
                ) {
                    moveTo(2F, 111F)
                    horizontalLineTo(300F)
                    lineToRelative(-242.7F, 176.3F)
                    lineToRelative(92.7F, -285.3F)
                    lineToRelative(92.7F, 285.3F)
                    close()
                }
            }.path(fill = SolidColor(Color(85, 170, 255, 127))) {
                moveTo(122.55F, 80.325F)
                arcTo(45F, 45F, 0F, true, false, 167.25F, 125.325F)
                lineTo(167.25F, 80.325F)
                close()
            }.build(),
            null
        )

        val actualImageVectors = ImageVectorArrayJsonAdapter().fromJson(sourceJson)
        Assertions.assertIterableEquals(expectedImageVectors, actualImageVectors)
    }
}
