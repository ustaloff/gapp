package com.example.adshield.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.List
import com.example.adshield.ui.theme.AdShieldTheme

@Composable
fun CyberNavBar(
    isRunning: Boolean,
    onPowerClick: () -> Unit,
    currentScreen: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center // Center content in the box
    ) {
        // Floating Pill Container
        Surface(
            modifier = Modifier
                .height(72.dp) // Height for the bar
                .fillMaxWidth(),
            shape = AdShieldTheme.shapes.menu, // Fully rounded pill
            //shape = RoundedCornerShape(3.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            ),
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. HOME
                NavBarItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = currentScreen == "HOME",
                    onClick = { onNavigate("HOME") }
                )

                // 2. LOGS
                NavBarItem(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "Logs",
                    isSelected = currentScreen == "LOGS",
                    onClick = { onNavigate("LOGS") }
                )

                // 3. CENTRAL SPACER (Hidden/Transparent to hold space)
                Spacer(modifier = Modifier.width(64.dp))

                // 4. STATS
                NavBarItem(
                    icon = androidx.compose.material.icons.Icons.Default.Info,
                    label = "Stats",
                    isSelected = currentScreen == "STATS",
                    onClick = { onNavigate("STATS") }
                )

                // 5. SETTINGS
                NavBarItem(
                    icon = Icons.Default.Settings,
                    label = "Config",
                    isSelected = currentScreen == "SETTINGS",
                    onClick = { onNavigate("SETTINGS") }
                )
            }
        }

        // CENTER FLOATING POWER BUTTON (No Offset = Vertically Centered)
        CyberMiniPowerButton(
            isRunning = isRunning,
            onClick = onPowerClick,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun NavBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
