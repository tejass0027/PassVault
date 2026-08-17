package com.tejas.passvault.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tejas.passvault.auth.PatternAuthManager
import com.tejas.passvault.ui.components.PatternLockView
import kotlinx.coroutines.delay

@Composable
fun CreatePatternScreen(onPatternConfirmed: (List<Int>) -> Unit) {
    var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showError) {
        if (showError) {
            delay(500)
            showError = false
            errorMessage = null
        }
    }

    val title = if (firstPattern == null) "Draw a new pattern" else "Confirm your pattern"
    val subtitle = errorMessage ?: if (firstPattern == null) {
        "Connect at least ${PatternAuthManager.MIN_PATTERN_LENGTH} dots. You'll use this every time you log in."
    } else {
        "Draw the same pattern again to confirm."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        PatternLockView(
            modifier = Modifier.fillMaxWidth(),
            showError = showError
        ) { pattern ->
            if (pattern.size < PatternAuthManager.MIN_PATTERN_LENGTH) {
                errorMessage = "Connect at least ${PatternAuthManager.MIN_PATTERN_LENGTH} dots"
                showError = true
                return@PatternLockView
            }
            val existing = firstPattern
            if (existing == null) {
                firstPattern = pattern
            } else if (existing == pattern) {
                onPatternConfirmed(pattern)
            } else {
                errorMessage = "Patterns didn't match, try again"
                showError = true
                firstPattern = null
            }
        }
    }
}
