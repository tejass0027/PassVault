package com.tejas.passvault.ui.vault

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.ClipboardManager
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tejas.passvault.data.Credential
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun copyToClipboard(context: Context, label: String, value: String, sensitive: Boolean) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, value)
    if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val extras = PersistableBundle()
        extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        clip.description.extras = extras
    }
    clipboard.setPrimaryClip(clip)
}

private fun clearClipboardIfUnchanged(context: Context, expected: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val current = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    if (current == expected) {
        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    credential: Credential,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showPassword by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingClearJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun copyAndAutoClear(label: String, value: String, sensitive: Boolean) {
        copyToClipboard(context, label, value, sensitive)
        pendingClearJob?.cancel()
        pendingClearJob = scope.launch {
            delay(30_000)
            clearClipboardIfUnchanged(context, value)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this entry?") },
            text = { Text("\"${credential.title}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(credential.title.ifBlank { "(untitled)" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            DetailField(
                label = "Username",
                value = credential.username,
                onCopy = { copyAndAutoClear("username", credential.username, sensitive = false) }
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text("Password", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (showPassword) credential.password else "•".repeat(credential.password.length.coerceAtMost(16)),
                    style = MaterialTheme.typography.bodyLarge
                )
                Row {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle visibility"
                        )
                    }
                    IconButton(onClick = {
                        copyAndAutoClear("password", credential.password, sensitive = true)
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy password")
                    }
                }
            }

            if (credential.url.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                DetailField(
                    label = "Website",
                    value = credential.url,
                    onCopy = { copyAndAutoClear("url", credential.url, sensitive = false) }
                )
            }
            if (credential.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Notes", style = MaterialTheme.typography.labelMedium)
                Text(credential.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String, onCopy: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(value, style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy $label")
            }
        }
    }
}
