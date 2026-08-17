package com.tejas.passvault.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private fun strengthScore(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score
}

@Composable
fun PasswordStrengthMeter(password: String, modifier: Modifier = Modifier) {
    val score = strengthScore(password)
    val (label, color) = when {
        password.isEmpty() -> "" to Color.Gray
        score <= 2 -> "Weak" to Color(0xFFE53935)
        score <= 4 -> "Okay" to Color(0xFFFB8C00)
        else -> "Strong" to Color(0xFF43A047)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { (score / 6f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color
        )
        if (label.isNotEmpty()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}
