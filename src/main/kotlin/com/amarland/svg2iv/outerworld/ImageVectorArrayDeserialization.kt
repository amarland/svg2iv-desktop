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
import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborNumber
import com.google.iot.cbor.CborObject
import com.google.iot.cbor.CborReader
import com.google.iot.cbor.CborSimple.FALSE
import com.google.iot.cbor.CborSimple.NULL
import com.google.iot.cbor.CborSimple.TRUE
import com.google.iot.cbor.CborTextString

private typealias ImageVectorBuilderOperation = (ImageVector.Builder) -> Unit

fun ImageVector.Companion.fromCbor(reader: CborReader): List<ImageVector?> {
    return (reader.readDataItem() as CborArray).map { item ->
        if (item == NULL) return@map null

        val imageVector = item as CborMap
        ImageVector.Builder(
            name = imageVector["vectorName"].asString(),
            defaultWidth = imageVector["width"].asFloat().dp,
            defaultHeight = imageVector["height"].asFloat().dp,
            viewportWidth = imageVector["viewportWidth"].asFloat(),
            viewportHeight = imageVector["viewportHeight"].asFloat(),
            tintColor = imageVector["tintColor"].asOptionalLong()?.let(::Color) ?: Color.Unspecified,
            tintBlendMode = when (imageVector["tintBlendMode"].asOptionalInt()) {
                0 -> BlendMode.SrcOver
                2 -> BlendMode.SrcAtop
                3 -> BlendMode.Modulate
                4 -> BlendMode.Screen
                5 -> BlendMode.Plus
                else -> BlendMode.SrcIn
            },
        ).apply {
            val operations = buildVectorNodeOperations(imageVector["nodes"].asIterableOfCborMaps())
            for (operation in operations) operation(this@apply)
        }.build()
    }
}

private fun buildVectorNodeOperations(array: Iterable<CborMap>): List<ImageVectorBuilderOperation> =
    array.map { vectorNode ->
        if ("nodes" in vectorNode) buildVectorGroupOperation(vectorNode)
        else buildVectorPathOperation(vectorNode)
    }

private fun buildVectorGroupOperation(vectorGroup: CborMap): ImageVectorBuilderOperation =
    { builder: ImageVector.Builder ->
        builder.group(
            name = vectorGroup["groupName"].asOptionalString() ?: DefaultGroupName,
            rotate = vectorGroup["rotation"].asOptionalFloat() ?: DefaultRotation,
            pivotX = vectorGroup["pivotX"].asOptionalFloat() ?: DefaultPivotX,
            pivotY = vectorGroup["pivotY"].asOptionalFloat() ?: DefaultPivotY,
            scaleX = vectorGroup["scaleX"].asOptionalFloat() ?: DefaultScaleX,
            scaleY = vectorGroup["scaleY"].asOptionalFloat() ?: DefaultScaleY,
            translationX = vectorGroup["translationX"].asOptionalFloat() ?: DefaultTranslationX,
            translationY = vectorGroup["translationY"].asOptionalFloat() ?: DefaultTranslationY,
            clipPathData = vectorGroup["clipPathData"]?.asIterableOfCborMaps()
                ?.mapNotNull(::mapPathNode) ?: EmptyPath
        ) {
            val operations = buildVectorNodeOperations(vectorGroup["nodes"].asIterableOfCborMaps())
            for (operation in operations) operation(this@group)
        }
    }

private fun buildVectorPathOperation(vectorPath: CborMap): ImageVectorBuilderOperation =
    { builder: ImageVector.Builder ->
        builder.addPath(
            pathData = vectorPath["pathNodes"].asIterableOfCborMaps().mapNotNull(::mapPathNode),
            pathFillType = when (vectorPath["fillType"].asOptionalInt()) {
                0 -> PathFillType.NonZero
                1 -> PathFillType.EvenOdd
                else -> DefaultFillType
            },
            name = vectorPath["pathName"].asOptionalString() ?: DefaultPathName,
            fill = vectorPath["fill"]?.let(::mapBrush),
            fillAlpha = vectorPath["fillAlpha"].asOptionalFloat() ?: 1F,
            stroke = vectorPath["stroke"]?.let(::mapBrush),
            strokeAlpha = vectorPath["strokeAlpha"].asOptionalFloat() ?: 1F,
            strokeLineWidth = vectorPath["strokeLineWidth"].asOptionalFloat() ?: DefaultStrokeLineWidth,
            strokeLineCap = when (vectorPath["strokeLineCap"].asOptionalInt()) {
                0 -> StrokeCap.Butt
                1 -> StrokeCap.Round
                2 -> StrokeCap.Square
                else -> DefaultStrokeLineCap
            },
            strokeLineJoin = when (vectorPath["strokeLineJoin"].asOptionalInt()) {
                0 -> StrokeJoin.Bevel
                1 -> StrokeJoin.Miter
                2 -> StrokeJoin.Round
                else -> DefaultStrokeLineJoin
            },
            strokeLineMiter = vectorPath["strokeLineMiter"].asOptionalFloat() ?: DefaultStrokeLineMiter,
            trimPathStart = vectorPath["trimPathStart"].asOptionalFloat() ?: DefaultTrimPathStart,
            trimPathEnd = vectorPath["trimPathEnd"].asOptionalFloat() ?: DefaultTrimPathEnd,
            trimPathOffset = vectorPath["trimPathOffset"].asOptionalFloat() ?: DefaultTrimPathOffset
        )
    }

