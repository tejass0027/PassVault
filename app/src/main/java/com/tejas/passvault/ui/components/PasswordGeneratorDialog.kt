package com.tejas.passvault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.security.SecureRandom

private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val DIGITS = "0123456789"
private const val SYMBOLS = "!@#\$%^&*()-_=+[]{}?"

private fun generatePassword(length: Int, useUpper: Boolean, useDigits: Boolean, useSymbols: Boolean): String {
    val pool = buildString {
        append(LOWER)
        if (useUpper) append(UPPER)
        if (useDigits) append(DIGITS)
        if (useSymbols) append(SYMBOLS)
    }
    val random = SecureRandom()
    return (1..length).map { pool[random.nextInt(pool.length)] }.joinToString("")
}

@Composable
fun PasswordGeneratorDialog(onDismiss: () -> Unit, onUsePassword: (String) -> Unit) {
    var length by remember { mutableStateOf(16f) }
    var useUpper by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(length, useUpper, useDigits, useSymbols) {
        password = generatePassword(length.toInt(), useUpper, useDigits, useSymbols)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate password") },
        text = {
            Column {
                Text(password, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Length: ${length.toInt()}")
                Slider(value = length, onValueChange = { length = it }, valueRange = 8f..32f, steps = 23)
                OptionRow("Uppercase letters (A-Z)", useUpper) { useUpper = it }
                OptionRow("Numbers (0-9)", useDigits) { useDigits = it }
                OptionRow("Symbols (!@#\$...)", useSymbols) { useSymbols = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onUsePassword(password) }) { Text("Use this password") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun OptionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.height(48.dp))
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}
