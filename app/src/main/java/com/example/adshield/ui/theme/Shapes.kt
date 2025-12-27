package com.example.adshield.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class AdShieldShapes(
    val container: Shape,
    val indicator: Shape, // For progress bars, small elements
    val window: Shape,     // For top-level containers/dialogs
    val dialog: Shape,     // For floating dialogs
    val button: Shape,
    val back: Shape,// For settings back button

    // New variables
    val setting: Shape, // For settings boxes
    val entity: Shape, // For app/domain lists
    val menu: Shape, // For main menu
    val banner: Shape, // For banner
    val input: Shape, //  For input text
    val screen: Shape, //  For graph screen
    val icon: Shape, //  For app icon
    val add: Shape, //  For settings add button

)


val CyberShapes = AdShieldShapes(
    container = RoundedCornerShape(5.dp),
    indicator = RoundedCornerShape(2.dp),
    window = RoundedCornerShape(0.dp),
    dialog = RoundedCornerShape(8.dp),
    button = RoundedCornerShape(5.dp),
    back = RoundedCornerShape(5.dp),

    setting = RoundedCornerShape(8.dp),
    entity = RoundedCornerShape(8.dp),
    menu = RoundedCornerShape(50),
    banner = RoundedCornerShape(16.dp),
    input = RoundedCornerShape(5.dp),
    screen = RoundedCornerShape(2.dp),
    icon = RoundedCornerShape(50),
    add = RoundedCornerShape(5.dp),
)

val RoundShapes = AdShieldShapes(
    container = RoundedCornerShape(16.dp),
    indicator = RoundedCornerShape(8.dp),
    window = RoundedCornerShape(12.dp),
    dialog = RoundedCornerShape(28.dp), // Standard Material3
    button = RoundedCornerShape(50), // Fully rounded
    back = RoundedCornerShape(50),

    setting = RoundedCornerShape(8.dp),
    entity = RoundedCornerShape(8.dp),
    menu = RoundedCornerShape(50),
    banner = RoundedCornerShape(16.dp),
    input = RoundedCornerShape(5.dp),
    screen = RoundedCornerShape(2.dp),
    icon = RoundedCornerShape(50),
    add = RoundedCornerShape(50),
)

val LocalAdShieldShapes = staticCompositionLocalOf { CyberShapes }
