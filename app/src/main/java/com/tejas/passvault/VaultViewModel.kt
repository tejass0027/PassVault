package com.tejas.passvault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tejas.passvault.auth.AuthPrefs
import com.tejas.passvault.auth.BiometricAuthManager
import com.tejas.passvault.auth.LoginEvent
import com.tejas.passvault.auth.LoginEventType
import com.tejas.passvault.auth.PatternAuthManager
import com.tejas.passvault.auth.SecurityQuestionManager
import com.tejas.passvault.crypto.CryptoManager
import com.tejas.passvault.data.Credential
import com.tejas.passvault.data.VaultRepository
import com.tejas.passvault.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Single shared holder for auth/vault state across the whole app - avoids re-creating
 * EncryptedSharedPreferences/managers per screen and keeps the in-memory DEK in one place.
 */
class VaultViewModel(application: Application) : AndroidViewModel(application) {

    val authPrefs = AuthPrefs(application)
    val patternAuth = PatternAuthManager(authPrefs)
    val securityQuestions = SecurityQuestionManager(authPrefs)
    val biometricAuth = BiometricAuthManager(application)
    val vaultRepository = VaultRepository(application)

    val credentials: StateFlow<List<Credential>> = vaultRepository.credentials

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // How many failed attempts happened before the login that just succeeded - lets the
    // vault list show "N incorrect attempts since your last login" once, right after login.
    private val _failedAttemptsSinceLastLogin = MutableStateFlow(0)
    val failedAttemptsSinceLastLogin: StateFlow<Int> = _failedAttemptsSinceLastLogin.asStateFlow()

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(authPrefs.themeMode) }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        authPrefs.themeMode = mode.name
        _themeMode.value = mode
    }

    // Only held transiently in memory while the user is stepping through onboarding.
    // Wiped as soon as onboarding finishes (or the process dies, since it's never persisted).
    private var onboardingDek: ByteArray? = null

    val isOnboarded: Boolean
        get() = authPrefs.onboardingComplete

    val isBiometricAvailable: Boolean
        get() = biometricAuth.isAvailable()

    val isBiometricEnabled: Boolean
        get() = authPrefs.biometricEnabled

    // --- Onboarding ---
    // Pattern/security-question setup and verification all run PBKDF2 (210k iterations,
    // intentionally slow to resist brute-forcing) - these are pushed onto Dispatchers.Default
    // so that work never blocks the UI thread. Screens should call these from a coroutine
    // scope and show a brief loading state while awaiting the result.

    suspend fun beginOnboardingWithPattern(pattern: List<Int>) {
        val dek = CryptoManager.generateRandomKey()
        onboardingDek = dek
        withContext(Dispatchers.Default) {
            patternAuth.setPattern(pattern, dek)
        }
    }

    suspend fun finishOnboardingSecurityQuestions(questions: List<String>, answers: List<String>) {
        val dek = onboardingDek ?: error("Pattern must be set before security questions")
        withContext(Dispatchers.Default) {
            securityQuestions.setQuestions(questions, answers, dek)
        }
    }

    fun finishOnboarding(enableBiometric: Boolean) {
        authPrefs.biometricEnabled = enableBiometric && isBiometricAvailable
        authPrefs.onboardingComplete = true
        val dek = onboardingDek
        if (dek != null) {
            vaultRepository.unlock(dek)
            _isUnlocked.value = true
        }
        onboardingDek = null
    }

    // --- Login ---

    /** Returns true if [pattern] was correct and the vault is now unlocked. */
    suspend fun tryLoginWithPattern(pattern: List<Int>): Boolean {
        val dek = withContext(Dispatchers.Default) { patternAuth.tryUnlock(pattern) }
        if (dek == null) {
            authPrefs.recordLoginEvent(LoginEventType.PATTERN, success = false)
            return false
        }
        _failedAttemptsSinceLastLogin.value = failedAttemptsSinceLastRecordedSuccess()
        authPrefs.recordLoginEvent(LoginEventType.PATTERN, success = true)
        vaultRepository.unlock(dek)
        _isUnlocked.value = true
        return true
    }

    suspend fun verifySecurityAnswers(answers: List<String>): Boolean =
        withContext(Dispatchers.Default) { securityQuestions.tryUnlock(answers) != null }

    /** Recovery path: verifies security answers and, if correct, sets a brand new pattern. */
    suspend fun recoverWithSecurityAnswers(answers: List<String>, newPattern: List<Int>): Boolean {
        val dek = withContext(Dispatchers.Default) {
            val unwrapped = securityQuestions.tryUnlock(answers) ?: return@withContext null
            patternAuth.setPattern(newPattern, unwrapped)
            unwrapped
        } ?: return false
        _failedAttemptsSinceLastLogin.value = failedAttemptsSinceLastRecordedSuccess()
        authPrefs.recordLoginEvent(LoginEventType.RECOVERY, success = true)
        vaultRepository.unlock(dek)
        _isUnlocked.value = true
        return true
    }

    /** Call when the system biometric prompt reports an actual failed scan (not a cancel). */
    fun recordFailedBiometricAttempt() {
        authPrefs.recordLoginEvent(LoginEventType.BIOMETRIC, success = false)
    }

    fun dismissFailedAttemptsBanner() {
        _failedAttemptsSinceLastLogin.value = 0
    }

    fun loginEvents(): List<LoginEvent> = authPrefs.loginEvents()

    private fun failedAttemptsSinceLastRecordedSuccess(): Int {
        var count = 0
        for (event in authPrefs.loginEvents()) {
            if (event.success) break
            count++
        }
        return count
    }

    fun lock() {
        vaultRepository.lock()
        _isUnlocked.value = false
    }

    // --- Vault operations ---

    fun saveCredential(credential: Credential) = vaultRepository.upsert(credential)

    fun deleteCredential(id: String) = vaultRepository.delete(id)

    // --- Settings ---

    fun setBiometricEnabled(enabled: Boolean) {
        authPrefs.biometricEnabled = enabled && isBiometricAvailable
    }

    fun setAutoLockSeconds(seconds: Int) {
        authPrefs.autoLockSeconds = seconds
    }

    suspend fun changePattern(newPattern: List<Int>) {
        val dek = vaultRepository.currentDek() ?: error("Vault must be unlocked to change pattern")
        withContext(Dispatchers.Default) {
            patternAuth.setPattern(newPattern, dek)
        }
    }

    suspend fun changeSecurityQuestions(questions: List<String>, answers: List<String>) {
        val dek = vaultRepository.currentDek() ?: error("Vault must be unlocked to change security questions")
        withContext(Dispatchers.Default) {
            securityQuestions.setQuestions(questions, answers, dek)
        }
    }

    fun eraseEverything() {
        lock()
        authPrefs.clearAll()
        vaultRepository.deleteVaultFile()
    }

    fun replaceAllCredentials(newCredentials: List<Credential>) = vaultRepository.replaceAll(newCredentials)
}
