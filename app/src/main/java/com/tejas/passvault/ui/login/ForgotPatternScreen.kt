package com.tejas.passvault.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tejas.passvault.VaultViewModel
import com.tejas.passvault.auth.PatternAuthManager
import com.tejas.passvault.ui.components.PatternLockView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class RecoveryStep { ANSWER_QUESTIONS, DRAW_NEW_PATTERN, CONFIRM_NEW_PATTERN }

@Composable
fun ForgotPatternScreen(vm: VaultViewModel, onRecovered: () -> Unit, onCancel: () -> Unit) {
    val scope = rememberCoroutineScope()
    val questions = remember { vm.securityQuestions.questions() }
    var step by remember { mutableStateOf(RecoveryStep.ANSWER_QUESTIONS) }
    var answers by remember { mutableStateOf(listOf("", "", "")) }
    var error by remember { mutableStateOf<String?>(null) }
    var firstNewPattern by remember { mutableStateOf<List<Int>?>(null) }
    var showError by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }

    LaunchedEffect(showError) {
        if (showError) {
            delay(500)
            showError = false
        }
    }

    if (questions.size < 3) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "No security questions were set up, so this device can't be recovered this way.",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCancel) { Text("Back to login") }
        }
        return
    }

    when (step) {
        RecoveryStep.ANSWER_QUESTIONS -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text("Answer your security questions", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(20.dp))
            questions.forEachIndexed { index, question ->
                Text(question, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = answers[index],
                    onValueChange = { newValue ->
                        answers = answers.toMutableList().also { it[index] = newValue }
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isWorking) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (answers.any { it.isBlank() }) {
                            error = "Please answer all questions"
                        } else {
                            isWorking = true
                            scope.launch {
                                val correct = vm.verifySecurityAnswers(answers)
                                isWorking = false
                                if (!correct) {
                                    error = "Those answers didn't match our records"
                                } else {
                                    step = RecoveryStep.DRAW_NEW_PATTERN
                                    error = null
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Continue") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }

        RecoveryStep.DRAW_NEW_PATTERN, RecoveryStep.CONFIRM_NEW_PATTERN -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val isConfirm = step == RecoveryStep.CONFIRM_NEW_PATTERN
            Text(
                if (isConfirm) "Confirm your new pattern" else "Draw a new pattern",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (isWorking) {
                CircularProgressIndicator()
            } else {
                PatternLockView(modifier = Modifier.fillMaxWidth(), showError = showError) { pattern ->
                    if (pattern.size < PatternAuthManager.MIN_PATTERN_LENGTH) {
                        showError = true
                        return@PatternLockView
                    }
                    if (!isConfirm) {
                        firstNewPattern = pattern
                        step = RecoveryStep.CONFIRM_NEW_PATTERN
                    } else if (pattern == firstNewPattern) {
                        isWorking = true
                        scope.launch {
                            val success = vm.recoverWithSecurityAnswers(answers, pattern)
                            isWorking = false
                            if (success) {
                                onRecovered()
                            } else {
                                // Answers somehow stopped matching between steps; start over.
                                error = "Something went wrong, please try again"
                                step = RecoveryStep.ANSWER_QUESTIONS
                                firstNewPattern = null
                            }
                        }
                    } else {
                        showError = true
                        firstNewPattern = null
                        step = RecoveryStep.DRAW_NEW_PATTERN
                    }
                }
            }
        }
    }
}
