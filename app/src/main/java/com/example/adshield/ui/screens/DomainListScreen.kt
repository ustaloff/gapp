package com.example.adshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.example.adshield.ui.components.CyberChip
import com.example.adshield.ui.components.GridBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


enum class DomainTab {
    ALL, BLOCKED, ALLOWED
}

data class DomainUiModel(
    val domain: String,
    val isBlocked: Boolean
)

@Composable
fun DomainListScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context) }

    // State
    var currentTab by remember { mutableStateOf(DomainTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Fetch lists dynamically
    // We use a key to force refresh when tab changes or after an operation
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val domains by produceState(
        initialValue = emptyList(),
        key1 = currentTab,
        key2 = refreshTrigger,
        key3 = searchQuery
    ) {
        value = withContext(Dispatchers.IO) {
            val blockedList = preferences.getUserBlocklist().map { DomainUiModel(it, true) }
            val allowedList = preferences.getUserAllowlist().map { DomainUiModel(it, false) }

            val combinedList = when (currentTab) {
                DomainTab.ALL -> blockedList + allowedList
                DomainTab.BLOCKED -> blockedList
                DomainTab.ALLOWED -> allowedList
            }

            // Filter and Sort
            combinedList
                .filter { it.domain.contains(searchQuery, ignoreCase = true) }
                .sortedBy { it.domain }
        }
    }

    val emptyText = if (searchQuery.isNotEmpty()) "> NO MATCHES FOUND"
    else when (currentTab) {
        DomainTab.ALL -> "> NO DOMAINS CONFIGURED"
        DomainTab.BLOCKED -> "> NO BANNED DOMAINS"
        DomainTab.ALLOWED -> "> NO ALLOWED DOMAINS"
    }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.shapes.medium
                        )
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.shapes.medium
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "DOMAIN MANAGER",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            }

            // SEARCH
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace
                ),
                placeholder = {
                    Text(
                        "SEARCH_DOMAIN...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                ),
                shape = MaterialTheme.shapes.small,
                singleLine = true
            )

            // UNIFIED FILTER CHIPS (Like LogsScreen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CyberChip(
                    text = "ALL",
                    selected = currentTab == DomainTab.ALL,
                    onClick = { currentTab = DomainTab.ALL },
                    modifier = Modifier.weight(1f)
                )
                CyberChip(
                    text = "BLOCKED",
                    selected = currentTab == DomainTab.BLOCKED,
                    onClick = { currentTab = DomainTab.BLOCKED },
                    modifier = Modifier.weight(1f)
                )
                CyberChip(
                    text = "ALLOWED",
                    selected = currentTab == DomainTab.ALLOWED,
                    onClick = { currentTab = DomainTab.ALLOWED },
                    modifier = Modifier.weight(1f)
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp), // Space for FAB
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(domains, key = { it.domain }) { item ->
                        DomainItem(
                            item = item,
                            onDelete = {
                                if (item.isBlocked) {
                                    FilterEngine.removeFromBlocklist(context, item.domain)
                                } else {
                                    FilterEngine.removeFromAllowlist(context, item.domain)
                                }
                                refreshTrigger++ // Refresh list
                                // Force stats refresh
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
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Domain")
        }

        // ADD DIALOG
        if (showAddDialog) {
            AddDomainDialog(
                initialIsBlocked = currentTab != DomainTab.ALLOWED, // Default to blocked unless explicitly in Allowed tab
                allowTypeSelection = currentTab == DomainTab.ALL,
                onDismiss = { showAddDialog = false },
                onAdd = { domain, isBlocked ->
                    if (isBlocked) {
                        FilterEngine.addToBlocklist(context, domain)
                    } else {
                        FilterEngine.addToAllowlist(context, domain)
                    }
                    refreshTrigger++
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
    item: DomainUiModel,
    onDelete: () -> Unit
) {
    val borderColor =
        if (item.isBlocked) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(
            alpha = 0.5f
        )

    val icon = if (item.isBlocked) Icons.Default.Close else Icons.Default.CheckCircle
    val iconTint =
        if (item.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                borderColor,
                MaterialTheme.shapes.extraSmall
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.domain,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

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
    initialIsBlocked: Boolean,
    allowTypeSelection: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, Boolean) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isBlocked by remember { mutableStateOf(initialIsBlocked) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (allowTypeSelection) "ADD DOMAIN" else if (isBlocked) "BLOCK DOMAIN" else "ALLOW DOMAIN",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Domain (e.g. google.com)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (text.isNotBlank()) onAdd(text, isBlocked)
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )

                if (allowTypeSelection) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CyberChip(
                            text = "BLOCK",
                            selected = isBlocked,
                            onClick = { isBlocked = true },
                            modifier = Modifier.weight(1f)
                        )
                        CyberChip(
                            text = "ALLOW",
                            selected = !isBlocked,
                            onClick = { isBlocked = false },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onAdd(text, isBlocked) },
                enabled = text.isNotBlank(),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text("ADD")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant, // Neutral Dark
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Text("CANCEL")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium
    )
}
