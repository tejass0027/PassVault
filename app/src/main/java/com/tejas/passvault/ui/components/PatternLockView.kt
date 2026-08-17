package com.tejas.passvault.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Classic 3x3 Android-style pattern lock. Reports the dot sequence (indices 0-8, left-to-right
 * top-to-bottom) via [onPatternComplete] once the user lifts their finger. Pass [showError] = true
 * briefly (e.g. for ~400ms) to flash the last-drawn pattern red after a wrong attempt.
 */
@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    showError: Boolean = false,
    onPatternComplete: (List<Int>) -> Unit
) {
    var selected by remember { mutableStateOf(listOf<Int>()) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val dotColor = MaterialTheme.colorScheme.outline
    val selectedColor = if (showError) Color(0xFFE53935) else MaterialTheme.colorScheme.primary

    // Computed once per layout size, not on every touch/draw event - avoids allocating a
    // new 9-element list on every single drag callback, which was making drawing feel janky.
    val centers = remember(canvasSize) {
        if (canvasSize.width == 0) emptyList() else dotCenters(canvasSize.width.toFloat(), canvasSize.height.toFloat())
    }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .onSizeChanged { canvasSize = it }
            .pointerInput(canvasSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val idx = nearestDotIndex(offset, centers, canvasSize)
                        selected = if (idx != null) listOf(idx) else emptyList()
                        dragPosition = offset
                    },
                    onDrag = { change, _ ->
                        dragPosition = change.position
                        val idx = nearestDotIndex(change.position, centers, canvasSize)
                        if (idx != null && idx !in selected) {
                            selected = selected + idx
                        }
                    },
                    onDragEnd = {
                        val finished = selected
                        dragPosition = null
                        selected = emptyList()
                        if (finished.isNotEmpty()) onPatternComplete(finished)
                    },
                    onDragCancel = {
                        dragPosition = null
                        selected = emptyList()
                    }
                )
            }
    ) {
        // Lines between confirmed dots.
        for (i in 0 until selected.size - 1) {
            drawLine(
                color = selectedColor,
                start = centers[selected[i]],
                end = centers[selected[i + 1]],
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }
        // Line from the last confirmed dot to the current finger position.
        val pos = dragPosition
        if (pos != null && selected.isNotEmpty()) {
            drawLine(
                color = selectedColor,
                start = centers[selected.last()],
                end = pos,
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }

        val dotRadius = min(size.width, size.height) * 0.035f
        val ringRadius = dotRadius * 2.2f
        centers.forEachIndexed { index, center ->
            val isSelected = index in selected
            if (isSelected) {
                drawCircle(color = selectedColor.copy(alpha = 0.18f), radius = ringRadius, center = center)
            }
            drawCircle(
                color = if (isSelected) selectedColor else dotColor,
                radius = dotRadius,
                center = center
            )
        }
    }
}

private fun dotCenters(width: Float, height: Float): List<Offset> {
    val side = min(width, height)
    val margin = side * 0.18f
    val step = (side - 2 * margin) / 2f
    val offsetX = (width - side) / 2f
    val offsetY = (height - side) / 2f
    return (0 until 9).map { i ->
        val row = i / 3
        val col = i % 3
        Offset(offsetX + margin + col * step, offsetY + margin + row * step)
    }
}

private fun nearestDotIndex(position: Offset, centers: List<Offset>, canvasSize: IntSize): Int? {
    if (centers.isEmpty() || canvasSize.width == 0 || canvasSize.height == 0) return null
    val touchRadius = min(canvasSize.width, canvasSize.height) * 0.13f
    var closestIndex: Int? = null
    var closestDistance = Float.MAX_VALUE
    centers.forEachIndexed { index, center ->
        val distance = sqrt((position.x - center.x).pow(2) + (position.y - center.y).pow(2))
        if (distance < touchRadius && distance < closestDistance) {
            closestDistance = distance
            closestIndex = index
        }
    }
    return closestIndex
}
