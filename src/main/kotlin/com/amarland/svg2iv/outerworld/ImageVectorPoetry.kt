package com.amarland.svg2iv.outerworld

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.Dp
import com.facebook.ktfmt.GOOGLE_FORMAT
import com.facebook.ktfmt.format
import com.squareup.kotlinpoet.CodeBlock
import java.io.File
import java.io.IOException
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.isAccessible

private const val DEFAULT_IMAGE_VECTOR_NAME = "\u200B" // zero-width space

@Throws(IOException::class)
fun writeImageVectorsToFile(
    destinationPath: String,
    imageVectors: Iterable<ImageVector>,
    extensionReceiver: String? = null
) {
    fun String.lastIndexOfOrNull(char: Char) = lastIndexOf(char).takeUnless { it == -1 }

    val file = File(if (destinationPath.endsWith(".kt")) destinationPath else "$destinationPath.kt")
    val filePathSeparator = File.separatorChar
    val absoluteFilePath = file.absolutePath
    val startOfPackageName =
        Regex("src\\$filePathSeparator(java|kotlin)\\$filePathSeparator")
            .find(absoluteFilePath)
            ?.range
            ?.endInclusive
            ?.plus(1)
    val packageName = if (startOfPackageName == null) "<package>" else {
        val endOfPackageName =
            absoluteFilePath.lastIndexOfOrNull('.')
                ?: absoluteFilePath.lastIndexOfOrNull(filePathSeparator)
                ?: absoluteFilePath.length
        absoluteFilePath.substring(startOfPackageName, endOfPackageName)
    }
    val contents = with(StringBuilder()) {
        appendLine("package $packageName")
        appendLine()
        appendLine("androidx.compose.ui.graphics.*")
        appendLine("androidx.compose.ui.unit.dp")
        appendLine()
        for (imageVector in imageVectors) {
            append(getCodeBlockForImageVector(imageVector, extensionReceiver))
        }
    }.toString()

    file.writeText(format(GOOGLE_FORMAT, contents))
}

fun getCodeBlockForImageVector(
    imageVector: ImageVector,
    extensionReceiver: String? = null
): CodeBlock {
    val extensionReceiverDeclaration = extensionReceiver?.plus(".") ?: ""
    val backingPropertyName = "_${imageVector.name.decapitalize()}"
    val publicPropertyName = imageVector.name

    return CodeBlock.builder()
        .add("private var $backingPropertyName: ImageVector? = null")
        .addLineSeparator()
        .addLineSeparator()
        .add("val $extensionReceiverDeclaration$publicPropertyName: ImageVector")
        .addLineSeparator()
        .indentFourSpaces()
        .add("get() {")
        .addLineSeparator()
        .indentFourSpaces()
        .add("if ($backingPropertyName == null) {")
        .addLineSeparator()
        .indentFourSpaces()
        .addMemberCall(
            "$backingPropertyName = ImageVector.Builder",
            Pair(
                "name",
                // "bypass" KNOWN_PARAMETERS_WITH_DEFAULT_VALUES
                imageVector.name.takeUnless { it == DefaultGroupName } ?: DEFAULT_IMAGE_VECTOR_NAME
            ),
            *imageVector.propertiesAsOrderedNameValuePairs(
                targetCallable = ImageVector.Builder::class.primaryConstructor!!
            ) { propertyName ->
                !propertyName.startsWith("default") && !propertyName.startsWith("viewport") && propertyName != "root"
            }
        )
        .addLineSeparator()
        .add(".")
        .add(getCodeBlockForGroup(imageVector.root))
        .add(".build()")
        .addLineSeparator()
        .unindentFourSpaces()
        .add("}")
        .addLineSeparator()
        .unindentFourSpaces()
        .add("return $backingPropertyName")
        .addLineSeparator()
        .unindentFourSpaces()
        .add("}")
        .addLineSeparator()
        .build()
}

fun getCodeBlockForGroup(group: VectorGroup): CodeBlock =
    with(CodeBlock.builder()) {
        addMemberCall(
            "group",
            *group.propertiesAsOrderedNameValuePairs(
                targetCallable = ImageVector.Builder::group
            ) { propertyName ->
                propertyName != "size" &&
                        propertyName != "children" &&
                        propertyName != "clipPathData"
            },
            "clipPathData" to group.clipPathData
        )
        add(" {")
        addLineSeparator()
        indentFourSpaces()
        for (node in group) {
            val block = when (node) {
                is VectorGroup -> getCodeBlockForGroup(node)
                is VectorPath -> getCodeBlockForPath(node)
            }
            add(block)
            addLineSeparator()
        }
        unindentFourSpaces()
        add("}")
        build()
    }

