package com.example.adshield.ui.theme

import androidx.compose.ui.graphics.Color

val NeonGreen = Color(0xFF0df259)
val NeonGreenDark = Color(0xFF08a33b) // For text on light backgrounds if needed
val NeonGreenLight = Color(0xFF6eff8b) // For hover/glow states

// Dark Theme Colors
val BackgroundDark = Color(0xFF102216)
val SurfaceDark = Color(0xFF152b1e)
val TerminalBgDark = Color(0xFF0a160e)
val GridLineDark = Color(0xFF0df259).copy(alpha = 0.05f)

// Light Theme Colors
val BackgroundLight = Color(0xFFf5f8f6)
val SurfaceLight = Color(0xFFffffff)
val TerminalBgLight = Color(0xFFe8f5e9) // Very light green for terminal in light mode
val GridLineLight = Color(0xFF0df259).copy(alpha = 0.1f) // Slightly more visible in light mode

val ErrorRed = Color(0xFFEF5350)
val SafeGreen = Color(0xFF66BB6A)