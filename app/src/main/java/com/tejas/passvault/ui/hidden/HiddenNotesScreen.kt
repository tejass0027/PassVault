package com.tejas.passvault.ui.hidden

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tejas.passvault.VaultViewModel
import com.tejas.passvault.data.HiddenNote

/**
 * The hidden vault's contents - a simple list of secret notes. Reachable only by drawing the
 * hidden vault's pattern on the login screen; there is no way to navigate here from the normal
 * vault or Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenNotesScreen(vm: VaultViewModel, onLock: () -> Unit) {
    val notes by vm.hiddenNotes.collectAsState()
    var editingNote by remember { mutableStateOf<HiddenNote?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<HiddenNote?>(null) }

    deleteTarget?.let { note ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete this note?") },
            text = { Text("\"${note.title.ifBlank { "(untitled)" }}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteHiddenNote(note.id)
                    deleteTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }

    val editing = editingNote
    if (isAddingNew || editing != null) {
        HiddenNoteEditor(
            existing = editing,
            onSave = { note ->
                vm.saveHiddenNote(note)
                isAddingNew = false
                editingNote = null
            },
            onCancel = {
                isAddingNew = false
                editingNote = null
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secret notes") },
                navigationIcon = {
                    IconButton(onClick = onLock) {
                        Icon(Icons.Filled.Lock, contentDescription = "Lock")
                    }
                },
                actions = {
                    IconButton(onClick = { isAddingNew = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add note")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (notes.isEmpty()) {
                Text(
                    "Nothing here yet. Tap + to add a secret note.",
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn {
                    items(notes.sortedByDescending { it.updatedAt }, key = { it.id }) { note ->
                        ListItem(
                            headlineContent = { Text(note.title.ifBlank { "(untitled)" }) },
                            supportingContent = {
                                Text(note.content, maxLines = 1)
                            },
                            trailingContent = {
                                IconButton(onClick = { deleteTarget = note }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { editingNote = note }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HiddenNoteEditor(existing: HiddenNote?, onSave: (HiddenNote) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var content by remember { mutableStateOf(existing?.content ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New secret note" else "Edit note") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Secret") },
                modifier = Modifier.fillMaxWidth().height(240.dp).padding(top = 16.dp)
            )
            TextButton(
                onClick = {
                    onSave(
                        existing?.copy(title = title, content = content, updatedAt = System.currentTimeMillis())
                            ?: HiddenNote(title = title, content = content)
                    )
                },
                modifier = Modifier.padding(top = 16.dp)
            ) { Text("Save") }
        }
    }
}