fun getCodeBlockForPath(path: VectorPath): CodeBlock =
    with(CodeBlock.builder()) {
        val isPathTrimmed = path.trimPathStart != DefaultTrimPathStart ||
                path.trimPathEnd != DefaultTrimPathEnd ||
                path.trimPathOffset != DefaultTrimPathOffset
        if (isPathTrimmed) {
            addMemberCall(
                "addPath",
                "pathData" to path.pathData,
                *path.propertiesAsOrderedNameValuePairs(
                    targetCallable = ImageVector.Builder::addPath
                ) { propertyName -> propertyName != "pathData" }
            )
        } else {
            addMemberCall(
                "path",
                *path.propertiesAsOrderedNameValuePairs(
                    targetCallable = ImageVector.Builder::path
                ) { propertyName ->
                    propertyName != "pathData" && !propertyName.startsWith("trimPath")
                }
            )
            add(" {")
            addLineSeparator()
            indentFourSpaces()
            for (node in path.pathData) {
                val targetFunction = getDslExtensionFunctionForPathNode(node)
                val block = getSingleLineCodeBlockForMember(
                    targetFunction.name,
                    *node.propertiesAsOrderedNameValuePairs(targetFunction).values()
                )
                add(block)
                addLineSeparator()
            }
            unindentFourSpaces()
            add("}")
        }
        build()
    }

// region Utilities

private fun CodeBlock.Builder.addLineSeparator() = add(System.lineSeparator())

private fun CodeBlock.Builder.indentFourSpaces() = indent().indent()
private fun CodeBlock.Builder.unindentFourSpaces() = unindent().unindent()

private fun CodeBlock.Builder.addMemberCall(
    memberName: String,
    vararg arguments: Pair<String?, Any?>
) = add(getBuilderStyleCodeBlockForMember(memberName, *arguments))

private fun getSingleLineCodeBlockForMember(
    memberName: String,
    vararg arguments: Any?,
    isMemberAnObjectDeclaration: Boolean = false
): CodeBlock {
    val block = getBuilderStyleCodeBlockForMember(
        memberName,
        *arguments.map { null to it }.toTypedArray(),
        wrapLines = false
    )
    if (arguments.isEmpty() && !isMemberAnObjectDeclaration) {
        return block.toBuilder().add("()").build()
    }
    return block
}

/*
 * - if at least one argument different from default values:
 *     `member`
 * - otherwise:
 *     - if wrapLines: ```member(
 *                            argName = argValue,
 *                            [...],
 *                        )```
 *     - else: `member(argName = argValue, [...])`
 */
private fun getBuilderStyleCodeBlockForMember(
    memberName: String,
    vararg arguments: Pair<String?, Any?>,
    wrapLines: Boolean = true
): CodeBlock =
    with(CodeBlock.builder()) {
        add(memberName)
        val argumentBlocks = arguments.mapNotNull { (name, value) ->
            getCodeBlockForArgument(name, value)
        }
        if (argumentBlocks.isNotEmpty()) {
            add("(")
            if (wrapLines) {
                addLineSeparator()
                indentFourSpaces()
            }
            for ((index, block) in argumentBlocks.withIndex()) {
                val isProcessingLastBlock = index == argumentBlocks.lastIndex

                if (!wrapLines && isProcessingLastBlock || argumentBlocks.size == 1) {
                    add(block.toString().let { it.substring(0 until it.lastIndex) })
                } else {
                    add(block)
                }

                if (wrapLines) {
                    addLineSeparator()
                } else if (!isProcessingLastBlock) {
                    add(" ")
                }
            }
            if (wrapLines) unindentFourSpaces()
            add(")")
        }
        build()
    }

/*
 * `argName = argValue,`
 */
