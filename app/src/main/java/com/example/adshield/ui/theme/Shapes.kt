package com.example.adshield.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class AdShieldShapes(
    val container: Shape,        // Main container shape for cards, sections
    val indicator: Shape,        // Small UI elements like progress bars, status indicators
    val screen: Shape,           // Graph section background or specific screen containers
    val window: Shape,           // Top-level containers, dialog backgrounds
    val dialog: Shape,           // Floating modal dialogs (alerts, confirmations)
    val button: Shape,           // Standard buttons (FilterChip, Buttons)
    val input: Shape,            // Text input fields (OutlinedTextField)
    val menu: Shape,             // Main menu container or bottom sheet top edge
    val back: Shape,             // Back navigation button specific shape
    val add: Shape,              // Add (+) button (FAB-like or standard button)
    val setting: Shape,          // Settings item containers
    val entity: Shape,           // List items in App/Domain lists (rows)
    val banner: Shape,           // Promotional or Hero banners
    val icon: Shape              // App icons or avatar clipping shape
)


val CyberShapes = AdShieldShapes(
    container = RoundedCornerShape(5.dp),
    indicator = RoundedCornerShape(2.dp),
    screen = RoundedCornerShape(2.dp),
    window = RoundedCornerShape(0.dp),
    dialog = RoundedCornerShape(8.dp),
    button = RoundedCornerShape(5.dp),
    input = RoundedCornerShape(5.dp),
    menu = RoundedCornerShape(50),
    back = RoundedCornerShape(5.dp),
    add = RoundedCornerShape(5.dp),
    setting = RoundedCornerShape(8.dp),
    entity = RoundedCornerShape(8.dp),
    banner = RoundedCornerShape(16.dp),
    icon = RoundedCornerShape(50)
)

val RoundShapes = AdShieldShapes(
    container = RoundedCornerShape(16.dp),
    indicator = RoundedCornerShape(8.dp),
    screen = RoundedCornerShape(2.dp),
    window = RoundedCornerShape(12.dp),
    dialog = RoundedCornerShape(28.dp),
    button = RoundedCornerShape(50),
    input = RoundedCornerShape(5.dp),
    menu = RoundedCornerShape(50),
    back = RoundedCornerShape(50),
    add = RoundedCornerShape(50),
    setting = RoundedCornerShape(8.dp),
    entity = RoundedCornerShape(8.dp),
    banner = RoundedCornerShape(16.dp),
    icon = RoundedCornerShape(50)
)

val LocalAdShieldShapes = staticCompositionLocalOf { CyberShapes }
