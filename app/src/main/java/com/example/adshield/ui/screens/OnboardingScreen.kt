package com.example.adshield.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.ui.components.CyberButton
import com.example.adshield.ui.components.GridBackground
import com.example.adshield.ui.theme.NeonBluePrimary
import com.example.adshield.ui.theme.NeonGreenPrimary
import com.example.adshield.ui.theme.NeonGreenError
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinish: (Boolean) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Deep Black
    ) {
        // High-Tech Grid Background
        GridBackground(gridColor = NeonGreenPrimary.copy(alpha = 0.1f))

        // Radial Glow Effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonGreenPrimary.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = 1000f,
                        center = androidx.compose.ui.geometry.Offset.Unspecified
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top Bar: Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "SKIP >>",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clickable { onFinish(false) }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Carousel Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(8f) // Take most space
            ) { page ->
                OnboardingSlide(page = page)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicators Pager
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        val width by animateDpAsState(
                            if (isSelected) 24.dp else 8.dp,
                            label = "dotWidth"
                        )
                        val color =
                            if (isSelected) NeonGreenPrimary else Color.White.copy(alpha = 0.2f)

                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(width)
                                .background(color, CircleShape)
                        )
                    }
                }

                // Primary Action Button
                val buttonText =
                    if (pagerState.currentPage == 2) "START ${com.example.adshield.data.AppConfig.TRIAL_DURATION_DAYS}-DAY FREE TRIAL" else "CONTINUE"

                CyberButton(
                    text = buttonText,
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinish(true)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }
        }
    }
}

@Composable
fun OnboardingSlide(page: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        // Visual Container
        Box(
            modifier = Modifier
                .size(280.dp)
                .border(1.dp, Color.White.copy(alpha = 0.1f), MaterialTheme.shapes.medium)
                .background(Color.White.copy(alpha = 0.02f), MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            when (page) {
                0 -> ScreenOneVisual()
                1 -> ScreenTwoVisual()
                2 -> ScreenThreeVisual()
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Text Content
        val (title, subtitle) = when (page) {
            0 -> "Ads and trackers\nare everywhere" to "They track you inside apps and websites"
            1 -> "One tap.\nFull control." to "AdShield blocks ads and trackers at the network level"
            2 -> "Make your traffic\ninvisible" to "Encrypted DNS hides your activity from providers"
            else -> "" to ""
        }

        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

// --- Custom Visuals for each screen ---

@Composable
fun ScreenOneVisual() {
    // Apps emitting signals (Red dots)
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ), label = "alpha"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppIconStub(isRed = true, alpha = alpha)
            AppIconStub(isRed = true, alpha = alpha)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppIconStub(isRed = true, alpha = alpha)
            AppIconStub(isRed = true, alpha = alpha)
        }
        Text(
            "THREATS DETECTED",
            color = NeonGreenError,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .padding(top = 16.dp)
                .alpha(alpha)
        )
    }
}

@Composable
fun AppIconStub(isRed: Boolean, alpha: Float) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .background(Color.White.copy(alpha = 0.05f), MaterialTheme.shapes.small)
    ) {
        if (isRed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(8.dp)
                    .background(NeonGreenError.copy(alpha = alpha), CircleShape)
            )
        }
    }
}

@Composable
fun ScreenTwoVisual() {
    // Shield blocking lines
    Box(contentAlignment = Alignment.Center) {
        // Shield
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = NeonGreenPrimary,
            modifier = Modifier.size(100.dp)
        )
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(160.dp)
                .border(1.dp, NeonGreenPrimary.copy(alpha = 0.3f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .border(1.dp, NeonGreenPrimary.copy(alpha = 0.1f), CircleShape)
        )
    }
}

@Composable
fun ScreenThreeVisual() {
    // Encrypted Device
    Box(contentAlignment = Alignment.Center) {
        // Aura
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonBluePrimary.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
        )

        Icon(
            imageVector = Icons.Default.VisibilityOff, // Eye Off representing Invisibility
            contentDescription = null,
            tint = NeonBluePrimary,
            modifier = Modifier.size(80.dp)
        )

        // Orbiting electron effect
        val infiniteTransition = rememberInfiniteTransition(label = "orbit")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing)
            ), label = "rotation"
        )

        Box(
            modifier = Modifier
                .size(160.dp)
                .border(2.dp, NeonBluePrimary.copy(alpha = 0.3f), CircleShape)
                .padding(4.dp)
        )
    }
}