private fun getCodeBlockForArgument(argName: String?, argValue: Any?): CodeBlock? {
    if (argValue == null)
        return null

    if (KNOWN_PARAMETERS_WITH_DEFAULT_VALUES[argName] == argValue)
        return null

    val block = when (argValue) {
        is CodeBlock -> argValue

        is String -> CodeBlock.of(
            "%S", argValue.takeUnless { it == DEFAULT_IMAGE_VECTOR_NAME } ?: DefaultGroupName
        )
        is Float -> CodeBlock.of(argValue.toLiteral())
        is Int -> CodeBlock.of("0x${argValue.toString(16).toUpperCase()}") // color int
        is Boolean -> CodeBlock.of(argValue.toString())

        is Dp -> CodeBlock.of("$argValue.dp")
        is Offset -> CodeBlock.of("Offset(${argValue.x.toLiteral()}, ${argValue.y.toLiteral()})")

        is Array<*> -> { // assume array means vararg
            CodeBlock.of(argValue.mapIndexed { index, value ->
                getCodeBlockForArgument(null, value).toString().let {
                    // erase the comma added by the recursive call, it will be added by this one
                    if (index == argValue.size - 1) it.substring(0 until it.length - 1) else it
                }
            }.joinToString(separator = System.lineSeparator()))
        }
        is List<*> -> getBuilderStyleCodeBlockForMember(
            "listOf",
            *argValue.map { null to it }.toTypedArray()
        )

        is PathNode -> {
            val clazz = argValue::class
            val isNodeACloseNode = argValue is PathNode.Close
            val arguments =
                if (isNodeACloseNode) emptyArray<Any>()
                else argValue.propertiesAsOrderedNameValuePairs(clazz.primaryConstructor!!).values()
            getSingleLineCodeBlockForMember(
                "PathNode.${clazz.simpleName!!}",
                *arguments,
                isMemberAnObjectDeclaration = isNodeACloseNode
            )
        }

        is Color -> getCodeBlockForColor(argValue)
        is SolidColor -> getSingleLineCodeBlockForMember(
            "SolidColor",
            getCodeBlockForColor(argValue.value)
        )
        is LinearGradient -> {
            @Suppress("USELESS_CAST")
            getCodeBlockForGradient(argValue as LinearGradient)
        }
        is RadialGradient -> {
            @Suppress("USELESS_CAST")
            getCodeBlockForGradient(argValue as RadialGradient)
        }

        is Pair<*, *> -> {
            val (stop, color) = argValue
            if (stop is Float && color is Color) {
                CodeBlock.builder()
                    .add("${stop.toLiteral()} to ")
                    .add(getCodeBlockForColor(color))
                    .build()
            } else null
        }

        is Enum<*> -> CodeBlock.of("${argValue::class.simpleName}.$argValue")

        else -> null
    } ?: throw NotImplementedError("Unexpected type: ${argValue::class.simpleName}")

    return CodeBlock.builder()
        .add(if (!argName.isNullOrEmpty()) "$argName = " else "")
        .add(block)
        .add(",")
        .build()
}

private fun Float.toLiteral() = (if (rem(1) == 0F) toInt().toString() else toString()) + "F"

private fun getCodeBlockForColor(color: Color) =
    getSingleLineCodeBlockForMember("Color", color.toArgb())

private inline fun <reified T : ShaderBrush> getCodeBlockForGradient(gradient: T): CodeBlock {
    val properties = T::class.declaredMemberProperties

    @Suppress("UNCHECKED_CAST")
    val colors = properties.firstWithName("colors").get(gradient) as List<Color>

    @Suppress("UNCHECKED_CAST")
    val stops = properties.firstWithName("stops").get(gradient) as? List<Float>
    val hasStops = !stops.isNullOrEmpty()

    return getBuilderStyleCodeBlockForMember(
        "Brush." + if (gradient is LinearGradient) "linearGradient" else "radialGradient",
        Pair(
            if (hasStops) null else "colors", // as "colorStops" is vararg, it can't be named
            if (hasStops) {
                stops!!.zip(colors) { stop, color -> stop to color }.toTypedArray() // vararg
            } else colors
        ),
        *gradient.propertiesAsOrderedNameValuePairs { propertyName ->
            propertyName != "colors" && propertyName != "stops"
        }
    )
}

private fun Array<Pair<String, *>>.values() = map { (_, value) -> value }.toTypedArray()

// TODO: take some time to reflect on this mess?
private inline fun <reified T : Any> T.propertiesAsOrderedNameValuePairs(
    targetCallable: KCallable<*>? = null,
    nameFilter: (String) -> Boolean = { true }
): Array<Pair<String, *>> {
    @Suppress("UNCHECKED_CAST")
    val clazz =
        if (T::class.isSealed) T::class.sealedSubclasses.single { this::class == it } as KClass<T>
        else T::class
    val sourceProperties = clazz.declaredMemberProperties
        .map { property -> property.name to property.apply { isAccessible = true }.get(this) }
        .filter { (name, _) -> nameFilter(name) }

    val targetParameters = (targetCallable ?: clazz.primaryConstructor!!).valueParameters
    val unmappedTargetParameterNames = ArrayList<String>(targetParameters.size)
    val mappedTargetParameterNames = ArrayList<String>(targetParameters.size)
    for (parameter in targetParameters) {
        val unmappedParameterName = parameter.name!!
        // the parameter names must match the source properties' so we can compare them later on
        val mappedParameterName =
            KNOWN_PARAMETER_NAMES_WITH_DIFFERENT_PROPERTY_NAME[unmappedParameterName]
                ?: unmappedParameterName
        if (nameFilter(mappedParameterName)) {
            unmappedTargetParameterNames += unmappedParameterName
            mappedTargetParameterNames += mappedParameterName
        }
    }

    val sizeDifference = sourceProperties.size - mappedTargetParameterNames.size
    if (sizeDifference != 0) {
        throw RuntimeException(
            "propertiesAsOrderedNameValuePairs:" +
                    " [target: ${targetCallable?.name ?: clazz.simpleName}]" +
                    System.lineSeparator() + "  The source's property list contains " +
                    (if (sizeDifference > 0) "more" else "less") +
                    " elements than the target's parameter list! Is `nameFilter` exhaustive?"
        )
    }

    return sourceProperties.mapIndexed { index, _ ->
        val nameToFind = mappedTargetParameterNames[index]
        sourceProperties.find { (name, _) -> nameToFind == name }
            // use the "original" parameter name in the result; in other words, reverse the mapping
            ?.let { (_, value) -> unmappedTargetParameterNames[index] to value }
            ?: throw NoSuchElementException(
                "propertiesAsOrderedNameValuePairs:" +
                        " [target: ${targetCallable?.name ?: clazz.simpleName} |" +
                        " name: $nameToFind]" + System.lineSeparator() +
                        "  Forgot to define a \"target parameter -> source property\" mapping?"
            )
    }.toTypedArray()
}

