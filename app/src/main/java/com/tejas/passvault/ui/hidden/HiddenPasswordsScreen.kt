package com.tejas.passvault.ui.hidden

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tejas.passvault.VaultViewModel
import com.tejas.passvault.ui.vault.AddEditEntryScreen
import com.tejas.passvault.ui.vault.EntryDetailScreen

private sealed interface PasswordsMode {
    object Browse : PasswordsMode
    class Detail(val id: String) : PasswordsMode
    class Edit(val id: String?) : PasswordsMode
}

/**
 * The hidden vault's own password list. Reuses the same detail and add/edit screens as the main
 * vault, just fed from the hidden vault's separately-encrypted store.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenPasswordsScreen(vm: VaultViewModel, onBack: () -> Unit) {
    val credentials by vm.hiddenCredentials.collectAsState()
    var mode by remember { mutableStateOf<PasswordsMode>(PasswordsMode.Browse) }

    BackHandler(enabled = mode !is PasswordsMode.Browse) { mode = PasswordsMode.Browse }

    when (val current = mode) {
        is PasswordsMode.Browse -> Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Secret passwords") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { mode = PasswordsMode.Edit(null) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add password")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (credentials.isEmpty()) {
                    Text(
                        "Nothing here yet. Tap + to add a secret password.",
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn {
                        items(credentials.sortedBy { it.title.lowercase() }, key = { it.id }) { credential ->
                            ListItem(
                                headlineContent = { Text(credential.title.ifBlank { "(untitled)" }) },
                                supportingContent = { Text(credential.username) },
                                modifier = Modifier.fillMaxWidth().clickable {
                                    mode = PasswordsMode.Detail(credential.id)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        is PasswordsMode.Detail -> {
            val credential = credentials.firstOrNull { it.id == current.id }
            if (credential == null) {
                LaunchedEffect(Unit) { mode = PasswordsMode.Browse }
            } else {
                EntryDetailScreen(
                    credential = credential,
                    onBack = { mode = PasswordsMode.Browse },
                    onEdit = { mode = PasswordsMode.Edit(credential.id) },
                    onDelete = {
                        vm.deleteHiddenCredential(credential.id)
                        mode = PasswordsMode.Browse
                    }
                )
            }
        }

        is PasswordsMode.Edit -> AddEditEntryScreen(
            existing = credentials.firstOrNull { it.id == current.id },
            onSave = { credential ->
                vm.saveHiddenCredential(credential)
                mode = PasswordsMode.Browse
            },
            onBack = { mode = PasswordsMode.Browse }
        )
    }
}
