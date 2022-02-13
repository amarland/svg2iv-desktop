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
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonReader.Options
import com.squareup.moshi.JsonReader.Token
import com.squareup.moshi.JsonWriter

private typealias ImageVectorBuilderOperation = (ImageVector.Builder) -> Unit

class ImageVectorArrayJsonAdapter : JsonAdapter<List<ImageVector?>>() {

    override fun fromJson(reader: JsonReader): List<ImageVector?> = with(reader) {
        beginArray()
        if (!hasNext()) return@with emptyList<ImageVector?>()

        val imageVectors = mutableListOf<ImageVector?>()
        while (hasNext()) {
            if (peek() == Token.NULL) {
                imageVectors += null
                continue
            }

            var name = DefaultGroupName
            var viewportWidth = 0F
            var viewportHeight = 0F
            var width: Float? = null
            var height: Float? = null
            var tintColor = Color.Unspecified
            var tintBlendMode = DefaultTintBlendMode
            var operations: List<ImageVectorBuilderOperation> = emptyList()

            beginObject()
            while (hasNext()) {
                when (selectName(IMAGE_VECTOR_OPTIONS)) {
                    0 -> name = nextString()
                    1 -> viewportWidth = nextFloat()
                    2 -> viewportHeight = nextFloat()
                    3 -> width = nextFloat()
                    4 -> height = nextFloat()
                    5 -> tintColor = nextColor()
                    6 -> tintBlendMode = when (nextString()) {
                        "srcOver" -> BlendMode.SrcOver
                        "srcAtop" -> BlendMode.SrcAtop
                        "modulate" -> BlendMode.Modulate
                        "screen" -> BlendMode.Screen
                        "plus" -> BlendMode.Plus
                        else -> BlendMode.SrcIn
                    }
                    7 -> operations = readVectorNodes()
                }
            }
            endObject()

            imageVectors += ImageVector.Builder(
                name,
                defaultWidth = (width ?: viewportWidth).dp,
                defaultHeight = (height ?: viewportHeight).dp,
                viewportWidth, viewportHeight,
                tintColor, tintBlendMode
            ).apply { applyOperations(operations) }.build()
        }
        endArray()

        return@with imageVectors
    }

    override fun toJson(writer: JsonWriter, value: List<ImageVector?>?) =
        throw NotImplementedError()

    private fun JsonReader.readVectorNodes(): List<ImageVectorBuilderOperation> {
        beginArray()
        val operations = mutableListOf<ImageVectorBuilderOperation>()
        while (hasNext()) {
            beginObject()
            val isVectorGroup = peekJson().nextName() in VECTOR_GROUP_OPTIONS.strings()
            operations += if (isVectorGroup) readVectorGroup() else readVectorPath()
            endObject()
        }
        endArray()
        return operations
    }

    private fun JsonReader.readVectorGroup(): ImageVectorBuilderOperation {
        var name = DefaultGroupName
        var rotation = DefaultRotation
        var pivotX = DefaultPivotX
        var pivotY = DefaultPivotY
        var scaleX = DefaultScaleX
        var scaleY = DefaultScaleY
        var translationX = DefaultTranslationX
        var translationY = DefaultTranslationY
        var clipPathData = EmptyPath
        var operations = emptyList<ImageVectorBuilderOperation>()

        while (hasNext()) {
            when (selectName(VECTOR_GROUP_OPTIONS)) {
                0 -> name = nextString()
                1 -> rotation = nextFloat()
                2 -> pivotX = nextFloat()
                3 -> pivotY = nextFloat()
                4 -> scaleX = nextFloat()
                5 -> scaleY = nextFloat()
                6 -> translationX = nextFloat()
                7 -> translationY = nextFloat()
                8 -> clipPathData = nextPathNodes()
                9 -> operations = readVectorNodes()
            }
        }

        return { builder: ImageVector.Builder ->
            builder.group(
                name,
                rotation,
                pivotX, pivotY,
                scaleX, scaleY,
                translationX, translationY,
                clipPathData
            ) { applyOperations(operations) }
        }
    }

