package com.amarland.svg2iv.outerworld

import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.unit.dp
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.Socket
import com.amarland.svg2iv.outerworld.ProtobufImageVector as _pb

class CliToolCommunicationTest {

    @DisplayName(
        "startCliToolProcess returns ImageVectors converted from what is received" +
                " from the client socket and the error messages from the process"
    )
    @Test
    fun startCliToolProcess() {
        val sourceImageVectors = _pb.ImageVectorCollection.newBuilder()
            .addNullableImageVectors(
                _pb.NullableImageVector.newBuilder()
                    .setValue(
                        _pb.ImageVector.newBuilder()
                            .setName("test_vector")
                            .setViewportWidth(100F).setViewportHeight(50F)
                            .setWidth(100F).setHeight(50F)
                            .addNodes(
                                _pb.VectorNode.newBuilder().setGroup(
                                    _pb.VectorGroup.newBuilder()
                                        .setId("test_group")
                                        .setRotation(270F).setPivotX(25F).setPivotY(25F)
                                        .setScaleX(1.5F).setScaleY(1.5F)
                                        .setTranslationX(10F).setTranslationY(20F)
                                        .addClipPathData(
                                            _pb.PathNode.newBuilder()
                                                .setCommand(_pb.PathNode.Command.VERTICAL_LINE_TO)
                                                .addArguments(
                                                    _pb.PathNode.Argument.newBuilder()
                                                        .setCoordinate(75F)
                                                )
                                        )
                                        .addNodes(
                                            _pb.VectorNode.newBuilder().setPath(
                                                _pb.VectorPath.newBuilder()
                                                    .setId("test_path")
                                                    .addPathNodes(
                                                        _pb.PathNode.newBuilder()
                                                            .setCommand(
                                                                _pb.PathNode.Command
                                                                    .HORIZONTAL_LINE_TO
                                                            )
                                                            .addArguments(
                                                                _pb.PathNode.Argument.newBuilder()
                                                                    .setCoordinate(25F)
                                                            )
                                                    )
                                                    .setFill(
                                                        _pb.Brush.newBuilder()
                                                            .setLinearGradient(
                                                                _pb.Gradient.newBuilder()
                                                                    .addColors(Color.Red.toArgb())
                                                                    .addColors(Color.Green.toArgb())
                                                                    .addColors(Color.Blue.toArgb())
                                                                    .addStops(0.2F)
                                                                    .addStops(0.5F)
                                                                    .addStops(0.8F)
                                                                    .setStartX(10F)
                                                                    .setStartY(90F)
                                                                    .setEndX(20F)
                                                                    .setEndY(80F)
                                                                    .setTileMode(
                                                                        _pb.Gradient.TileMode.CLAMP
                                                                    )
                                                            )
                                                    )
                                                    .setFillAlpha(0.85F)
                                                    .setStroke(
                                                        _pb.Brush.newBuilder()
                                                            .setSolidColor(Color.DarkGray.toArgb())
                                                    )
                                                    .setStrokeAlpha(0.45F)
                                                    .setStrokeLineWidth(3F)
                                                    .setStrokeLineCap(
                                                        _pb.VectorPath.StrokeCap.CAP_BUTT
                                                    )
                                                    .setStrokeLineJoin(
                                                        _pb.VectorPath.StrokeJoin.JOIN_MITER
                                                    )
                                                    .setStrokeLineMiter(0.6F)
                                                    .setFillType(_pb.VectorPath.FillType.NON_ZERO)
                                            )
                                        )
                                )
                            )
                    )
            ).build()
        val errorMessages = listOf(
            "Error message #1",
            "Error message #2",
            "Error message #3"
        )

        val expectedImageVectors = listOf(
            ImageVector.Builder(
                name = "test_vector",
                defaultWidth = 100F.dp, defaultHeight = 50F.dp,
                viewportWidth = 100F, viewportHeight = 50F
            )
                .addGroup(
                    name = "test_group",
                    rotate = 270F, pivotX = 25F, pivotY = 25F,
                    scaleX = 1.5F, scaleY = 1.5F,
                    translationX = 10F, translationY = 20F,
                    clipPathData = listOf(PathNode.VerticalTo(75F))
                )
                .addPath(
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
                    stroke = SolidColor(Color.DarkGray),
                    strokeAlpha = 0.45F,
                    strokeLineWidth = 3F,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 0.6F
                )
                .build()
        )

        val errorMessageStream = errorMessages
            .joinToString(System.lineSeparator())
            .byteInputStream()

        val (actualImageVectors, actualErrorMessages) = runBlocking {
            callCliTool(
                sourceFiles = listOf(mock()),
                extensionReceiver = null,
                startProcess = { _, _, address, port ->
                    mock<Process> {
                        on { errorStream } doReturn errorMessageStream
                        on { waitFor() } doReturn 0
                    }.also {
                        Socket(address, port).use { client ->
                            sourceImageVectors.writeTo(client.getOutputStream())
                        }
                    }
                }
            )
        }

        Assertions.assertIterableEquals(expectedImageVectors, actualImageVectors)
        Assertions.assertIterableEquals(errorMessages, actualErrorMessages)
    }
}
