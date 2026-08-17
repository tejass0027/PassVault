package com.tejas.passvault.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Thin wrapper around androidx.biometric. This is a device-level "something you are" gate
 * shown before the pattern step - whatever the phone has enrolled (fingerprint and/or face)
 * satisfies it. It intentionally does NOT derive key material from biometrics (Android can't
 * do that reliably); the actual vault key comes from the pattern/security-answer KDF instead.
 */
class BiometricAuthManager(private val context: Context) {

    private val allowedAuthenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK

    fun isAvailable(): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(allowedAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock PassVault",
        subtitle: String = "Use your fingerprint or face to continue",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onFailed()
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(allowedAuthenticators)
            .setNegativeButtonText("Use pattern instead")
            .build()

        prompt.authenticate(promptInfo)
    }
}
