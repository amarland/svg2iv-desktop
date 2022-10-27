package com.amarland.svg2iv.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize

class ImageVectorNotPainter {

    private var cachedImageBitmap: ImageBitmap? = null
    private var matrix: Matrix? = null
    private var cachedImageVectorHashCode: Int = 0
    private var cachedSize: IntSize? = null

    private val drawScope = CanvasDrawScope()
    private val path = Path()
    private val pathMeasure = PathMeasure()
    private val pathParser = PathParser()

    fun drawImageVectorInto(
        target: DrawScope,
        imageVector: ImageVector,
        size: IntSize,
        tint: Color = Color.Unspecified
    ) {
        val hashCode = imageVector.hashCode()
        if (cachedImageBitmap == null ||
            hashCode != cachedImageVectorHashCode ||
            size != cachedSize
        ) {
            val bitmap = ImageBitmap(size.width, size.height)
            drawScope.draw(
                Density(target.density),
                target.layoutDirection,
                Canvas(bitmap),
                size.toSize()
            ) {
                withTransform({
                    val width = size.width
                    val height = size.height
                    val viewportWidth = imageVector.viewportWidth
                    val viewportHeight = imageVector.viewportHeight
                    val ratio = minOf(width / viewportWidth, height / viewportHeight)
                    val pivot = Offset(
                        (width - viewportWidth * ratio) / 2F,
                        (height - viewportHeight * ratio) / 2F
                    )
                    scale(ratio, ratio, pivot)
                    clipRect(0F, 0F, viewportWidth, viewportHeight)
                }) {
                    drawVectorGroup(imageVector.root, tint)
                }
            }
            cachedImageBitmap = bitmap
            cachedImageVectorHashCode = hashCode
            cachedSize = size
        }
        val image = cachedImageBitmap
        check(image != null)
        target.drawImage(image)
    }

    private fun DrawScope.drawVectorGroup(vectorGroup: VectorGroup, tint: Color) {
        withTransform({
            with(vectorGroup) {
                if (translationX != DefaultTranslationX ||
                    translationY != DefaultTranslationY ||
                    scaleX != DefaultScaleX ||
                    scaleY != DefaultScaleY ||
                    rotation != DefaultRotation
                ) {
                    val matrix = matrix ?: Matrix().also { matrix = it }
                    transform(
                        matrix.apply {
                            reset()
                            translate(translationX + pivotX, translationY + pivotY)
                            rotateZ(rotation)
                            scale(scaleX, scaleY)
                            translate(-pivotX, -pivotY)
                        }
                    )
                }
                if (clipPathData.isNotEmpty()) clipPath(clipPathData.toPath())
            }
        }) {
            for (node in vectorGroup) {
                when (node) {
                    is VectorGroup -> drawVectorGroup(node, tint)
                    is VectorPath -> drawVectorPath(node, tint)
                }
            }
        }
    }

    private fun DrawScope.drawVectorPath(vectorPath: VectorPath, tint: Color) {
        with(vectorPath) {
            val path = pathData.toPath()
            if (trimPathStart != DefaultTrimPathStart || trimPathEnd != DefaultTrimPathEnd) {
                pathMeasure.setPath(path, forceClosed = false)
                val length = pathMeasure.length
                val start = ((trimPathStart + trimPathOffset) % 1F) * length
                val end = ((trimPathEnd + trimPathOffset) % 1F) * length
                val newPath = Path()
                if (start > end) {
                    pathMeasure.getSegment(start, length, newPath, startWithMoveTo = true)
                    pathMeasure.getSegment(0F, end, newPath, startWithMoveTo = true)
                } else {
                    pathMeasure.getSegment(start, end, newPath, startWithMoveTo = true)
                }
                with(path) {
                    reset()
                    addPath(newPath)
                }
            }
            val colorFilter = if (tint.isSpecified) ColorFilter.tint(tint) else null
            fill?.let { fill ->
                drawPath(path, fill, fillAlpha, colorFilter = colorFilter)
            }
            stroke?.let { stroke ->
                val style = Stroke(strokeLineWidth, strokeLineMiter, strokeLineCap, strokeLineJoin)
                drawPath(path, stroke, strokeAlpha, style, colorFilter = colorFilter)
            }
        }
    }

    private fun List<PathNode>.toPath() =
        with(pathParser) {
            clear()
            addPathNodes(this@toPath)
        }.toPath(path)
}