    private fun JsonReader.readVectorPath(): ImageVectorBuilderOperation {
        var name = DefaultPathName
        var fill: Brush = SolidColor(Color.Black)
        var fillAlpha = 1F
        var stroke: Brush? = null
        var strokeAlpha = 1F
        var strokeLineWidth = DefaultStrokeLineWidth
        var strokeLineCap = DefaultStrokeLineCap
        var strokeLineJoin = DefaultStrokeLineJoin
        var strokeLineMiter = DefaultStrokeLineMiter
        var fillType = DefaultFillType
        var trimPathStart = DefaultTrimPathStart
        var trimPathEnd = DefaultTrimPathEnd
        var trimPathOffset = DefaultTrimPathOffset
        var pathNodes = EmptyPath

        while (hasNext()) {
            when (selectName(VECTOR_PATH_OPTIONS)) {
                0 -> name = nextString()
                1 -> fill = nextBrush()
                2 -> fillAlpha = nextFloat()
                3 -> stroke = nextBrush()
                4 -> strokeAlpha = nextFloat()
                5 -> strokeLineWidth = nextFloat()
                6 -> strokeLineCap = when (nextString()) {
                    "butt" -> StrokeCap.Butt
                    "round" -> StrokeCap.Round
                    "square" -> StrokeCap.Square
                    else -> DefaultStrokeLineCap
                }
                7 -> strokeLineJoin = when (nextString()) {
                    "bevel" -> StrokeJoin.Bevel
                    "miter" -> StrokeJoin.Miter
                    "round" -> StrokeJoin.Round
                    else -> DefaultStrokeLineJoin
                }
                8 -> strokeLineMiter = nextFloat()
                9 -> fillType = when (nextString()) {
                    "nonZero" -> PathFillType.NonZero
                    "evenOdd" -> PathFillType.EvenOdd
                    else -> DefaultFillType
                }
                10 -> trimPathStart = nextFloat()
                11 -> trimPathEnd = nextFloat()
                12 -> trimPathOffset = nextFloat()
                13 -> pathNodes = nextPathNodes()
            }
        }

        return { builder: ImageVector.Builder ->
            builder.addPath(
                pathNodes,
                fillType,
                name,
                fill, fillAlpha,
                stroke, strokeAlpha,
                strokeLineWidth, strokeLineCap, strokeLineJoin, strokeLineMiter,
                trimPathStart, trimPathEnd, trimPathOffset
            )
        }
    }

    private fun JsonReader.nextPathNodes(): List<PathNode> {
        beginArray()
        val nodes = mutableListOf<PathNode>()
        while (hasNext()) {
            var commandName: String? = null
            val arguments = mutableListOf<Any>()
            beginObject()
            when (selectName(PATH_NODE_OPTIONS)) {
                0 -> commandName = nextString()
                1 -> {
                    beginArray()
                    while (hasNext()) {
                        arguments += if (peek() == Token.BOOLEAN) nextBoolean() else nextFloat()
                    }
                    endArray()
                }
            }
            if (commandName.isNullOrBlank()) {
                throw JsonDataException("${PATH_NODE_OPTIONS.strings()[1]}: $commandName")
            }
            nodes += getPathNodeForCommandNameAndArguments(commandName, arguments)
        }
        endArray()
        return nodes
    }

    private fun JsonReader.nextBrush(): Brush {
        if (peek() == Token.BEGIN_ARRAY) {
            return SolidColor(nextColor())
        } else {
            var isLinear = true
            val colors = mutableListOf<Color>()
            val stops = mutableListOf<Float>()
            var startX = 0F
            var startY = Float.POSITIVE_INFINITY
            var endX = 0F
            var endY = Float.POSITIVE_INFINITY
            var centerX = Float.NaN
            var centerY = Float.NaN
            var radius = Float.POSITIVE_INFINITY
            var tileMode = TileMode.Clamp

            beginObject()
            while (hasNext()) {
                when (selectName(GRADIENT_OPTIONS)) {
                    0 -> isLinear = nextString() == "linear"
                    1 -> {
                        beginArray()
                        while (hasNext()) colors += nextColor()
                        endArray()
                    }
                    2 -> {
                        beginArray()
                        while (hasNext()) stops += nextFloat()
                        endArray()
                    }
                    3 -> startX = nextFloat()
                    4 -> startY = nextFloat()
                    5 -> endX = nextFloat()
                    6 -> endY = nextFloat()
                    7 -> centerX = nextFloat()
                    8 -> centerY = nextFloat()
                    9 -> radius = nextFloat()
                    10 -> tileMode = when (nextString()) {
                        "repeated" -> TileMode.Repeated
                        "mirror" -> TileMode.Mirror
                        else -> TileMode.Clamp
                    }
                }
            }
            endObject()

            val colorCount = colors.size
            if (stops.isEmpty()) {
                colors.indices.mapTo(stops) { index -> index.toFloat() / colorCount }
            }
            val colorStops = Array(colorCount) { index -> stops[index] to colors[index] }
            return if (isLinear) {
                Brush.linearGradient(
                    *colorStops,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    tileMode = tileMode
                )
            } else {
                Brush.radialGradient(
                    *colorStops,
                    center = Offset(centerX, centerY),
                    radius = radius,
                    tileMode = tileMode
                )
            }
        }
    }

    private fun JsonReader.nextFloat() = nextDouble().toFloat()

