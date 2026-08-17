package com.tejas.passvault.ui.vault

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.tejas.passvault.VaultViewModel
import com.tejas.passvault.data.Credential

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    vm: VaultViewModel,
    onAddEntry: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onViewLoginActivity: () -> Unit,
    onLock: () -> Unit
) {
    val credentials by vm.credentials.collectAsState()
    val failedAttempts by vm.failedAttemptsSinceLastLogin.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = credentials.filter {
        query.isBlank() ||
            it.title.contains(query, ignoreCase = true) ||
            it.username.contains(query, ignoreCase = true)
    }.sortedBy { it.title.lowercase() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PassVault") },
                navigationIcon = {
                    IconButton(onClick = onLock) {
                        Icon(Icons.Filled.Lock, contentDescription = "Lock")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Filled.Add, contentDescription = "Add entry")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (failedAttempts > 0) {
                FailedAttemptsBanner(
                    count = failedAttempts,
                    onView = onViewLoginActivity,
                    onDismiss = { vm.dismissFailedAttemptsBanner() }
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            if (filtered.isEmpty()) {
                Text(
                    text = if (credentials.isEmpty()) "No saved passwords yet. Tap + to add one."
                    else "No matches",
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { credential ->
                        CredentialRow(credential, onClick = { onOpenEntry(credential.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FailedAttemptsBanner(count: Int, onView: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = if (count == 1) "Someone tried to open PassVault" else "Someone tried to open PassVault $count times",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "$count incorrect attempt${if (count == 1) "" else "s"} before you logged in",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                TextButton(onClick = onView, contentPadding = PaddingValues(0.dp)) {
                    Text("View activity", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun CredentialRow(credential: Credential, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(credential.title.ifBlank { "(untitled)" }) },
        supportingContent = { Text(credential.username) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}
