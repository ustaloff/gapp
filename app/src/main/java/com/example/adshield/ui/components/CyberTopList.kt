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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun CyberTopList(
    title: String, 
    data: Map<String, Int>, 
    onAllowClick: (String) -> Unit,
    isWhitelisted: (String) -> Boolean // Logic to check status
) {
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
            
            sorted.forEach { (name, count) ->
                val isActive = isWhitelisted(name)
                val icon = Icons.Filled.Lock // Always Lock. Green = Open/Allowed, Red = Closed/Blocked.
                // Red for Lock (0xFFFF5252), Green for Allowed
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
                             text = name,
                             style = MaterialTheme.typography.bodySmall,
                             fontWeight = FontWeight.Medium,
                             color = if (isActive) com.example.adshield.ui.theme.NeonGreen else MaterialTheme.colorScheme.onSurface,
                             maxLines = 1,
                             overflow = TextOverflow.Ellipsis,
                             fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                         )
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
                             .clickable { onAllowClick(name) }
                             .border(1.dp, bgBorder, CircleShape),
                         contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = "Toggle", tint = tint, modifier = Modifier.size(14.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), thickness = 0.5.dp)
            }
        }
    }
}
