package com.example.adshield.ui.theme

import androidx.compose.ui.graphics.Color

val NeonGreen = Color(0xFF0df259)
val NeonGreenDark = Color(0xFF08a33b)
val NeonGreenLight = Color(0xFF6eff8b)

// Dark Theme Colors
val BackgroundDark = Color(0xFF0D1117) // Slightly bluish black (GitHub Dark Dimmed style)
val SurfaceDark = Color(0xFF161B22)
val TerminalBgDark = Color(0xFF0D1117)
val GridLineDark = NeonGreen.copy(alpha = 0.05f)

// Light Theme Colors (Keeping standard for contrast, but user likely prefers dark)
val BackgroundLight = Color(0xFFf5f8f6)
val SurfaceLight = Color(0xFFffffff)
val TerminalBgLight = Color(0xFFe8f5e9)
val GridLineLight = Color(0xFF0df259).copy(alpha = 0.1f)

val ErrorRed = Color(0xFFFF5252) // More vibrant
val SafeGreen = Color(0xFF66BB6A)
val WarningOrange = Color(0xFFFFAB40)
val WarningYellow = Color(0xFFFFD600)
val Gold = Color(0xFFFFD700)

// Premium Gradient
val PremiumStart = Color(0xFF6200EE)
val PremiumEnd = Color(0xFFBB86FC)