package com.example.adshield.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.R

// Placeholder for Google Sign-In button interaction
// Actual implementation requires Activity logic, so we pass a callback
@Composable
fun AuthScreen(
    onSignInClick: () -> Unit,
    onSkipClick: () -> Unit // For development/skipping
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo_final),
                contentDescription = "AdShield Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "ADSHIELD",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "SECURE YOUR DIGITAL LIFE",
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Sign In Button
            Button(
                onClick = onSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                // Placeholder Icon
                // Icon(painter = painterResource(id = R.drawable.ic_google_logo), contentDescription = null) 
                Spacer(Modifier.width(8.dp))
                Text("SIGN IN WITH GOOGLE")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSkipClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                Text("CONTINUE AS GUEST", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
