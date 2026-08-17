package com.tejas.passvault.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.tejas.passvault.ui.theme.ThemeMode
import java.time.Year

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: VaultViewModel,
    onBack: () -> Unit,
    onChangePattern: () -> Unit,
    onChangeSecurityQuestions: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenLoginActivity: () -> Unit,
    onErased: () -> Unit
) {
    var biometricEnabled by remember { mutableStateOf(vm.isBiometricEnabled) }
    var autoLockSeconds by remember { mutableStateOf(vm.authPrefs.autoLockSeconds) }
    var showAutoLockMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showEraseConfirm by remember { mutableStateOf(false) }
    val themeMode by vm.themeMode.collectAsState()

    val autoLockOptions = listOf(0 to "Immediately", 30 to "After 30 seconds", 60 to "After 1 minute", 300 to "After 5 minutes")
    val themeOptions = listOf(ThemeMode.SYSTEM to "System default", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")

    if (showEraseConfirm) {
        AlertDialog(
            onDismissRequest = { showEraseConfirm = false },
            title = { Text("Erase all data?") },
            text = { Text("This permanently deletes every saved password, your pattern, and your security questions from this device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showEraseConfirm = false
                    vm.eraseEverything()
                    onErased()
                }) { Text("Erase everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showEraseConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Row {
                ListItem(
                    headlineContent = { Text("Theme") },
                    supportingContent = {
                        Text(themeOptions.first { it.first == themeMode }.second)
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showThemeMenu = true }
                )
                DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                    themeOptions.forEach { (mode, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                vm.setThemeMode(mode)
                                showThemeMenu = false
                            }
                        )
                    }
                }
            }
            HorizontalDivider()

            if (vm.isBiometricAvailable) {
                ListItem(
                    headlineContent = { Text("Fingerprint / face unlock") },
                    supportingContent = { Text("Require biometric verification before your pattern") },
                    trailingContent = {
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                vm.setBiometricEnabled(it)
                            }
                        )
                    }
                )
                HorizontalDivider()
            }

            Row {
                ListItem(
                    headlineContent = { Text("Auto-lock") },
                    supportingContent = {
                        Text(autoLockOptions.firstOrNull { it.first == autoLockSeconds }?.second ?: "After 30 seconds")
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showAutoLockMenu = true }
                )
                DropdownMenu(expanded = showAutoLockMenu, onDismissRequest = { showAutoLockMenu = false }) {
                    autoLockOptions.forEach { (seconds, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                autoLockSeconds = seconds
                                vm.setAutoLockSeconds(seconds)
                                showAutoLockMenu = false
                            }
                        )
                    }
                }
            }
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Change pattern") },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onChangePattern)
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Change security questions") },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onChangeSecurityQuestions)
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Export encrypted backup") },
                supportingContent = { Text("Save all your passwords to a password-protected file") },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onExportBackup)
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Import backup") },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onImportBackup)
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Login activity") },
                supportingContent = { Text("See every unlock attempt, including wrong patterns") },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenLoginActivity)
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Erase all data", color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.fillMaxWidth().clickable { showEraseConfirm = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "© ${Year.now().value} AlgoBuilds. All rights reserved.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        }
    }
}
