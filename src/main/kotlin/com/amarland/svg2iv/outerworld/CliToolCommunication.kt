package com.amarland.svg2iv.outerworld

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp
import com.amarland.svg2iv.util.RingBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.Reader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import com.amarland.svg2iv.outerworld.ProtobufImageVector as _pb

@Suppress("BlockingMethodInNonBlockingContext") // TODO: use ServerSocketChannel?
@Throws(
    IOException::class,
    InterruptedException::class,
    SecurityException::class
)
suspend fun callCliTool(
    sourceFiles: Collection<File>,
    startProcess: (
        sourceFiles: Collection<File>,
        extensionReceiver: String?,
        serverSocketAddress: InetAddress,
        serverSocketPort: Int,
    ) -> Process = ::startCliToolProcess,
    extensionReceiver: String? = null
): Pair<List<ImageVector?>, List<String>> {
    require(sourceFiles.isNotEmpty())

    return withContext(Dispatchers.IO) {
        val serverSocket = ServerSocket(0, 1, InetAddress.getLoopbackAddress()).apply {
            soTimeout = 3000
        }
        val process = startProcess(
            sourceFiles,
            extensionReceiver,
            serverSocket.inetAddress,
            serverSocket.localPort
        )

        val imageVectors = ArrayList<ImageVector?>()
        val errorMessages = process.errorStream
            .bufferedReader()
            .readLines(to = RingBuffer.create(50)) // uses `use` internally

        try {
            serverSocket.accept().use { client ->
                client.getInputStream().use { stream ->
                    imageVectors += _pb.ImageVectorCollection
                        // "Despite usually reading the entire input,
                        // this does not close the stream."
                        .parseFrom(stream)
                        .toComposeModels()
                }
            }
        } catch (e: SocketTimeoutException) {
            // do nothing?
        } finally {
            serverSocket.close()
        }

        process.waitFor()

        return@withContext imageVectors to errorMessages
    }
}

@Throws(
    IOException::class,
    SecurityException::class
)
private fun startCliToolProcess(
    sourceFiles: Collection<File>,
    extensionReceiver: String?,
    serverSocketAddress: InetAddress,
    serverSocketPort: Int,
): Process {
    val isOSWindows = System.getProperty("os.name")
        ?.toLowerCase()
        ?.startsWith("windows")
        ?: false
    val shellInvocation = if (isOSWindows) "cmd.exe /c" else "sh"
    val commandOption = if (isOSWindows) "/c" else "-c"
    val executableName = "svg2iv.exe"
    val command = (if (File(executableName).exists()) "./$executableName" else executableName) +
            (if (extensionReceiver.isNullOrEmpty()) "" else " -r $extensionReceiver") +
            " -s ${serverSocketAddress.hostAddress}:$serverSocketPort" +
            " " + sourceFiles.joinToString(" ") { it.absolutePath }
    return Runtime.getRuntime().exec(arrayOf(shellInvocation, commandOption, command))
}

private fun _pb.ImageVectorCollection.toComposeModels(): List<ImageVector?> {
    return nullableImageVectorsList.map { nullableImageVector ->
        when (nullableImageVector.valueOrNothingCase) {
            _pb.NullableImageVector.ValueOrNothingCase.VALUE -> {
                val imageVector = nullableImageVector.value
                ImageVector.Builder(
                    name = imageVector.name.takeIf { it.isNotEmpty() } ?: DefaultGroupName,
                    defaultWidth = imageVector.viewportWidth.dp,
                    defaultHeight = imageVector.viewportHeight.dp,
                    viewportWidth = imageVector.width,
                    viewportHeight = imageVector.height
                )
                    .addNodes(imageVector.nodesList)
                    .build()
            }
            else -> null
        }
    }
}

private fun Reader.readLines(to: MutableList<String>): List<String> =
    to.apply { forEachLine { add(it) } }

private fun ImageVector.Builder.addNodes(nodes: Iterable<_pb.VectorNode>): ImageVector.Builder {
    for (node in nodes) {
        when (node.nodeCase) {
            _pb.VectorNode.NodeCase.GROUP -> addGroup(node.group)
            else -> addPath(node.path)
        }
    }
    return this
}

private fun ImageVector.Builder.addGroup(group: _pb.VectorGroup): ImageVector.Builder {
    group(
        name = group.id.takeIf { it.isNotEmpty() } ?: DefaultGroupName,
        rotate = group.rotation,
        pivotX = group.pivotX,
        pivotY = group.pivotY,
        scaleX = group.scaleX,
        scaleY = group.scaleY,
        translationX = group.translationX,
        translationY = group.translationY,
        clipPathData = group.clipPathDataList
            .takeIf { it.isNotEmpty() }
            ?.mapNotNull(::mapPathNode)
            ?: EmptyPath
    ) {
        addNodes(group.nodesList)
    }
    return this
}

private fun ImageVector.Builder.addPath(path: _pb.VectorPath): ImageVector.Builder {
    addPath(
        pathData = path.pathNodesList.mapNotNull(::mapPathNode),
        pathFillType = when (path.fillType) {
            _pb.VectorPath.FillType.EVEN_ODD -> PathFillType.EvenOdd
            _pb.VectorPath.FillType.NON_ZERO -> PathFillType.NonZero
            else -> DefaultFillType
        },
        name = path.id.takeIf { it.isNotEmpty() } ?: DefaultPathName,
        fill = mapBrush(path.fill),
        fillAlpha = path.fillAlpha,
        stroke = mapBrush(path.stroke),
        strokeAlpha = path.strokeAlpha,
        strokeLineWidth = path.strokeLineWidth,
        strokeLineCap = when (path.strokeLineCap) {
            _pb.VectorPath.StrokeCap.CAP_BUTT -> StrokeCap.Butt
            _pb.VectorPath.StrokeCap.CAP_ROUND -> StrokeCap.Round
            _pb.VectorPath.StrokeCap.CAP_SQUARE -> StrokeCap.Square
            else -> DefaultStrokeLineCap
        },
        strokeLineJoin = when (path.strokeLineJoin) {
            _pb.VectorPath.StrokeJoin.JOIN_BEVEL -> StrokeJoin.Bevel
            _pb.VectorPath.StrokeJoin.JOIN_MITER -> StrokeJoin.Miter
            _pb.VectorPath.StrokeJoin.JOIN_ROUND -> StrokeJoin.Round
            else -> DefaultStrokeLineJoin
        },
        strokeLineMiter = path.strokeLineMiter
    )
    return this
}

