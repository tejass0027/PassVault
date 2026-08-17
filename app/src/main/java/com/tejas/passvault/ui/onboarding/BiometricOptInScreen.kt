package com.tejas.passvault.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BiometricOptInScreen(
    biometricAvailable: Boolean,
    onFinish: (enableBiometric: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Fingerprint,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(64.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (biometricAvailable) {
            Text(
                "One last step",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add your fingerprint or face as an extra step before your pattern every " +
                    "time you open PassVault. You can turn this off later in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { onFinish(true) }, modifier = Modifier.fillMaxWidth()) {
                Text("Enable fingerprint / face unlock")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { onFinish(false) }, modifier = Modifier.fillMaxWidth()) {
                Text("Skip, use pattern only")
            }
        } else {
            Text(
                "You're all set",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No fingerprint or face unlock was found on this device, so you'll log in " +
                    "with your pattern. You can enable biometric unlock later in Settings if " +
                    "you add one.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { onFinish(false) }, modifier = Modifier.fillMaxWidth()) {
                Text("Finish setup")
            }
        }
    }
}
