package com.amarland.svg2iv.outerworld

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorStop
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.RadialGradient
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty1
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmName

private const val DEFAULT_IMAGE_VECTOR_NAME = "\u200B"

fun writeImageVectorsToFile(
    destinationPath: String,
    imageVectors: Iterable<ImageVector>,
    extensionReceiver: String? = null
) {
    TODO()
}

fun FileSpec.Builder.addImageVector(
    imageVector: ImageVector,
    extensionReceiver: String? = null
): FileSpec.Builder {
    val backingPropertyName = "_${imageVector.name.decapitalize()}"
    val publicPropertyName = imageVector.name

    val imageVectorTypeName = ImageVector::class.asTypeName()

    addProperty(
        PropertySpec.builder(backingPropertyName, imageVectorTypeName.copy(nullable = true))
            .mutable()
            .addModifiers(KModifier.PRIVATE)
            .initializer("null")
            .build()
    )
    addProperty(
        PropertySpec.builder(publicPropertyName, imageVectorTypeName)
            .getter(
                FunSpec.getterBuilder()
                    .addCode(
                        CodeBlock.builder()
                            .add("$backingPropertyName ?: run {")
                            .addLineSeparator()
                            .indent()
                            .add(
                                CodeBlock.builder()
                                    .addMemberCall(
                                        ImageVector.Builder::class.primaryConstructor!!,
                                        Pair(
                                            "name",
                                            // "bypass" KNOWN_PARAMETERS_WITH_DEFAULT_VALUES
                                            imageVector.name.takeUnless { it == DefaultGroupName }
                                                ?: DEFAULT_IMAGE_VECTOR_NAME
                                        ),
                                        *imageVector.propertiesAsNameValuePairs { props ->
                                            props.subList(1)
                                        }
                                    )
                                    .build()
                            )
                            .addLineSeparator()
                            .indent()
                            .add(".")
                            .add(getCodeBlockForGroup(imageVector.root))
                            .add(".build()")
                            .addLineSeparator()
                            .unindent()
                            .add("}")
                            .addLineSeparator()
                            .build()
                    )
                    .build()
            ).build()
    )

    return this
}

fun getCodeBlockForGroup(group: VectorGroup): CodeBlock =
    with(CodeBlock.builder()) {
        addMemberCall(
            ImageVector.Builder::group,
            *group.propertiesAsNameValuePairs { props ->
                props.subList(0, props.lastIndex - 1) // excluding `clipPathData` and `children`
            },
            // TODO
        )
        add(" {")
        addLineSeparator()
        indent()
        for (node in group) {
            val block = when (node) {
                is VectorGroup -> getCodeBlockForGroup(node)
                is VectorPath -> getCodeBlockForPath(node)
            }
            add(block)
        }
        addLineSeparator()
        unindent()
        add("}")
        build()
    }

fun getCodeBlockForPath(path: VectorPath): CodeBlock =
    with(CodeBlock.builder()) {
        addMemberCall(
            ImageVector.Builder::path,
            *path.propertiesAsNameValuePairs { props ->
                props.subList(0, props.lastIndex) // excluding `pathBuilder`
            }
        )
        add(" {")
        addLineSeparator()
        indent()
        for (node in path.pathData) {
            // TODO
        }
        addLineSeparator()
        unindent()
        add("}")
        build()
    }

// region Utilities

private fun CodeBlock.Builder.addLineSeparator() = add(System.lineSeparator())

private fun CodeBlock.Builder.addMemberCall(
    member: KCallable<*>,
    vararg arguments: Pair<String, Any>
) = add(getCodeBlockForMember(member, *arguments))

/*
 * if at least one argument different from default values: `member`
 * otherwise: ```member(
 *                   argName = argValue,
 *                   [...]
 *               )```
 */
private fun getCodeBlockForMember(member: KCallable<*>, vararg arguments: Pair<String, Any>) =
    with(CodeBlock.builder()) {
        add("%M", member.asMemberName())
        val argumentBlocks = arguments.mapNotNull { (name, value) ->
            getCodeBlockForArgument(name, value)
        }
        if (argumentBlocks.isNotEmpty()) {
            add("(")
            addLineSeparator()
            indent()
            for (block in argumentBlocks) {
                add(block)
                addLineSeparator()
            }
            unindent()
            add(")")
        }
        build()
    }

/*
 * `argName = argValue,`
 */
