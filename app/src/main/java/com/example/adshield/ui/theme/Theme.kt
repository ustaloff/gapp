package com.example.adshield.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = BackgroundDark,
    secondary = NeonCyan,
    onSecondary = BackgroundDark,
    tertiary = NeonPurple,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = TerminalBgDark,
    onSurfaceVariant = NeonGreen.copy(alpha = 0.7f),
    outline = NeonCyan.copy(alpha = 0.3f), // Cyan outlines for structure
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = NeonGreenDark, // Darker green for better contrast on light bg
    onPrimary = Color.White,
    secondary = NeonGreen,
    onSecondary = BackgroundDark,
    tertiary = SafeGreen,
    background = BackgroundLight,
    onBackground = Color(0xFF0a160e), // Very dark green/black
    surface = SurfaceLight,
    onSurface = Color(0xFF0a160e),
    surfaceVariant = TerminalBgLight,
    onSurfaceVariant = NeonGreenDark.copy(alpha = 0.8f),
    outline = NeonGreenDark.copy(alpha = 0.2f)
)

@Composable
fun AdShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // We default to FALSE to strictly enforce the Industrial Brand Identity
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}