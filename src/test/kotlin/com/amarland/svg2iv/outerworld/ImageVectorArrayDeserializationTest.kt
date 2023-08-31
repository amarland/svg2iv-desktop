package com.amarland.svg2iv.outerworld

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborReader
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImageVectorArrayDeserializationTest {

    @Test
    fun fromCbor() {
        @Language("JSON")
        val sourceJsonString = """
            |[
            |    {
            |        "vectorName": "does_not_matter",
            |        "viewportWidth": 304.0,
            |        "viewportHeight": 290.0,
            |        "width": 608.0,
            |        "height": 580.0,
            |        "tintColor": ${Color.LightGray.toArgb()},
            |        "tintBlendMode": 3,
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
            |                       "command": 0,
            |                       "arguments": [27.5, 27.5]
            |                   },
            |                   {
            |                       "command": 1,
            |                       "arguments": [276.5, 27.5]
            |                   },
            |                   {
            |                       "command": 2
            |                   }
            |                   {
            |                       "command": 1,
            |                       "arguments": [276.5, 262.5]
            |                   },
            |                   {
            |                       "command": 1,
            |                       "arguments": [27.5, 262.5]
            |                   },
            |                   {
            |                       "command": 4
            |                   }
            |                ],
            |                "nodes": [
            |                   {
            |                       "pathNodes": [
            |                           {
            |                               "command": 1,
            |                               "arguments": [2, 111]
            |                           },
            |                           {
            |                               "command": 1,
            |                               "arguments": [300, 111]
            |                           },
            |                           {
            |                               "command": 1,
            |                               "arguments": [57.3, 287.3]
            |                           },
            |                           {
            |                               "command": 1,
            |                               "arguments": [150, 2]
            |                           },
            |                           {
            |                               "command": 1,
            |                               "arguments": [242.7, 287.3]
            |                           },
            |                           {
            |                               "command": 4
            |                           }
            |                       ],
            |                       "fillType": 1,
            |                       "pathName": "neither_does_it_here",
            |                       "fill": {
            |                           "isLinear": false,
            |                           "colors": [
            |                               ${Color.Red.toArgb()},
            |                               ${Color.Green.toArgb()},
            |                               ${Color.Blue.toArgb()}
            |                           ],
            |                           "stops": [0.25, 0.5, 0.75],
            |                           "centerX": 100.001,
            |                           "centerY": 100.001,
            |                           "radius": 20.0,
            |                           "tileMode": 2
            |                       },
            |                       "fillAlpha": 0.9125,
            |                       "stroke": ${0xE1808080},
            |                       "strokeAlpha": 1.0,
            |                       "strokeLineWidth": 15.0,
            |                       "strokeLineCap": 0,
            |                       "strokeLineJoin": 1,
            |                       "strokeLineMiter": 4.6667
            |                   }
            |                ]
            |            },
            |            {
            |               "pathNodes": [
            |                   {
            |                       "command": 0,
            |                       "arguments": [122.55, 80.325]
            |                   },
            |                   {
            |                       "command": 3,
            |                       "arguments": [45, 45, 0, 1, 0, 167.25, 125.325]
            |                   },
            |                   {
            |                       "command": 1,
            |                       "arguments": [167.25, 80.325]
            |                   },
            |                   {
            |                       "command": 37,
            |                       "arguments": [13.37, 123.45]
            |                   },
            |                   {
            |                       "command": 4
            |                   }
            |               ],
            |               "fill": ${Color(85, 170, 255, 127).toArgb()}
            |            }
            |        ]
            |    },
            |    null
            |]
        """.trimMargin()
        val sourceCborArray = CborArray.createFromJSONArray(JSONArray(sourceJsonString))
        val sourceCborByteArray = sourceCborArray.toCborByteArray()

        @Suppress("BooleanLiteralArgument")
        val expectedImageVectors = listOf(
            ImageVector.Builder(
                name = "does_not_matter",
                defaultWidth = 608.dp,
                defaultHeight = 580.dp,
                viewportWidth = 304F,
                viewportHeight = 290F,
                tintColor = Color.LightGray,
                tintBlendMode = BlendMode.Modulate
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
                    PathNode.LineTo(276.5F, 27.5F),
                    PathNode.LineTo(276.5F, 262.5F),
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
                        radius = 20F,
                        tileMode = TileMode.Mirror
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
                    lineTo(300F, 111F)
                    lineTo(57.3F, 287.3F)
                    lineTo(150F, 2F)
                    lineTo(242.7F, 287.3F)
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

        val actualImageVectors = ImageVector.fromCbor(
            CborReader.createFromByteArray(sourceCborByteArray)
        )

        fun Iterable<ImageVector?>.stringify() =
            filterNotNull().map { imageVector ->
                getCodeBlockForImageVector(imageVector).toString()
            }

        assertEquals(expectedImageVectors.stringify(), actualImageVectors.stringify())
    }
}
