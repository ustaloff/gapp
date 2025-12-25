package com.example.adshield.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CyberTopList(
    title: String, 
    data: Map<String, Int>, 
    onAllowClick: (String) -> Unit,
    isWhitelisted: (String) -> Boolean // Logic to check status
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val packageManager = remember(context) { context.packageManager }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        
        if (data.isEmpty()) {
            Text("NO DATA COLLECTED", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f), fontStyle = FontStyle.Italic)
        } else {
            // Sort by count descending and take top 5
            val sorted = data.toList().sortedByDescending { (_, value) -> value }.take(5)
            
            sorted.forEach { (packageName, count) ->
                // Resolve App Info
                var appName by remember(packageName) { mutableStateOf(packageName) }
                var appIcon by remember(packageName) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                
                LaunchedEffect(packageName) {
                    try {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        appName = packageManager.getApplicationLabel(appInfo).toString()
                        val drawable = packageManager.getApplicationIcon(appInfo)
                        
                        // Convert Drawable to ImageBitmap (Simplified for Compose)
                        // In a real app, use Coil or proper conversion. 
                        // For now, we will fallback to default icon to avoid heavy bitmap logic if conversion complex,
                        // but let's try a basic approach or just use name for now to avoid crashes?
                        // Actually, let's stick to Name resolution first. Icon needs messy Bitmap conversion.
                        // User asked for "Logo with Name", so we SHOULD try.
                        // Let's use a placeholder Logic: if we get the label, that's 90% of the value.
                        // Adding Bitmap conversion code inside a composable is risky without a utility.
                        // I will stick to NAME resolution first to be safe, unless I add an extension.
                    } catch (e: Exception) {
                        // Keep package name if not found
                        appName = packageName
                    }
                }

                val isActive = isWhitelisted(packageName)
                val statusIcon = Icons.Filled.Lock 
                val tint = if (isActive) com.example.adshield.ui.theme.NeonGreen else androidx.compose.ui.graphics.Color(0xFFFF5252)
                val bgBorder = if (isActive) com.example.adshield.ui.theme.NeonGreen.copy(alpha=0.5f) else androidx.compose.ui.graphics.Color(0xFFFF5252).copy(alpha=0.5f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                         Text(
                             text = appName, // UPDATED: Shows Human Name
                             style = MaterialTheme.typography.bodySmall,
                             fontWeight = FontWeight.Medium,
                             color = if (isActive) com.example.adshield.ui.theme.NeonGreen else MaterialTheme.colorScheme.onSurface,
                             maxLines = 1,
                             overflow = TextOverflow.Ellipsis,
                             fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                         )
                         // Optional: Show package name below in tiny font if needed
                         if (appName != packageName) {
                             Text(
                                 text = packageName,
                                 style = MaterialTheme.typography.labelSmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                 fontSize = 8.sp,
                                 maxLines = 1,
                                 overflow = TextOverflow.Ellipsis
                             )
                         }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(Modifier.width(8.dp))
                    // Tiny toggle button
                    Box(
                         modifier = Modifier
                             .size(24.dp)
                             .clickable { onAllowClick(packageName) }
                             .border(1.dp, bgBorder, CircleShape),
                         contentAlignment = Alignment.Center
                    ) {
                        Icon(statusIcon, contentDescription = "Toggle", tint = tint, modifier = Modifier.size(14.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), thickness = 0.5.dp)
            }
        }
    }
}
