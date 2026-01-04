package com.example.adshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.data.BillingManager
import android.app.Activity

@Composable
fun PremiumScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val isPremium by BillingManager.isPremium.collectAsState()
    var isLoading by remember { mutableStateOf(false) }

    // Mock Pricing
    val packageMonthly = BillingManager.MockPackage("pro_monthly", BillingManager.MockProduct("$2.49", "Monthly", "Billed monthly"))
    val packageYearly = BillingManager.MockPackage("pro_yearly", BillingManager.MockProduct("$11.99", "Yearly", "Billed annually"))
    
    var selectedPackage by remember { mutableStateOf<BillingManager.MockPackage?>(packageYearly) }

    // Colors
    val neonGreen = Color(0xFF1BFF80) // Toxic Green
    val darkBg = Color(0xFF050505) // Vantablack-ish
    val surfaceColor = Color(0xFF111111)
    val warningColor = Color(0xFFFFAB40) // Amber for 68%

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Close Button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onBackClick) {
                    Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            // Headline
            Text(
                "Your traffic is visible.\nMake it invisible.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "Upgrade to Premium to unlock full protection and encrypted DNS tunnels.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(32.dp))

            // SECURITY SCORES
            // Current (Free)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CURRENT PROTECTION", color = Color.Gray, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("68% - VULNERABLE", color = Color.Gray, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { 0.68f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = warningColor,
                trackColor = surfaceColor
            )

            Spacer(Modifier.height(16.dp))

            // Premium (Target)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(androidx.compose.material.icons.Icons.Default.Lock, null, tint = neonGreen, modifier = Modifier.size(12.dp))
                     Spacer(Modifier.width(4.dp))
                     Text("PREMIUM PROTECTION", color = neonGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Text("100% - ENCRYPTED", color = neonGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            // Animated Glow Effect (Simulated via Brush)
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(
                brush = Brush.horizontalGradient(
                    colors = listOf(neonGreen.copy(alpha=0.8f), neonGreen)
                )
            ))

            Spacer(Modifier.height(32.dp))

            // BENEFITS CARDS
            BenefitCard("Hide activity from ISP", "Mask your digital footprint completely", androidx.compose.material.icons.Icons.Default.VisibilityOff, neonGreen, surfaceColor)
            Spacer(Modifier.height(12.dp))
            BenefitCard("Block ads in apps", "Stop trackers & popups system-wide", androidx.compose.material.icons.Icons.Default.Security, neonGreen, surfaceColor)
            Spacer(Modifier.height(12.dp))
            BenefitCard("2x Faster Loading", "Optimized DNS for speed", androidx.compose.material.icons.Icons.Default.Speed, neonGreen, surfaceColor)

            Spacer(Modifier.height(32.dp))

            // PRICING
            if (isPremium) {
                Text("PREMIUM ACTIVE", color = neonGreen, style = MaterialTheme.typography.headlineLarge)
            } else {
                Row(
                    Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Monthly
                    PricingCard(
                        modifier = Modifier.weight(1f),
                        title = "MONTHLY",
                        price = "$2.49",
                        sub = "/ month",
                        selected = selectedPackage == packageMonthly,
                        onClick = { selectedPackage = packageMonthly },
                        surfaceColor = surfaceColor,
                        accentColor = neonGreen
                    )

                    // Yearly
                    PricingCard(
                        modifier = Modifier.weight(1f),
                        title = "YEARLY",
                        price = "$11.99",
                        sub = "/ year",
                        badge = "BEST VALUE -60%",
                        selected = selectedPackage == packageYearly,
                        onClick = { selectedPackage = packageYearly },
                        surfaceColor = surfaceColor,
                        accentColor = neonGreen
                    )
                }

                Spacer(Modifier.height(24.dp))

                // CTA BUTTON
                Button(
                    onClick = {
                        selectedPackage?.let { pkg ->
                            BillingManager.purchase(context as Activity, pkg) { loading -> isLoading = loading }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = neonGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Start 14-Day Free Trial", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                Text("No commitment. Cancel anytime.", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("RESTORE", color = Color.DarkGray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable { BillingManager.restorePurchases(context) {} })
                    Text("TERMS", color = Color.DarkGray, style = MaterialTheme.typography.labelSmall)
                    Text("PRIVACY", color = Color.DarkGray, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun BenefitCard(title: String, subtitle: String, icon: ImageVector, accent: Color, bg: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = accent.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Icon(androidx.compose.material.icons.Icons.Default.Lock, null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun PricingCard(
    modifier: Modifier,
    title: String,
    price: String,
    sub: String,
    badge: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    surfaceColor: Color,
    accentColor: Color
) {
    Box(modifier = modifier) {
        // Card Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (badge != null) 12.dp else 0.dp) // Space for badge
                .border(if (selected) 2.dp else 0.dp, if (selected) accentColor else Color.Transparent, RoundedCornerShape(16.dp))
                .background(surfaceColor, RoundedCornerShape(16.dp))
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Text(title, color = Color.Gray, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(price, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(sub, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(24.dp))
            // Radio Circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, if (selected) accentColor else Color.Gray, CircleShape)
                    .padding(4.dp)
            ) {
                if (selected) {
                    Box(Modifier.fillMaxSize().background(accentColor, CircleShape))
                }
            }
        }
        
        // Badge Overlay
        if (badge != null) {
            Surface(
                color = accentColor,
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.Black,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