private fun mapPathNode(node: _pb.PathNode): PathNode? {
    val nodeClass = when (node.command) {
        _pb.PathNode.Command.CLOSE ->
            PathNode.Close::class
        _pb.PathNode.Command.MOVE_TO ->
            PathNode.MoveTo::class
        _pb.PathNode.Command.RELATIVE_MOVE_TO ->
            PathNode.RelativeMoveTo::class
        _pb.PathNode.Command.LINE_TO ->
            PathNode.LineTo::class
        _pb.PathNode.Command.RELATIVE_LINE_TO ->
            PathNode.RelativeLineTo::class
        _pb.PathNode.Command.HORIZONTAL_LINE_TO ->
            PathNode.HorizontalTo::class
        _pb.PathNode.Command.RELATIVE_HORIZONTAL_LINE_TO ->
            PathNode.RelativeHorizontalTo::class
        _pb.PathNode.Command.VERTICAL_LINE_TO ->
            PathNode.VerticalTo::class
        _pb.PathNode.Command.RELATIVE_VERTICAL_LINE_TO ->
            PathNode.RelativeVerticalTo::class
        _pb.PathNode.Command.CURVE_TO ->
            PathNode.CurveTo::class
        _pb.PathNode.Command.RELATIVE_CURVE_TO ->
            PathNode.RelativeCurveTo::class
        _pb.PathNode.Command.SMOOTH_CURVE_TO ->
            PathNode.ReflectiveCurveTo::class
        _pb.PathNode.Command.RELATIVE_SMOOTH_CURVE_TO ->
            PathNode.RelativeReflectiveCurveTo::class
        _pb.PathNode.Command.QUADRATIC_BEZIER_CURVE_TO ->
            PathNode.QuadTo::class
        _pb.PathNode.Command.RELATIVE_QUADRATIC_BEZIER_CURVE_TO ->
            PathNode.RelativeQuadTo::class
        _pb.PathNode.Command.SMOOTH_QUADRATIC_BEZIER_CURVE_TO ->
            PathNode.ReflectiveQuadTo::class
        _pb.PathNode.Command.RELATIVE_SMOOTH_QUADRATIC_BEZIER_CURVE_TO ->
            PathNode.RelativeReflectiveQuadTo::class
        _pb.PathNode.Command.ARC_TO ->
            PathNode.ArcTo::class
        _pb.PathNode.Command.RELATIVE_ARC_TO ->
            PathNode.RelativeArcTo::class
        else -> null
    }
    return nodeClass?.let {
        nodeClass.objectInstance ?: nodeClass.constructors.first().call(
            *node.argumentsList.map { argument ->
                when (argument.argumentCase) {
                    _pb.PathNode.Argument.ArgumentCase.FLAG -> argument.flag
                    else -> argument.coordinate
                }
            }.toTypedArray()
        )
    }
}

private fun mapBrush(brush: _pb.Brush): Brush? {
    val case = brush.solidColorOrGradientCase
    if (case == _pb.Brush.SolidColorOrGradientCase.SOLID_COLOR) {
        return SolidColor(Color(brush.solidColor))
    } else {
        fun _pb.Gradient.composeTileMode() =
            when (tileMode) {
                _pb.Gradient.TileMode.REPEATED -> TileMode.Repeated
                _pb.Gradient.TileMode.MIRROR -> TileMode.Mirror
                else -> TileMode.Clamp
            }

        fun _pb.Gradient.colorStops() =
            stopsList.zip(colorsList) { stop, colorInt -> stop to Color(colorInt) }.toTypedArray()

        val gradient: _pb.Gradient
        return when (case) {
            _pb.Brush.SolidColorOrGradientCase.LINEAR_GRADIENT -> {
                gradient = brush.linearGradient
                if (gradient.stopsCount > 0) {
                    Brush.linearGradient(
                        *gradient.colorStops(),
                        start = Offset(gradient.startX, gradient.startY),
                        end = Offset(gradient.endX, gradient.endY),
                        tileMode = gradient.composeTileMode()
                    )
                } else {
                    Brush.linearGradient(
                        gradient.colorsList.map { Color(it) },
                        start = Offset(gradient.startX, gradient.startY),
                        end = Offset(gradient.endX, gradient.endY),
                        tileMode = gradient.composeTileMode()
                    )
                }
            }
            _pb.Brush.SolidColorOrGradientCase.RADIAL_GRADIENT -> {
                gradient = brush.radialGradient
                if (gradient.stopsCount > 0) {
                    Brush.radialGradient(
                        *gradient.colorStops(),
                        center = Offset(gradient.centerX, gradient.centerY),
                        radius = gradient.radius,
                        tileMode = gradient.composeTileMode()
                    )
                } else {
                    Brush.radialGradient(
                        gradient.colorsList.map { Color(it) },
                        center = Offset(gradient.centerX, gradient.centerY),
                        radius = gradient.radius,
                        tileMode = gradient.composeTileMode()
                    )
                }
            }
            else -> null
        }
    }
}
