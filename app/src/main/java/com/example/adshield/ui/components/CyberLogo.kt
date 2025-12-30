package com.example.adshield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.adshield.R
import kotlin.math.abs
import kotlin.math.cos

@Composable
fun CyberLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "domino_anim")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val logoBitmap = ImageBitmap.imageResource(id = R.drawable.ic_cyber_logo)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // --- LAYER 1: GHOST (Background) ---
        // Static, semi-transparent backing layer
        // We bake the alpha (0.2) directly into the matrix to ensure it's applied.
        androidx.compose.foundation.Image(
            bitmap = logoBitmap,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            // alpha = 0.2f, // Removed, handled in matrix
            colorFilter = ColorFilter.colorMatrix(
                androidx.compose.ui.graphics.ColorMatrix(
                    floatArrayOf(
                        0f, 0f, 0f, 0f, color.red * 255,
                        0f, 0f, 0f, 0f, color.green * 255,
                        0f, 0f, 0f, 0f, color.blue * 255,
                        // Alpha Row: Luminance * 0.2
                        // 0.33 * 0.2 = 0.066
                        0.066f, 0.066f, 0.066f, 0f, 0f
                    )
                )
            )
        )

        // --- LAYER 2: DOMINO ANIMATION (Foreground) ---
        // DRAW SLICES DIRECTLY
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val cols = 8
            val rows = 8
            
            // Defines the color tint for the logo parts
            val paintColor = ColorFilter.colorMatrix(
                androidx.compose.ui.graphics.ColorMatrix(
                     floatArrayOf(
                        0f, 0f, 0f, 0f, color.red * 255,
                        0f, 0f, 0f, 0f, color.green * 255,
                        0f, 0f, 0f, 0f, color.blue * 255,
                        0.33f, 0.33f, 0.33f, 0f, 0f
                    )
                )
            )

            // Iterate Grid
            for (row in 0 until rows) {
                // Calculate precise integer boundaries for rows to avoid gaps
                val rowTop = (row * h / rows).toInt()
                val rowBottom = ((row + 1) * h / rows).toInt()
                val cellH = (rowBottom - rowTop).toFloat()

                for (col in 0 until cols) {
                    // Calculate precise integer boundaries for cols
                    val colLeft = (col * w / cols).toInt()
                    val colRight = ((col + 1) * w / cols).toInt()
                    val cellW = (colRight - colLeft).toFloat()

                    val diag = (col + row).toFloat()
                    val maxDiag = (cols + rows - 2).toFloat()
                    val cellDelay = diag / maxDiag * 0.6f 
                    
                    var scaleX = 1f
                    
                    // Animation Logic
                    var translationOffset = androidx.compose.ui.geometry.Offset.Zero
                    
                    if (progress <= 1f) {
                        // DISAPPEARING PHASE
                        val localT = (progress - cellDelay).coerceIn(0f, 0.4f) / 0.4f
                        scaleX = abs(cos(localT * Math.PI / 2)).toFloat()
                        
                        // Fly diagonally DOWN (Towards Bottom-Right)
                        // Max drift: 30% of cell size
                        val drift = (1f - scaleX) * cellW * 0.5f
                        translationOffset = androidx.compose.ui.geometry.Offset(drift, drift)

                        if (localT >= 1f) scaleX = 0f
                    } else {
                        // REAPPEARING PHASE
                        val p2 = progress - 1f
                        val localT = (p2 - cellDelay).coerceIn(0f, 0.4f) / 0.4f
                        scaleX = abs(kotlin.math.sin(localT * Math.PI / 2)).toFloat()
                        
                        // Fly in FROM TOP (From Top-Left)
                        // Starts at negative offset, moves to Zero
                        val drift = (1f - scaleX) * -cellW * 0.5f 
                        translationOffset = androidx.compose.ui.geometry.Offset(drift, drift)
                    }

                    // Optimization: Skip drawing if invisible
                    if (scaleX > 0.01f) {
                        val cx = colLeft + cellW / 2
                        val cy = rowTop + cellH / 2
                        
                        // Transform ONLY this cell
                        withTransform({
                            // Move to center to scale
                            translate(cx, cy)
                            // Apply "Flying" drift
                            translate(translationOffset.x, translationOffset.y)
                            scale(scaleX, 1f)
                            // Restore
                            translate(-cx, -cy)
                        }) {
                            // Map integers to source bitmap
                            val srcX = (colLeft.toFloat() / w) * logoBitmap.width
                            val srcY = (rowTop.toFloat() / h) * logoBitmap.height
                            val srcW = (cellW / w) * logoBitmap.width
                            val srcH = (cellH / h) * logoBitmap.height
                            
                            // Draw the slice
                            drawImage(
                                image = logoBitmap,
                                srcOffset = IntOffset(srcX.toInt(), srcY.toInt()),
                                srcSize = IntSize(srcW.toInt(), srcH.toInt()),
                                dstOffset = IntOffset(colLeft, rowTop),
                                dstSize = IntSize(cellW.toInt(), cellH.toInt()),
                                colorFilter = paintColor
                            )
                        }
                    }
                }
            }
        }
    }
}
