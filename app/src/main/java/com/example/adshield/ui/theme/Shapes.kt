package com.example.adshield.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp


// Custom shapes removed in favor of MaterialTheme.shapes

val __MaterialCyberShapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(5.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(50)
)

val MaterialCyberShapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(1.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(5.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp)
)