package com.tejas.passvault.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tejas.passvault.VaultViewModel
import com.tejas.passvault.auth.PatternAuthManager
import com.tejas.passvault.ui.components.PatternLockView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Draws and confirms a brand new pattern for the hidden vault. Kept visually identical to
 * [ChangePatternScreen] so nothing about this screen itself hints at what it unlocks - only
 * the (subtly labeled) Settings entry point and this title give it away.
 */
private enum class Step { VerifyOld, DrawNew, ConfirmNew }

@Composable
fun HiddenVaultSetupScreen(vm: VaultViewModel, onDone: () -> Unit, onCancel: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(if (vm.hasHiddenVault) Step.VerifyOld else Step.DrawNew) }
    var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }

    LaunchedEffect(showError) {
        if (showError) {
            delay(700)
            showError = false
            errorMessage = null
        }
    }

    val title = when (step) {
        Step.VerifyOld -> "Draw your current hidden vault pattern"
        Step.DrawNew -> "Draw a new hidden vault pattern"
        Step.ConfirmNew -> "Confirm your new hidden vault pattern"
    }
    val subtitle = errorMessage ?: when (step) {
        Step.VerifyOld -> "Prove you know it before changing it"
        Step.DrawNew -> "This must be different from your main pattern. Connect at least ${PatternAuthManager.MIN_PATTERN_LENGTH} dots."
        Step.ConfirmNew -> "Draw the same pattern again"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        if (isWorking) {
            CircularProgressIndicator()
        } else {
            PatternLockView(modifier = Modifier.fillMaxWidth(), showError = showError) { pattern ->
                when (step) {
                    Step.VerifyOld -> {
                        isWorking = true
                        scope.launch {
                            val ok = vm.beginHiddenVaultPatternChange(pattern)
                            isWorking = false
                            if (ok) {
                                step = Step.DrawNew
                            } else {
                                errorMessage = "Wrong pattern, try again"
                                showError = true
                            }
                        }
                    }
                    Step.DrawNew -> {
                        if (pattern.size < PatternAuthManager.MIN_PATTERN_LENGTH) {
                            errorMessage = "Connect at least ${PatternAuthManager.MIN_PATTERN_LENGTH} dots"
                            showError = true
                            return@PatternLockView
                        }
                        firstPattern = pattern
                        step = Step.ConfirmNew
                    }
                    Step.ConfirmNew -> {
                        if (pattern == firstPattern) {
                            isWorking = true
                            scope.launch {
                                val ok = vm.setupHiddenVault(pattern)
                                isWorking = false
                                if (ok) {
                                    onDone()
                                } else {
                                    errorMessage = "That's your main pattern - choose a different one"
                                    showError = true
                                    firstPattern = null
                                    step = Step.DrawNew
                                }
                            }
                        } else {
                            errorMessage = "Patterns didn't match, try again"
                            showError = true
                            firstPattern = null
                            step = Step.DrawNew
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}
