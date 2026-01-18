package com.example.adshield.ui.components

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset

/**
 * Draws "CyberPunk" style rounded corner brackets (L-shapes) around the component.
 *
 * @param color The color of the corners.
 * @param strokeWidth The thickness of the lines.
 * @param cornerLength The length of each arm of the corner L-shape.
 * @param cornerShape The shape to derive corner radius from (e.g. MaterialTheme.shapes.small).
 */
fun Modifier.drawCyberCorners(
    color: Color,
    strokeWidth: Dp = 2.dp,
    cornerLength: Dp = 10.dp,
    cornerShape: CornerBasedShape? = null
): Modifier = this.drawBehind {
    val stroke = strokeWidth.toPx()
    val length = cornerLength.toPx()
    
    // Calculate radius from shape if provided, otherwise 0
    val radius = cornerShape?.topStart?.toPx(size, this) ?: 0f

    val w = size.width
    val h = size.height

    // Top-Left
    val pathTL = Path().apply {
        moveTo(0f, length)
        lineTo(0f, radius)
        if (radius > 0) quadraticTo(0f, 0f, radius, 0f)
        lineTo(length, 0f)
    }

    // Top-Right
    val pathTR = Path().apply {
        moveTo(w - length, 0f)
        lineTo(w - radius, 0f)
        if (radius > 0) quadraticTo(w, 0f, w, radius)
        lineTo(w, length)
    }

    // Bottom-Right
    val pathBR = Path().apply {
        moveTo(w, h - length)
        lineTo(w, h - radius)
        if (radius > 0) quadraticTo(w, h, w - radius, h)
        lineTo(w - length, h)
    }

    // Bottom-Left
    val pathBL = Path().apply {
        moveTo(length, h)
        lineTo(radius, h)
        if (radius > 0) quadraticTo(0f, h, 0f, h - radius)
        lineTo(0f, h - length)
    }

    val style = Stroke(width = stroke, cap = StrokeCap.Round)

    drawPath(path = pathTL, color = color, style = style)
    drawPath(path = pathTR, color = color, style = style)
    drawPath(path = pathBR, color = color, style = style)
    drawPath(path = pathBL, color = color, style = style)
}

/**
 * Draws horizontal "Accent" lines on the Top-Right and Bottom-Left corners.
 * This creates a modern, Sci-Fi "bracket" effect without full corners.
 *
 * @param color The color of the accents.
 * @param width The thickness of the lines.
 * @param length The length of the horizontal segments.
 */

fun Modifier.drawCyberAccents(
    color: Color,
    width: Dp = 3.dp,
    length: Dp = 30.dp,
    xOffset: Dp = 16.dp
): Modifier = this.drawBehind {
    val stroke = width.toPx()
    val len = length.toPx()
    val off = xOffset.toPx()
    val w = size.width
    val h = size.height
    
    // Top-Right Horizontal Accent (Shifted from right edge)
    drawRect(
        color = color,
        topLeft = Offset(w - len - off, 0f),
        size = androidx.compose.ui.geometry.Size(len, stroke)
    )

    // Bottom-Left Horizontal Accent (Shifted from left edge)
    drawRect(
        color = color,
        topLeft = Offset(off, h - stroke),
        size = androidx.compose.ui.geometry.Size(len * 2, stroke)
    )
}