private inline fun <reified T> getCodeBlockForArgument(argName: String, argValue: T): CodeBlock? {
    if (KNOWN_PARAMETERS_WITH_DEFAULT_VALUES[argName] == argValue) return null

    val block = when (argValue) {
        is String -> CodeBlock.of(
            "%S", argValue.takeUnless { it == DEFAULT_IMAGE_VECTOR_NAME } ?: DefaultGroupName
        )
        is Number -> CodeBlock.of("%L", argValue)

        is Dp -> CodeBlock.of("%L.%M", argValue.value, Float::dp.asMemberName())

        is Color -> getCodeBlockForColor(argValue)
        is LinearGradient,
        is RadialGradient -> getCodeBlockForGradient(argValue as ShaderBrush)

        is Pair<*, *> -> {
            val isColorStop = T::class.typeParameters
                .map { it.upperBounds.single() } == listOf(Float::class, Color::class)
            @Suppress("UNCHECKED_CAST")
            if (isColorStop) {
                val (stop, color) = argValue as ColorStop
                CodeBlock.builder()
                    .add("%L to ", stop)
                    .add(getCodeBlockForColor(color))
                    .build()
            } else null
        }
        T::class.isSubclassOf(Enum::class) ->
            CodeBlock.of("%T.%S", T::class.asTypeName(), argValue.toString())

        else -> null
    } ?: throw NotImplementedError("Unexpected type: ${T::class.jvmName}")

    return CodeBlock.builder()
        .add("$argName = ")
        .add(block)
        .add(",")
        .build()
}

private fun getCodeBlockForColor(color: Color) =
    CodeBlock.of("%M(%L)", Color::class.primaryConstructor!!.asMemberName(), color)

private inline fun <reified T : ShaderBrush> getCodeBlockForGradient(gradient: T): CodeBlock {
    val properties = T::class.memberProperties

    @Suppress("UNCHECKED_CAST")
    val colors = properties.firstWithName("colors").get(gradient) as List<Color>

    @Suppress("UNCHECKED_CAST")
    val stops = properties.firstWithName("stops").get(gradient) as List<Float>
    val hasStops = !stops.isNullOrEmpty()

    val firstFunctionParameterName = if (hasStops) "colorStops" else "colors"

    return getCodeBlockForMember(
        Brush.Companion::class.functions
            .first { function ->
                function.name.startsWith(
                    if (gradient is LinearGradient) "linear" else "radial"
                ) && function.parameters[0].name == firstFunctionParameterName
            },
        Pair(
            firstFunctionParameterName,
            if (hasStops) stops.zip(colors) { stop, color -> stop to color }
            else colors
        ),
        *gradient.propertiesAsNameValuePairs { props -> props.subList(2) }
    )
}

private inline fun <reified T : Any> T.propertiesAsNameValuePairs(
    noinline filter: ((List<KProperty1<T, *>>) -> List<KProperty1<T, *>>)? = null
) = T::class.memberProperties
    .let { props -> filter?.invoke(props.toList()) ?: props }
    .map { prop -> prop.name to prop.get(this)!! }
    .toTypedArray()

private fun KCallable<*>.asMemberName() = MemberName(javaClass.packageName, name)

private fun <T> Collection<KProperty1<T, *>>.firstWithName(name: String) =
    first { prop -> prop.name == name }

private fun <T> List<T>.subList(fromIndex: Int) = subList(fromIndex, size)

private val KNOWN_PARAMETERS_WITH_DEFAULT_VALUES =
    hashMapOf(
        "name" to DefaultGroupName,
        "rotate" to DefaultRotation,
        "pivotX" to DefaultPivotX,
        "pivotY" to DefaultScaleY,
        "scaleX" to DefaultScaleX,
        "scaleY" to DefaultScaleY,
        "translationX" to DefaultTranslationX,
        "translationY" to DefaultTranslationY,
        "clipPathData" to EmptyPath,
        "fill" to null,
        "fillAlpha" to DefaultAlpha,
        "stroke" to null,
        "strokeAlpha" to DefaultAlpha,
        "strokeLineWidth" to DefaultStrokeLineWidth,
        "strokeLineCap" to DefaultStrokeLineCap,
        "strokeLineJoin" to DefaultStrokeLineJoin,
        "strokeLineMiter" to DefaultStrokeLineMiter,
        "start" to Offset.Zero,
        "end" to Offset.Infinite,
        "tileMode" to TileMode.Clamp,
        "center" to Offset.Unspecified,
        "radius" to Float.POSITIVE_INFINITY
    )

// endregion