private fun mapPathNode(map: CborMap): PathNode? {
    val argumentsObject = map["arguments"]
    val commandIndex = map["command"].asInt()

    if (commandIndex == 4) return PathNode.Close
    if (argumentsObject == NULL) return null

    val args = argumentsObject.asFloatArray()
    return when (commandIndex) {
        0 -> PathNode.MoveTo(args[0], args[1])
        1 -> PathNode.LineTo(args[0], args[1])
        2 -> PathNode.CurveTo(args[0], args[1], args[2], args[3], args[4], args[5])
        3 -> PathNode.ArcTo(
            args[0], args[1],
            args[2],
            args[3].asBoolean(),
            args[4].asBoolean(),
            args[5], args[6]
        )
        else -> null
    }
}

private fun mapBrush(brush: CborObject): Brush =
    if (brush is CborInteger) mapSolidColor(brush) else mapGradient(brush as CborMap)

private fun mapSolidColor(colorInt: CborInteger): Brush = SolidColor(Color(colorInt.asLong()))

private fun mapGradient(gradient: CborMap): Brush {
    val colorIntegers = (gradient["colors"] as CborArray).listValue()
    val colorCount = colorIntegers.size
    val stops = gradient["stops"]?.asFloatArray() ?: FloatArray(colorCount) { index -> (index + 1F) / colorCount }
    val colorStops = Array(colorCount) { index -> stops[index] to Color(colorIntegers[index].asLong()) }

    val tileMode = when (gradient["tileMode"].asOptionalInt()) {
        1 -> TileMode.Repeated
        2 -> TileMode.Mirror
        else -> TileMode.Clamp
    }

    return if (gradient["isLinear"].asBoolean()) {
        Brush.linearGradient(
            colorStops = colorStops,
            start = Offset.fromNullablePoints(
                gradient["startX"].asOptionalFloat(),
                gradient["startY"].asOptionalFloat(),
                default = Offset.Zero
            ),
            end = Offset.fromNullablePoints(
                gradient["endX"].asOptionalFloat(),
                gradient["endY"].asOptionalFloat(),
                default = Offset.Infinite
            ),
            tileMode
        )
    } else {
        Brush.radialGradient(
            colorStops = colorStops,
            center = Offset.fromNullablePoints(
                gradient["centerX"].asOptionalFloat(),
                gradient["centerY"].asOptionalFloat(),
                Offset.Unspecified
            ),
            radius = gradient["radius"].asOptionalFloat() ?: Float.POSITIVE_INFINITY,
            tileMode
        )
    }
}

private fun CborObject.asString() = (this as CborTextString).stringValue()
private fun CborObject?.asOptionalString() = (this as? CborTextString)?.stringValue()

private fun CborObject.asFloat() = (this as CborNumber).floatValue()
private fun CborObject?.asOptionalFloat() = (this as? CborNumber)?.floatValue()

private fun CborObject.asInt() = (this as CborInteger).intValueExact()
private fun CborObject?.asOptionalInt() = (this as? CborInteger)?.intValueExact()

private fun CborObject.asLong() = (this as CborInteger).longValue()
private fun CborObject?.asOptionalLong() = (this as? CborInteger)?.longValue()

private fun CborObject.asBoolean() =
    when (this) {
        TRUE -> true
        FALSE -> false
        else -> throw ClassCastException("$this is not a Boolean!")
    }

private fun CborObject.asIterableOfCborMaps(): Iterable<CborMap> {
    val arrayIterator = (this as CborArray).iterator()
    return Iterable {
        object : Iterator<CborMap> {

            override fun hasNext() = arrayIterator.hasNext()

            override fun next() = arrayIterator.next() as CborMap
        }
    }
}

private fun CborObject.asFloatArray(): FloatArray {
    val values = (this as CborArray).listValue()
    return FloatArray(size()) { index -> values[index].asFloat() }
}

private operator fun CborMap.contains(key: String) = containsKey(key)

private fun Float.asBoolean() = this != 0F

private fun Offset.Companion.fromNullablePoints(x: Float?, y: Float?, default: Offset) =
    Offset(x ?: default.x, y ?: default.y)