private fun <T> Collection<KProperty1<T, *>>.firstWithName(name: String) =
    first { it.name == name }.apply { isAccessible = true }

private val KNOWN_PARAMETERS_WITH_DEFAULT_VALUES =
    hashMapOf(
        "name" to DefaultGroupName,
        "tintColor" to Color.Unspecified,
        "tintBlendMode" to BlendMode.SrcIn,
        "rotate" to DefaultRotation,
        "pivotX" to DefaultPivotX,
        "pivotY" to DefaultPivotY,
        "scaleX" to DefaultScaleX,
        "scaleY" to DefaultScaleY,
        "translationX" to DefaultTranslationX,
        "translationY" to DefaultTranslationY,
        "clipPathData" to EmptyPath,
        "pathFillType" to PathFillType.NonZero,
        "fill" to null,
        "fillAlpha" to DefaultAlpha,
        "stroke" to null,
        "strokeAlpha" to DefaultAlpha,
        "strokeLineWidth" to DefaultStrokeLineWidth,
        "strokeLineCap" to DefaultStrokeLineCap,
        "strokeLineJoin" to DefaultStrokeLineJoin,
        "strokeLineMiter" to DefaultStrokeLineMiter,
        "trimPathStart" to DefaultTrimPathStart,
        "trimPathEnd" to DefaultTrimPathEnd,
        "trimPathOffset" to DefaultTrimPathOffset,
        "start" to Offset.Zero,
        "end" to Offset.Infinite,
        "tileMode" to TileMode.Clamp,
        "center" to Offset.Unspecified,
        "radius" to Float.POSITIVE_INFINITY
    )

private val KNOWN_PARAMETER_NAMES_WITH_DIFFERENT_PROPERTY_NAME =
    hashMapOf(
        "rotate" to "rotation",
        "block" to "children",
        "pathBuilder" to "pathData",
        "x1" to "arcStartX",
        "y1" to "arcStartY",
        "a" to "horizontalEllipseRadius",
        "b" to "verticalEllipseRadius",
        "dx1" to "arcStartDx",
        "dy1" to "arcStartDy"
    )

private fun getDslExtensionFunctionForPathNode(node: PathNode): KFunction<PathBuilder> =
    when (node) {
        PathNode.Close -> PathBuilder::close
        is PathNode.RelativeMoveTo -> PathBuilder::moveToRelative
        is PathNode.MoveTo -> PathBuilder::moveTo
        is PathNode.RelativeLineTo -> PathBuilder::lineToRelative
        is PathNode.LineTo -> PathBuilder::lineTo
        is PathNode.RelativeHorizontalTo -> PathBuilder::horizontalLineToRelative
        is PathNode.HorizontalTo -> PathBuilder::horizontalLineTo
        is PathNode.RelativeVerticalTo -> PathBuilder::verticalLineToRelative
        is PathNode.VerticalTo -> PathBuilder::verticalLineTo
        is PathNode.RelativeCurveTo -> PathBuilder::curveToRelative
        is PathNode.CurveTo -> PathBuilder::curveTo
        is PathNode.RelativeReflectiveCurveTo -> PathBuilder::reflectiveCurveToRelative
        is PathNode.ReflectiveCurveTo -> PathBuilder::reflectiveCurveTo
        is PathNode.RelativeQuadTo -> PathBuilder::quadToRelative
        is PathNode.QuadTo -> PathBuilder::quadTo
        is PathNode.RelativeReflectiveQuadTo -> PathBuilder::reflectiveQuadToRelative
        is PathNode.ReflectiveQuadTo -> PathBuilder::reflectiveQuadTo
        is PathNode.RelativeArcTo -> PathBuilder::arcToRelative
        is PathNode.ArcTo -> PathBuilder::arcTo
    }

// endregion
