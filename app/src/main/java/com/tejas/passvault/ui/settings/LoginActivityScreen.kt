package com.tejas.passvault.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tejas.passvault.auth.LoginEvent
import com.tejas.passvault.auth.LoginEventType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")

private fun LoginEvent.label(): String {
    val kind = when (type) {
        LoginEventType.PATTERN -> "Pattern"
        LoginEventType.BIOMETRIC -> "Fingerprint / face"
        LoginEventType.RECOVERY -> "Security question recovery"
    }
    return if (success) "$kind unlock" else "Incorrect $kind attempt"
}

private fun LoginEvent.formattedTime(): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(FORMATTER)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginActivityScreen(events: List<LoginEvent>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login activity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (events.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp)) {
                Text(
                    "No login activity recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(events) { event -> LoginEventRow(event) }
            }
        }
    }
}

@Composable
private fun LoginEventRow(event: LoginEvent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = if (event.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        Icon(
            imageVector = if (event.success) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = color
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                event.label(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (event.success) MaterialTheme.colorScheme.onSurface else color
            )
            Text(event.formattedTime(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
