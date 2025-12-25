package com.example.adshield.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adshield.data.AppPreferences
import com.example.adshield.filter.FilterEngine
import com.example.adshield.ui.components.GridBackground
import kotlinx.coroutines.launch

@Composable
fun DomainListScreen(
    onBackClick: () -> Unit,
    isBlocklist: Boolean = false
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context) }
    
    // Dynamic fetch function
    fun getList(): List<String> {
        return if (isBlocklist) preferences.getUserBlocklist().toList().sorted()
               else preferences.getUserAllowlist().toList().sorted()
    }

    var domains by remember { mutableStateOf(getList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    val listTitle = if (isBlocklist) "BLOCKED CONFIG" else "ALLOWED CONFIG"
    val emptyText = if (isBlocklist) "> NO BANNED DOMAINS" else "> NO ALLOWED DOMAINS"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // HEADER
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(5.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = listTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LIST
            if (domains.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        emptyText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp), // Space for FAB
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(domains) { domain ->
                        DomainItem(
                            domain = domain,
                            onDelete = {
                                if (isBlocklist) {
                                    FilterEngine.removeFromBlocklist(context, domain)
                                } else {
                                    FilterEngine.removeFromAllowlist(context, domain)
                                }
                                domains = getList()
                                // Force stats refresh if needed?
                                com.example.adshield.data.VpnStats.refreshLogStatuses()
                            }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Domain")
        }

        // ADD DIALOG
        if (showAddDialog) {
            AddDomainDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { newDomain ->
                    if (isBlocklist) {
                        FilterEngine.addToBlocklist(context, newDomain)
                    } else {
                        FilterEngine.addToAllowlist(context, newDomain)
                    }
                    domains = getList()
                    showAddDialog = false
                    // Force stats refresh
                    com.example.adshield.data.VpnStats.refreshLogStatuses()
                }
            )
        }
    }
}

@Composable
fun DomainItem(
    domain: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = domain,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun AddDomainDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("ADD DOMAIN", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Domain (e.g. google.com)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (text.isNotBlank()) onAdd(text)
                }),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onAdd(text) },
                enabled = text.isNotBlank()
            ) {
                Text("ADD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
}
