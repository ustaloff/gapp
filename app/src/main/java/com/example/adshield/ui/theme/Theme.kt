package com.example.adshield.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color


// Enum to define available themes
enum class AppTheme {
    CyberGreen,
    CyberBlue,
    CyberAmber
}

// Helper to generate ColorScheme based on selected theme
private fun getAdShieldScheme(theme: AppTheme): androidx.compose.material3.ColorScheme {
    // We strictly enforce Dark Mode design for "Industrial Brand"


    return when (theme) {
        AppTheme.CyberGreen -> darkColorScheme(
            // === Main Accents ===
            primary = NeonGreenPrimary,
            onPrimary = NeonGreenSurface, // Black-ish text on Green button is readable
            primaryContainer = NeonGreenPrimary.copy(alpha = 0.2f),
            onPrimaryContainer = NeonGreenPrimary,

            // === Secondary / Content ===
            secondary = NeonGreenSecondary, // White
            onSecondary = NeonGreenSurface,
            secondaryContainer = NeonGreenSurface,
            onSecondaryContainer = NeonGreenSecondary,

            // === Backgrounds ===
            background = NeonGreenSurface,
            onBackground = NeonGreenSecondary,
            surface = NeonGreenSurface,
            onSurface = NeonGreenSecondary,
            surfaceVariant = NeonGreenSurface, // Can be slightly lighter if needed
            onSurfaceVariant = NeonGreenSecondary.copy(alpha = 0.7f),

            // === Errors & Status ===
            error = NeonGreenError,
            onError = NeonGreenSurface,

            // === Borders ===
            outline = NeonGreenPrimary.copy(alpha = 0.5f),
            outlineVariant = NeonGreenPrimary.copy(alpha = 0.2f)
        )

        AppTheme.CyberBlue -> darkColorScheme(
            // === Main Accents ===
            primary = NeonBluePrimary,
            onPrimary = NeonBlueSurface,
            primaryContainer = NeonBluePrimary.copy(alpha = 0.2f),
            onPrimaryContainer = NeonBluePrimary,

            // === Secondary / Content ===
            secondary = NeonBlueSecondary,
            onSecondary = NeonBlueSurface,
            secondaryContainer = NeonBlueSurface,
            onSecondaryContainer = NeonBlueSecondary,

            // === Backgrounds ===
            background = NeonBlueSurface,
            onBackground = NeonBlueSecondary,
            surface = NeonBlueSurface,
            onSurface = NeonBlueSecondary,
            surfaceVariant = NeonBlueSurface,
            onSurfaceVariant = NeonBlueSecondary.copy(alpha = 0.7f),

            // === Errors & Status ===
            error = NeonBlueError,
            onError = NeonBlueSurface,

            // === Borders ===
            outline = NeonBluePrimary.copy(alpha = 0.5f),
            outlineVariant = NeonBluePrimary.copy(alpha = 0.2f)
        )

        AppTheme.CyberAmber -> darkColorScheme(
            // === Main Accents ===
            primary = NeonAmberPrimary,
            onPrimary = NeonAmberSurface,
            primaryContainer = NeonAmberPrimary.copy(alpha = 0.2f),
            onPrimaryContainer = NeonAmberPrimary,

            // === Secondary / Content ===
            secondary = NeonAmberSecondary,
            onSecondary = NeonAmberSurface,
            secondaryContainer = NeonAmberSurface,
            onSecondaryContainer = NeonAmberSecondary,

            // === Backgrounds ===
            background = NeonAmberSurface,
            onBackground = NeonAmberSecondary,
            surface = NeonAmberSurface,
            onSurface = NeonAmberSecondary,
            surfaceVariant = NeonAmberSurface,
            onSurfaceVariant = NeonAmberSecondary.copy(alpha = 0.7f),

            // === Errors & Status ===
            error = NeonAmberError,
            onError = NeonAmberSurface,

            // === Borders ===
            outline = NeonAmberPrimary.copy(alpha = 0.5f),
            outlineVariant = NeonAmberPrimary.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun AdShieldTheme(
    appTheme: AppTheme = AppTheme.CyberGreen, // Default to Green
    content: @Composable () -> Unit
) {
    // Generate the color scheme based on the selected enum
    val colorScheme = getAdShieldScheme(appTheme)
    val customColors = getAdShieldColors(appTheme)

    CompositionLocalProvider(
        LocalAdShieldColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = MaterialCyberShapes, // <--- Connect standard shapes!
            typography = Typography,
            content = content
        )
    }
}

private fun getAdShieldColors(theme: AppTheme): AdShieldColors {
    return when (theme) {
        AppTheme.CyberGreen -> AdShieldColors(
            warning = NeonGreenWarning,
            success = NeonGreenSuccess,
            info = NeonGreenInfo,
            premium = NeonGreenPremium,
            premiumStart = NeonGreenPremiumStart,
            premiumEnd = NeonGreenPremiumEnd
        )

        AppTheme.CyberBlue -> AdShieldColors(
            warning = NeonBlueWarning,
            success = NeonBlueSuccess,
            info = NeonBlueInfo,
            premium = NeonBluePremium,
            premiumStart = NeonBluePremiumStart,
            premiumEnd = NeonBluePremiumEnd
        )

        AppTheme.CyberAmber -> AdShieldColors(
            warning = NeonAmberWarning,
            success = NeonAmberSuccess,
            info = NeonAmberInfo,
            premium = NeonAmberPremium,
            premiumStart = NeonAmberPremiumStart,
            premiumEnd = NeonAmberPremiumEnd
        )
    }
}

// Helper to access shapes easily
object AdShieldTheme {
    val colors: AdShieldColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAdShieldColors.current
}

@Immutable
data class AdShieldColors(
    val warning: Color,
    val success: Color,
    val info: Color,
    val premium: Color,
    val premiumStart: Color,
    val premiumEnd: Color
)

val LocalAdShieldColors = staticCompositionLocalOf {
    AdShieldColors(
        warning = Color.Unspecified,
        success = Color.Unspecified,
        info = Color.Unspecified,
        premium = Color.Unspecified,
        premiumStart = Color.Unspecified,
        premiumEnd = Color.Unspecified
    )
}