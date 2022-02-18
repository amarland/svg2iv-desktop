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
                skipValue()
                continue
            }

            var name = DefaultGroupName
            var viewportWidth = Float.NaN
            var viewportHeight = Float.NaN
            var width = Float.NaN
            var height = Float.NaN
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
                    6 -> tintBlendMode = when (selectString(BLEND_MODE_OPTIONS)) {
                        0 -> BlendMode.SrcIn
                        1 -> BlendMode.SrcOver
                        2 -> BlendMode.SrcAtop
                        3 -> BlendMode.Modulate
                        4 -> BlendMode.Screen
                        5 -> BlendMode.Plus
                        else -> throw JsonDataException(
                            "$path: ${IMAGE_VECTOR_OPTIONS.strings()[6]}"
                        )
                    }
                    7 -> operations = readVectorNodes()
                    else -> skipValue()
                }
            }
            endObject()

            if (viewportWidth.isNaN() || viewportHeight.isNaN())
                throw JsonDataException("$path: invalid or missing values for viewport!")

            if (width.isNaN()) width = viewportWidth
            if (height.isNaN()) height = viewportHeight
            imageVectors += ImageVector.Builder(
                name,
                defaultWidth = width.dp,
                defaultHeight = height.dp,
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
            val isVectorGroup = peekJson().use { peeked ->
                peeked.selectName(VECTOR_GROUP_OPTIONS) > -1
            }
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
                else -> skipValue()
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
                6 -> strokeLineCap = when (selectString(STROKE_CAP_OPTIONS)) {
                    0 -> StrokeCap.Butt
                    1 -> StrokeCap.Round
                    2 -> StrokeCap.Square
                    else -> throw JsonDataException("$path: ${VECTOR_PATH_OPTIONS.strings()[6]}")
                }
                7 -> strokeLineJoin = when (selectString(STROKE_JOIN_OPTIONS)) {
                    0 -> StrokeJoin.Bevel
                    1 -> StrokeJoin.Miter
                    2 -> StrokeJoin.Round
                    else -> throw JsonDataException("$path: ${VECTOR_PATH_OPTIONS.strings()[7]}")
                }
                8 -> strokeLineMiter = nextFloat()
                9 -> fillType = when (selectString(FILL_TYPE_OPTIONS)) {
                    0 -> PathFillType.NonZero
                    1 -> PathFillType.EvenOdd
                    else -> throw JsonDataException("$path: ${VECTOR_PATH_OPTIONS.strings()[9]}")
                }
                10 -> trimPathStart = nextFloat()
                11 -> trimPathEnd = nextFloat()
                12 -> trimPathOffset = nextFloat()
                13 -> pathNodes = nextPathNodes()
                else -> skipValue()
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
        // NaNs are forbidden in JSON
        val arguments = FloatArray(7) { Float.NaN }
        val nodes = mutableListOf<PathNode>()
        while (hasNext()) {
            var commandIndex = -1
            beginObject()
            while (hasNext()) {
                when (selectName(PATH_NODE_OPTIONS)) {
                    0 -> commandIndex = selectString(PATH_NODE_COMMAND_OPTIONS)
                    1 -> {
                        arguments.fill(Float.NaN)

                        if (peek() == Token.NULL) continue

                        beginArray()
                        var index = -1
                        while (hasNext()) {
                            if (index++ > arguments.lastIndex)
                                throw JsonDataException("$path: too many arguments")
                            arguments[index] =
                                if ((commandIndex == 16 || commandIndex == 17) &&
                                    (index == 3 || index == 4) // boolean flags for arcs
                                ) {
                                    if (peek() == Token.BOOLEAN)
                                        if (nextBoolean()) 1F else 0F
                                    else throw JsonDataException(
                                        "$path: boolean argument expected at index $index"
                                    )
                                } else nextFloat()
                        }
                        endArray()
                    }
                    else -> skipValue()
                }
            }
            endObject()

            fun getArgumentAt(index: Int) =
                arguments[index].also {
                    if (it.isNaN()) throw JsonDataException(
                        "$path: too few arguments for " +
                                PATH_NODE_COMMAND_OPTIONS.strings()[commandIndex]
                    )
                }

            val argumentCount: Int
            nodes += when (commandIndex) {
                0 -> {
                    argumentCount = 2
                    PathNode.MoveTo(getArgumentAt(0), getArgumentAt(1))
                }
                1 -> {
                    argumentCount = 2
                    PathNode.RelativeMoveTo(getArgumentAt(0), getArgumentAt(1))
                }
                2 -> {
                    argumentCount = 2
                    PathNode.LineTo(getArgumentAt(0), getArgumentAt(1))
                }
                3 -> {
                    argumentCount = 2
                    PathNode.RelativeLineTo(getArgumentAt(0), getArgumentAt(1))
                }
                4 -> {
                    argumentCount = 1
                    PathNode.HorizontalTo(getArgumentAt(0))
                }
                5 -> {
                    argumentCount = 1
                    PathNode.RelativeHorizontalTo(getArgumentAt(0))
                }
                6 -> {
                    argumentCount = 1
                    PathNode.VerticalTo(getArgumentAt(0))
                }
                7 -> {
                    argumentCount = 1
                    PathNode.RelativeVerticalTo(getArgumentAt(0))
                }
                8 -> {
                    argumentCount = 6
                    PathNode.CurveTo(
                        getArgumentAt(0), getArgumentAt(1),
                        getArgumentAt(2), getArgumentAt(3),
                        getArgumentAt(4), getArgumentAt(5)
                    )
                }
                9 -> {
                    argumentCount = 6
                    PathNode.RelativeCurveTo(
                        getArgumentAt(0), getArgumentAt(1),
                        getArgumentAt(2), getArgumentAt(3),
                        getArgumentAt(4), getArgumentAt(5)
                    )
                }
                10 -> {
                    argumentCount = 4
                    PathNode.ReflectiveCurveTo(
                        getArgumentAt(0), getArgumentAt(1), getArgumentAt(2), getArgumentAt(3)
                    )
                }
                11 -> {
                    argumentCount = 4
                    PathNode.RelativeReflectiveCurveTo(
                        getArgumentAt(0), getArgumentAt(1), getArgumentAt(2), getArgumentAt(3)
                    )
                }
                12 -> {
                    argumentCount = 4
                    PathNode.QuadTo(
                        getArgumentAt(0), getArgumentAt(1),
                        getArgumentAt(2), getArgumentAt(3)
                    )
                }
                13 -> {
                    argumentCount = 4
                    PathNode.RelativeQuadTo(
                        getArgumentAt(0), getArgumentAt(1),
                        getArgumentAt(2), getArgumentAt(3)
                    )
                }
                14 -> {
                    argumentCount = 2
                    PathNode.ReflectiveQuadTo(getArgumentAt(0), getArgumentAt(1))
                }
                15 -> {
                    argumentCount = 2
                    PathNode.RelativeReflectiveQuadTo(getArgumentAt(0), getArgumentAt(1))
                }
                16 -> {
                    argumentCount = 7
                    PathNode.ArcTo(
                        getArgumentAt(0), getArgumentAt(1),
                        getArgumentAt(2),
                        getArgumentAt(3) != 0F, getArgumentAt(4) != 0F,
                        getArgumentAt(5), getArgumentAt(6)
                    )
                }
                17 -> {
                    argumentCount = 7
                    PathNode.RelativeArcTo(
                        getArgumentAt(0), getArgumentAt(1),
                        getArgumentAt(2),
                        getArgumentAt(3) != 0F, getArgumentAt(4) != 0F,
                        getArgumentAt(5), getArgumentAt(6)
                    )
                }
                18 -> {
                    argumentCount = 0
                    PathNode.Close
                }
                else -> {
                    throw JsonDataException("$path: ${PATH_NODE_COMMAND_OPTIONS.strings()[0]}")
                }
            }

            if (argumentCount < arguments.size && !arguments[argumentCount].isNaN())
                throw JsonDataException("$path: too many arguments")
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
                    0 -> isLinear = when (selectString(GRADIENT_TYPE_OPTIONS)) {
                        0 -> true
                        1 -> false
                        else -> throw JsonDataException(
                            "$path: ${VECTOR_PATH_OPTIONS.strings()[0]}"
                        )
                    }
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
                    10 -> tileMode = when (selectString(TILE_MODE_OPTIONS)) {
                        0 -> TileMode.Clamp
                        1 -> TileMode.Repeated
                        2 -> TileMode.Mirror
                        else -> throw JsonDataException(
                            "$path: ${VECTOR_PATH_OPTIONS.strings()[10]}"
                        )
                    }
                    else -> skipValue()
                }
            }
            endObject()

            val colorCount = colors.size
            val areStopsDefined = stops.isNotEmpty()

            if (areStopsDefined && stops.size != colorCount)
                throw JsonDataException("$path: ${stops.size} stops and $colorCount colors")

            if (areStopsDefined) {
                val colorStops = Array(colorCount) { index -> stops[index] to colors[index] }
                return if (isLinear) {
                    Brush.linearGradient(
                        colorStops = colorStops,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        tileMode
                    )
                } else {
                    Brush.radialGradient(
                        colorStops = colorStops,
                        center = Offset(centerX, centerY),
                        radius,
                        tileMode
                    )
                }
            } else {
                return if (isLinear) {
                    Brush.linearGradient(
                        colors,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        tileMode
                    )
                } else {
                    Brush.radialGradient(
                        colors,
                        center = Offset(centerX, centerY),
                        radius,
                        tileMode
                    )
                }
            }
        }
    }

    private fun JsonReader.nextFloat() = nextDouble().toFloat()

    private fun JsonReader.nextColor(): Color {
        beginArray()
        val argb = (3 downTo 0).sumOf { position ->
            val int = if (peek() == Token.NUMBER) nextInt() else -1
            if (int !in 0..255) {
                throw JsonDataException(
                    """$path:
                       |    Colors must be declared as an array of 4 integers""".trimMargin() +
                            " comprised between 0 and 255 that represent the ARGB values."
                )
            }
            return@sumOf (int shl (position * 8)).toLong()
        }
        endArray()
        return Color(argb)
    }

    private fun ImageVector.Builder.applyOperations(operations: List<ImageVectorBuilderOperation>) {
        for (operation in operations) operation(this)
    }

    private companion object {

        @JvmStatic
        private val IMAGE_VECTOR_OPTIONS =
            Options.of(
                "vectorName",
                "viewportWidth",
                "viewportHeight",
                "width",
                "height",
                "tintColor",
                "tintBlendMode",
                "nodes"
            )

        @JvmStatic
        private val BLEND_MODE_OPTIONS =
            Options.of("srcIn", "srcOver", "srcAtop", "modulate", "screen", "plus")

        @JvmStatic
        private val STROKE_CAP_OPTIONS = Options.of("butt", "round", "square")

        @JvmStatic
        private val STROKE_JOIN_OPTIONS = Options.of("bevel", "miter", "round")

        @JvmStatic
        private val FILL_TYPE_OPTIONS = Options.of("nonZero", "evenOdd")

        @JvmStatic
        private val GRADIENT_TYPE_OPTIONS = Options.of("linear", "radial")

        @JvmStatic
        private val TILE_MODE_OPTIONS = Options.of("clamp", "repeated", "mirror")

        @JvmStatic
        private val VECTOR_GROUP_OPTIONS =
            Options.of(
                "groupName",
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
                "pathName",
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
        private val PATH_NODE_COMMAND_OPTIONS =
            Options.of(
                "moveTo",
                "relativeMoveTo",
                "lineTo",
                "relativeLineTo",
                "horizontalLineTo",
                "relativeHorizontalLineTo",
                "verticalLineTo",
                "relativeVerticalLineTo",
                "curveTo",
                "relativeCurveTo",
                "smoothCurveTo",
                "relativeSmoothCurveTo",
                "quadraticBezierCurveTo",
                "relativeQuadraticBezierCurveTo",
                "smoothQuadraticBezierCurveTo",
                "relativeSmoothQuadraticBezierCurveTo",
                "arcTo",
                "relativeArcTo",
                "close"
            )

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
    }
}
