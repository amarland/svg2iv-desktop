package com.amarland.svg2iv.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.awt.event.MouseEvent
import kotlin.math.hypot

// Based on this gist (adapted for desktop): https://gist.github.com/bmonjoie/8506040b2ea534eac931378348622725

@Composable
@Suppress("FunctionName")
fun <T> CircularReveal(
    targetState: T,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(),
    content: @Composable CircularRevealScope.(T) -> Unit
) {
    val items = remember { mutableStateListOf<CircularRevealAnimationItem<T>>() }
    val transitionState = remember { MutableTransitionState(targetState) }
    var offset: Offset? by remember { mutableStateOf(null) }
    val targetChanged = targetState != transitionState.targetState
    transitionState.targetState = targetState
    val transition = updateTransition(transitionState, label = "transition")
    if (targetChanged || items.isEmpty()) {
        // Only manipulate the list when the state has changed, or on the first run.
        val keys = items.map { item -> item.key }.let { list ->
            if (!list.contains(targetState)) {
                list.toMutableList().apply { add(targetState) }
            } else list
        }
        items.clear()
        keys.mapIndexedTo(items) { index, key ->
            CircularRevealAnimationItem(key) {
                val progress by transition.animateFloat(
                    transitionSpec = { animationSpec }, label = ""
                ) {
                    if (index == keys.lastIndex) {
                        if (it == key) 1F else 0F
                    } else 1F
                }
                Box(Modifier.circularReveal(progress = progress, offset = offset)) {
                    with(CircularRevealScope) {
                        content(key)
                    }
                }
            }
        }
    } else if (transitionState.currentState == transitionState.targetState) {
        // Remove all the intermediate items from the list once the animation has completed.
        items.removeAll { item -> item.key != transitionState.targetState }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            forEachGesture {
                awaitPointerEventScope {
                    offset = awaitPointerEvent().mouseEvent
                        ?.takeIf { it.button == MouseEvent.BUTTON1 }
                        ?.let { event -> Offset(event.x.toFloat(), event.y.toFloat())
                    }
                }
            }
        }
    ) {
        for ((key, item) in items) {
            key(key) {
                item()
            }
        }
    }
}

private data class CircularRevealAnimationItem<T>(
    val key: T,
    val content: @Composable () -> Unit
)

private fun Modifier.circularReveal(progress: Float, offset: Offset? = null) =
    then(clip(CircularRevealShape(progress, offset)))

private class CircularRevealShape(
    private val progress: Float,
    private val offset: Offset?
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ) = Outline.Generic(
        Path().apply {
            addOval(
                Rect(
                    center = Offset(
                        offset?.x ?: size.width / 2F,
                        offset?.y ?: size.height / 2F
                    ),
                    radius = longestDistanceToACorner(size, offset) * progress
                )
            )
        }
    )

    private fun longestDistanceToACorner(size: Size, offset: Offset?): Float {
        if (offset == null) {
            return hypot(size.width / 2F, size.height / 2F)
        }

        val topLeft = hypot(offset.x, offset.y)
        val topRight = hypot(size.width - offset.x, offset.y)
        val bottomLeft = hypot(offset.x, size.height - offset.y)
        val bottomRight = hypot(size.width - offset.x, size.height - offset.y)

        return topLeft.coerceAtLeast(topRight)
            .coerceAtLeast(bottomLeft)
            .coerceAtLeast(bottomRight)
    }
}

@LayoutScopeMarker
@Immutable
object CircularRevealScope