    private fun JsonReader.nextColor(): Color {
        beginArray()
        val argb = (4 downTo 0).sumOf { position -> (nextInt() shl (position * 8)).toLong() }
        endArray()
        return Color(argb)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun ImageVector.Builder.applyOperations(
        operations: List<ImageVectorBuilderOperation>
    ) {
        for (operation in operations) {
            operation(this)
        }
    }

    private companion object {

        @JvmStatic
        private val IMAGE_VECTOR_OPTIONS =
            Options.of(
                "name",
                "viewportWidth",
                "viewportHeight",
                "width",
                "height",
                "tintColor",
                "tintBlendMode",
                "nodes"
            )

        @JvmStatic
        private val VECTOR_GROUP_OPTIONS =
            Options.of(
                "id",
                "rotation",
                "pivotX",
                "pivotY",
                "scaleX",
                "scaleY",
                "translationX",
                "translationY",
                "clipPathData",
                "nodes"
            )

        @JvmStatic
        private val VECTOR_PATH_OPTIONS =
            Options.of(
                "id",
                "fill",
                "fillAlpha",
                "stroke",
                "strokeAlpha",
                "strokeLineWidth",
                "strokeLineCap",
                "strokeLineJoin",
                "strokeLineMiter",
                "fillType",
                "trimPathStart",
                "trimPathEnd",
                "trimPathOffset",
                "pathNodes"
            )

        @JvmStatic
        private val PATH_NODE_OPTIONS = Options.of("command", "arguments")

        @JvmStatic
        private val GRADIENT_OPTIONS =
            Options.of(
                "type",
                "colors",
                "stops",
                "startX",
                "startY",
                "endX",
                "endY",
                "centerX",
                "centerY",
                "radius",
                "tileMode"
            )

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        private fun getPathNodeForCommandNameAndArguments(
            commandName: String,
            arguments: List<Any>
        ): PathNode {
            if ("rcTo" in commandName) {
                return when (commandName) {
                    "arcTo" ->
                        PathNode.ArcTo(
                            arguments[0] as Float, arguments[1] as Float,
                            arguments[2] as Float,
                            arguments[3] as Boolean, arguments[4] as Boolean,
                            arguments[5] as Float, arguments[6] as Float
                        )
                    /* "relativeArcTo", */
                    else ->
                        PathNode.RelativeArcTo(
                            arguments[0] as Float, arguments[1] as Float,
                            arguments[2] as Float,
                            arguments[3] as Boolean, arguments[4] as Boolean,
                            arguments[5] as Float, arguments[6] as Float
                        )
                }
            }
            arguments as List<Float>
            return when (commandName) {
                "moveTo" -> PathNode.MoveTo(arguments[0], arguments[1])

                "relativeMoveTo" -> PathNode.RelativeMoveTo(arguments[0], arguments[1])

                "lineTo" -> PathNode.LineTo(arguments[0], arguments[1])

                "relativeLineTo" -> PathNode.RelativeLineTo(arguments[0], arguments[1])

                "horizontalLineTo" -> PathNode.HorizontalTo(arguments[0])

                "relativeHorizontalLineTo" -> PathNode.RelativeHorizontalTo(arguments[0])

                "verticalLineTo" -> PathNode.VerticalTo(arguments[0])

                "relativeVerticalLineTo" -> PathNode.RelativeVerticalTo(arguments[0])

                "curveTo" ->
                    PathNode.CurveTo(
                        arguments[0], arguments[1],
                        arguments[2], arguments[3],
                        arguments[4], arguments[5]
                    )

                "relativeCurveTo" ->
                    PathNode.RelativeCurveTo(
                        arguments[0], arguments[1],
                        arguments[2], arguments[3],
                        arguments[4], arguments[5]
                    )

                "smoothCurveTo" ->
                    PathNode.ReflectiveCurveTo(
                        arguments[0], arguments[1],
                        arguments[2], arguments[3]
                    )

                "relativeSmoothCurveTo" ->
                    PathNode.RelativeReflectiveCurveTo(
                        arguments[0], arguments[1],
                        arguments[2], arguments[3]
                    )

                "quadraticBezierCurveTo" ->
                    PathNode.QuadTo(arguments[0], arguments[1], arguments[2], arguments[3])

                "relativeQuadraticBezierCurveTo" ->
                    PathNode.RelativeQuadTo(arguments[0], arguments[1], arguments[2], arguments[3])

                "smoothQuadraticBezierCurveTo" ->
                    PathNode.ReflectiveQuadTo(arguments[0], arguments[1])

                "relativeSmoothQuadraticBezierCurveTo" ->
                    PathNode.RelativeReflectiveQuadTo(arguments[0], arguments[1])

                "close" -> PathNode.Close

                else -> throw IllegalArgumentException("commandName: $commandName")
            }
        }
    }
}
