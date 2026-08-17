package com.tejas.passvault.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.tejas.passvault.VaultViewModel
import com.tejas.passvault.ui.components.PatternLockView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    vm: VaultViewModel,
    onLoginSuccess: () -> Unit,
    onForgotPattern: () -> Unit
) {
    val activity = LocalContext.current as FragmentActivity
    val scope = rememberCoroutineScope()
    val useBiometricFirst = vm.isBiometricEnabled && vm.isBiometricAvailable

    var awaitingBiometric by remember { mutableStateOf(useBiometricFirst) }
    var isVerifyingPattern by remember { mutableStateOf(false) }
    var showPatternError by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf(if (useBiometricFirst) "Verifying..." else "Draw your pattern") }

    fun promptBiometric() {
        vm.biometricAuth.authenticate(
            activity = activity,
            onSuccess = {
                awaitingBiometric = false
                statusMessage = "Now draw your pattern"
            },
            onError = { _ ->
                awaitingBiometric = false
                statusMessage = "Draw your pattern"
            },
            onFailed = {
                // An actual failed scan (not a cancel/error) - worth logging as an attempt.
                // androidx.biometric keeps the same prompt open so the user can just retry.
                vm.recordFailedBiometricAttempt()
            }
        )
    }

    LaunchedEffect(Unit) {
        if (useBiometricFirst) promptBiometric()
    }

    LaunchedEffect(showPatternError) {
        if (showPatternError) {
            delay(500)
            showPatternError = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PassVault", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(statusMessage, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))

        if (awaitingBiometric) {
            IconButton(onClick = { promptBiometric() }) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = "Retry biometric",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(56.dp)
                )
            }
        } else if (isVerifyingPattern) {
            CircularProgressIndicator()
        } else {
            PatternLockView(
                modifier = Modifier.fillMaxWidth(),
                showError = showPatternError
            ) { pattern ->
                isVerifyingPattern = true
                statusMessage = "Verifying..."
                scope.launch {
                    val success = vm.tryLoginWithPattern(pattern)
                    isVerifyingPattern = false
                    if (success) {
                        onLoginSuccess()
                    } else {
                        statusMessage = "Wrong pattern, try again"
                        showPatternError = true
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(onClick = onForgotPattern) {
                Text("Forgot pattern?")
            }
        }
    }
}
