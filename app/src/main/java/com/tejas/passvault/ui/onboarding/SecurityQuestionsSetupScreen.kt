package com.tejas.passvault.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tejas.passvault.auth.SecurityQuestionManager

@Composable
fun SecurityQuestionsSetupScreen(onDone: (questions: List<String>, answers: List<String>) -> Unit) {
    val allQuestions = SecurityQuestionManager.SUGGESTED_QUESTIONS
    var q1 by remember { mutableStateOf(allQuestions[0]) }
    var q2 by remember { mutableStateOf(allQuestions[1]) }
    var q3 by remember { mutableStateOf(allQuestions[2]) }
    var a1 by remember { mutableStateOf("") }
    var a2 by remember { mutableStateOf("") }
    var a3 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Security questions", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "These let you get back in and reset your pattern if you ever forget it or set up " +
                "on a new phone. Answers aren't shown to anyone but you.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        QuestionSlot("Question 1", allQuestions, q1, listOf(q2, q3)) { q1 = it }
        AnswerField(a1) { a1 = it }
        Spacer(modifier = Modifier.height(20.dp))

        QuestionSlot("Question 2", allQuestions, q2, listOf(q1, q3)) { q2 = it }
        AnswerField(a2) { a2 = it }
        Spacer(modifier = Modifier.height(20.dp))

        QuestionSlot("Question 3", allQuestions, q3, listOf(q1, q2)) { q3 = it }
        AnswerField(a3) { a3 = it }

        error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = {
                when {
                    a1.isBlank() || a2.isBlank() || a3.isBlank() ->
                        error = "Please answer all three questions"
                    setOf(q1, q2, q3).size < 3 ->
                        error = "Please choose three different questions"
                    else -> onDone(listOf(q1, q2, q3), listOf(a1, a2, a3))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun QuestionSlot(
    label: String,
    allQuestions: List<String>,
    selected: String,
    excluding: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = allQuestions.filter { it !in excluding }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        // Transparent overlay so a tap opens the menu instead of placing a text cursor.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AnswerField(value: String, onValueChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Your answer") },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = "Toggle answer visibility"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
