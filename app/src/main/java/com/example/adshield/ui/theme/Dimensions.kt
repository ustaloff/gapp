package com.example.adshield.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized UI dimensions for consistent spacing and sizing across the app.
 * Using these constants instead of hardcoded values improves maintainability.
 */
object Dimensions {
    // Padding & Spacing
    val spacingXS = 4.dp
    val spacingSM = 8.dp
    val spacingMD = 12.dp
    val spacingLG = 16.dp
    val spacingXL = 24.dp
    val spacingXXL = 32.dp
    
    // Screen padding
    val screenPaddingHorizontal = 24.dp
    val screenPaddingVertical = 16.dp
    
    // Card & Container
    val cardPadding = 16.dp
    val cardBorderWidth = 1.dp
    
    // Icon sizes
    val iconSizeXS = 12.dp
    val iconSizeSM = 16.dp
    val iconSizeMD = 24.dp
    val iconSizeLG = 32.dp
    val iconSizeXL = 48.dp
    
    // Component heights
    val buttonHeight = 48.dp
    val buttonHeightSmall = 32.dp
    val chipHeight = 32.dp
    val textFieldHeight = 56.dp
    val navBarHeight = 80.dp
    val terminalHeight = 200.dp
    val graphHeight = 200.dp
    
    // Corner radius
    val cornerRadiusSM = 4.dp
    val cornerRadiusMD = 8.dp
    val cornerRadiusLG = 12.dp
    val cornerRadiusXL = 24.dp
    val cornerRadiusFull = 999.dp  // For pill shapes
    
    // Bottom navigation spacer (to avoid overlap)
    val bottomNavSpacer = 130.dp
}

/**
 * Typography sizes for consistent text scaling.
 */
object TextSizes {
    val labelXS = 10.sp
    val labelSM = 12.sp
    val labelMD = 14.sp
    val bodyMD = 16.sp
    val titleSM = 18.sp
    val titleMD = 20.sp
    val titleLG = 24.sp
    val headlineMD = 28.sp
    val headlineLG = 32.sp
}

/**
 * Animation durations for consistent motion design.
 */
object AnimDurations {
    const val fast = 150
    const val normal = 300
    const val slow = 500
    const val pulse = 800
    const val shimmer = 2000
}

/**
 * Alpha values for consistent transparency.
 */
object Alphas {
    const val disabled = 0.4f
    const val dim = 0.5f
    const val subtle = 0.7f
    const val full = 1f
    
    // Background alphas
    const val backgroundLight = 0.01f
    const val backgroundMedium = 0.1f
    const val backgroundDark = 0.3f
    
    // Border alphas  
    const val borderLight = 0.1f
    const val borderMedium = 0.3f
    const val borderStrong = 0.5f
}
