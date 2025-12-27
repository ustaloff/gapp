package com.example.adshield.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    // Hardcoded Offline Package
    val offlinePackage = remember {
        BillingManager.MockPackage(
            "pro_lifetime",
            BillingManager.MockProduct(
                "$4.99",
                "AdShield Pro (Lifetime)",
                "Unlock full protection forever"
            )
        )
    }

    var selectedPackage by remember { mutableStateOf<BillingManager.MockPackage?>(offlinePackage) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Icon(
                androidx.compose.material.icons.Icons.Default.Star,
                contentDescription = null,
                tint = com.example.adshield.ui.theme.Gold,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "ADSHIELD PRO",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "UNLOCK FULL PROTECTION",
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(32.dp))

            // Comparison Table
            FeatureRow("Custom Blocklists", false, true)
            FeatureRow("Faster DNS Proxy", false, true)
            FeatureRow("Priority Support", false, true)
            FeatureRow("Support Development", false, true)

            Spacer(Modifier.height(32.dp))

            if (isPremium) {
                Text(
                    "YOU ARE PREMIUM!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Thank you for your support.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val packages = listOf(offlinePackage)

                packages.forEach { pkg ->
                    val isSelected = selectedPackage == pkg
                    val price = pkg.product.price

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedPackage = pkg }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(pkg.product.title, fontWeight = FontWeight.Bold)
                                Text(
                                    pkg.product.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                price,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        selectedPackage?.let { pkg ->
                            BillingManager.purchase(context as Activity, pkg) { loading ->
                                isLoading = loading
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && selectedPackage != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isLoading) CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    else Text("UNLOCK NOW (OFFLINE)", fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = {
                    BillingManager.restorePurchases(context) { loading -> isLoading = loading }
                }) {
                    Text("Restore Purchases", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Terms of Service | Privacy Policy",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun FeatureRow(text: String, free: Boolean, pro: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, modifier = Modifier.weight(1f))
        Row(modifier = Modifier.width(80.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(
                if (free) androidx.compose.material.icons.Icons.Default.Check else androidx.compose.material.icons.Icons.Default.Close,
                contentDescription = null,
                tint = if (free) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Icon(
                if (pro) androidx.compose.material.icons.Icons.Default.Check else androidx.compose.material.icons.Icons.Default.Close,
                contentDescription = null,
                tint = if (pro) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
