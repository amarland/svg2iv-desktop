package com.amarland.svg2iv.outerworld

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CliToolCommunicationTest {

    @DisplayName(
        "startCliToolProcess returns ImageVectors converted from what is received" +
                " and the error messages from the process"
    )
    @Test
    fun startCliToolProcess() {
        @Language("JSON")
        val sourceJson = """
            |[
            |    {
            |        "vectorName": "test_vector",
            |        "width": 200.0,
            |        "height": 200.0,
            |        "viewportWidth": 100.0,
            |        "viewportHeight": 100.0,
            |        "nodes": [
            |            {
            |                "groupName": "test_group",
            |                "rotation": 270.0,
            |                "pivotX": 25.0,
            |                "pivotY": 25.0,
            |                "scaleX": 1.5,
            |                "scaleY": 1.5,
            |                "translationX": 10.0,
            |                "translationY": 20.0,
            |                "clipPathData": [
            |                   {
            |                       "command": "verticalLineTo",
            |                       "arguments": [75.0]
            |                   }
            |                ],
            |                "nodes": [
            |                   {
            |                       "pathNodes": [
            |                           {
            |                               "command": "horizontalLineTo",
            |                               "arguments": [25.0]
            |                           }
            |                       ],
            |                       "fillType": "nonZero",
            |                       "pathName": "test_path",
            |                       "fill": {
            |                           "type": "linear",
            |                           "colors": [
            |                               [255, 255, 0, 0],
            |                               [255, 0, 255, 0],
            |                               [255, 0, 0, 255]
            |                           ],
            |                           "stops": [0.2, 0.5, 0.8],
            |                           "startX": 10.0,
            |                           "startY": 90.0,
            |                           "endX": 20.0,
            |                           "endY": 80.0,
            |                           "tileMode": "clamp"
            |                       },
            |                       "fillAlpha": 0.85,
            |                       "stroke": [255, 68, 68, 68],
            |                       "strokeAlpha": 0.45,
            |                       "strokeLineWidth": 3.0,
            |                       "strokeLineCap": "butt",
            |                       "strokeLineJoin": "miter",
            |                       "strokeLineMiter": 0.6
            |                   }
            |                ]
            |            }
            |        ]
            |    },
            |    null
            |]
        """.trimMargin()

        val errorMessages = listOf(
            "Error message #1",
            "Error message #2",
            "Error message #3"
        )

        val expectedImageVectors = listOf(
            ImageVector.Builder(
                name = "test_vector",
                defaultWidth = 200F.dp, defaultHeight = 200F.dp,
                viewportWidth = 100F, viewportHeight = 100F
            ).group(
                name = "test_group",
                rotate = 270F, pivotX = 25F, pivotY = 25F,
                scaleX = 1.5F, scaleY = 1.5F,
                translationX = 10F, translationY = 20F,
                clipPathData = listOf(PathNode.VerticalTo(75F))
            ) {
                addPath(
                    pathData = listOf(PathNode.HorizontalTo(25F)),
                    pathFillType = PathFillType.NonZero,
                    name = "test_path",
                    fill = Brush.linearGradient(
                        0.2F to Color.Red,
                        0.5F to Color.Green,
                        0.8F to Color.Blue,
                        start = Offset(10F, 90F), end = Offset(20F, 80F),
                        tileMode = TileMode.Clamp
                    ),
                    fillAlpha = 0.85F,
                    stroke = SolidColor(Color(68, 68, 68)),
                    strokeAlpha = 0.45F,
                    strokeLineWidth = 3F,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 0.6F
                )
            }.build(),
            null
        )

        val imageVectorStream = sourceJson.byteInputStream()

        val errorMessageStream = errorMessages
            .joinToString(System.lineSeparator())
            .byteInputStream()

        val actualErrorMessages = ArrayList<String>(errorMessages.size)
        val actualImageVectors = runBlocking {
            callCliTool(
                sourceFilePaths = listOf("/path/to/source/file"),
                extensionReceiver = null,
                startProcess = { _, _ ->
                    mock {
                        on { inputStream } doReturn imageVectorStream
                        on { errorStream } doReturn errorMessageStream
                        on { waitFor() } doReturn 0
                    }
                },
                doWithErrorMessages = { messageReader ->
                    messageReader.useLines(actualErrorMessages::addAll)
                }
            )
        }

        Assertions.assertIterableEquals(expectedImageVectors, actualImageVectors)
        Assertions.assertIterableEquals(errorMessages, actualErrorMessages)
    }
}
