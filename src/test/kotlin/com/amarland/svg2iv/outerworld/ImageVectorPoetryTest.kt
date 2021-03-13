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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.primaryConstructor

@Suppress("ClassName")
class ImageVectorPoetryTest {

    @Nested
    inner class `getCodeBlockForPath() generates a DSL-compliant path declaration` {

        @Test
        fun `without attributes`() {
            val expected =
                """path {
                  |$PATH_DATA_AS_STRING
                  |}""".trimMargin()

            val actual = getCodeBlockForPath(
                buildVectorPath()
            ).toString()

            Assertions.assertEquals(expected, actual)
        }

        @Test
        fun `with only a solid fill color`() {
            val expected =
                """path(
                  |    fill = SolidColor(Color(0x11223344))
                  |) {
                  |$PATH_DATA_AS_STRING
                  |}""".trimMargin()

            val actual = getCodeBlockForPath(
                buildVectorPath(fill = SolidColor(Color(0x11223344)))
            ).toString()

            Assertions.assertEquals(expected, actual)
        }

        @Test
        fun `with all attributes set (with default values) and gradients`() {
            val expected =
                """path(
                  |    name = "TestVector",
                  |    fill = Brush.linearGradient(
                  |        colors = listOf(
                  |            Color(0x11223344),
                  |            Color(0x55667788),
                  |        ),
                  |        start = Offset(1F, 2F),
                  |        end = Offset(3F, 4F),
                  |    ),
                  |    fillAlpha = 0.5F,
                  |    stroke = Brush.radialGradient(
                  |        0.25F to Color(0x11223344),
                  |        0.5F to Color(0x22556677),
                  |        0.75F to Color(0x33889910),
                  |        center = Offset(1F, 2F),
                  |        radius = 3F,
                  |    ),
                  |    strokeLineWidth = 2F,
                  |    strokeLineCap = StrokeCap.Round,
                  |    strokeLineMiter = 3F,
                  |) {
                  |$PATH_DATA_AS_STRING
                  |}""".trimMargin()

            val actual = getCodeBlockForPath(
                buildVectorPath(
                    name = "TestVector",
                    fill = Brush.linearGradient(
                        listOf(Color(0x11223344), Color(0x55667788)),
                        start = Offset(1F, 2F),
                        end = Offset(3F, 4F)
                    ),
                    fillAlpha = 0.5F,
                    stroke = Brush.radialGradient(
                        0.25F to Color(0x11223344),
                        0.5F to Color(0x22556677),
                        0.75F to Color(0x33889910),
                        center = Offset(1F, 2F),
                        radius = 3F,
                        tileMode = TileMode.Clamp,
                    ),
                    strokeAlpha = 1F,
                    strokeLineWidth = 2F,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 3F,
                    pathFillType = PathFillType.NonZero
                )
            ).toString()

            Assertions.assertEquals(expected, actual)
        }
    }

    @Nested
    inner class `writeGroup() writes a DSL-compliant group declaration` {

        @Test
        fun `with both set and unset attributes`() {
            val expected =
                """group(
                  |    name = "TestGroup",
                  |    rotate = 30F,
                  |    pivotX = 5F,
                  |    pivotY = 5F,
                  |    scaleX = 1.2F,
                  |    scaleY = 1.2F,
                  |    translationX = 9F,
                  |    clipPathData = listOf(
                  |        PathNode.HorizontalTo(12F)
                  |    ),
                  |) {
                  |    path {
                  |        horizontalLineTo(24F)
                  |    }
                  |    group {
                  |        path {
                  |            verticalLineTo(6F)
                  |        }
                  |    }
                  |}""".trimMargin()

            val actual = getCodeBlockForGroup(
                ImageVector.Builder("irrelevant", 24.dp, 24.dp, 24F, 24F)
                    .group(
                        name = "TestGroup",
                        rotate = 30F,
                        pivotX = 5F,
                        pivotY = 5F,
                        scaleX = 1.2F,
                        scaleY = 1.2F,
                        translationX = 9F,
                        translationY = 0F,
                        clipPathData = listOf(
                            PathNode.HorizontalTo(12F)
                        )
                    ) {
                        path {
                            horizontalLineTo(24F)
                        }
                        group {
                            path {
                                verticalLineTo(6F)
                            }
                        }
                    }.build().root[0] as VectorGroup
            ).toString()

            Assertions.assertEquals(expected, actual)
        }
    }

    private companion object {

        @Suppress("BooleanLiteralArgument")
        private val PATH_DATA = listOf(
            PathNode.MoveTo(9.72F, 10.93F),
            PathNode.VerticalTo(2.59F),
            PathNode.ArcTo(1.65F, 1.65F, 0F, false, false, 8F, 1F),
            PathNode.ArcTo(1.65F, 1.65F, 0F, false, false, 6.28F, 2.59F),
            PathNode.VerticalTo(11F),
            PathNode.RelativeArcTo(2.11F, 2.11F, 0F, false, false, -0.83F, 1.65F),
            PathNode.ArcTo(2.48F, 2.48F, 0F, false, false, 8F, 15F),
            PathNode.RelativeArcTo(2.44F, 2.44F, 0F, false, false, 2.55F, -2.35F),
            PathNode.ArcTo(2.34F, 2.34F, 0F, false, false, 9.72F, 10.93F),
            PathNode.Close
        )

        private val PATH_DATA_AS_STRING = """
            |    moveTo(9.72F, 10.93F)
            |    verticalLineTo(2.59F)
            |    arcTo(1.65F, 1.65F, 0F, false, false, 8F, 1F)
            |    arcTo(1.65F, 1.65F, 0F, false, false, 6.28F, 2.59F)
            |    verticalLineTo(11F)
            |    arcToRelative(2.11F, 2.11F, 0F, false, false, -0.83F, 1.65F)
            |    arcTo(2.48F, 2.48F, 0F, false, false, 8F, 15F)
            |    arcToRelative(2.44F, 2.44F, 0F, false, false, 2.55F, -2.35F)
            |    arcTo(2.34F, 2.34F, 0F, false, false, 9.72F, 10.93F)
            |    close()""".trimMargin()

        private fun buildVectorPath(
            name: String = DefaultPathName,
            pathFillType: PathFillType = DefaultFillType,
            fill: Brush? = null,
            fillAlpha: Float = 1.0f,
            stroke: Brush? = null,
            strokeAlpha: Float = 1.0f,
            strokeLineWidth: Float = DefaultStrokeLineWidth,
            strokeLineCap: StrokeCap = DefaultStrokeLineCap,
            strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
            strokeLineMiter: Float = DefaultStrokeLineMiter
        ) = VectorPath::class.primaryConstructor!!.let { constructor ->
            constructor.callBy(
                mapOf(
                    constructor.findParameterByName("name")!! to name,
                    constructor.findParameterByName("pathData")!! to PATH_DATA,
                    constructor.findParameterByName("pathFillType")!! to pathFillType,
                    constructor.findParameterByName("fill")!! to fill,
                    constructor.findParameterByName("fillAlpha")!! to fillAlpha,
                    constructor.findParameterByName("stroke")!! to stroke,
                    constructor.findParameterByName("strokeAlpha")!! to strokeAlpha,
                    constructor.findParameterByName("strokeLineWidth")!! to strokeLineWidth,
                    constructor.findParameterByName("strokeLineCap")!! to strokeLineCap,
                    constructor.findParameterByName("strokeLineJoin")!! to strokeLineJoin,
                    constructor.findParameterByName("strokeLineMiter")!! to strokeLineMiter
                )
            )
        }
    }
}
