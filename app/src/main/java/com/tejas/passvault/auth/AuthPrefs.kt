package com.tejas.passvault.auth

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores all non-secret vault metadata (salts, wrapped DEK copies, security question text,
 * settings) in an EncryptedSharedPreferences file, itself protected by a hardware-backed
 * Android Keystore key. Never stores the DEK or any plaintext vault data.
 */
class AuthPrefs(context: Context) {

    private val masterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context.applicationContext,
        "passvault_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var autoLockSeconds: Int
        get() = prefs.getInt(KEY_AUTO_LOCK_SECONDS, 30)
        set(value) = prefs.edit().putInt(KEY_AUTO_LOCK_SECONDS, value).apply()

    /** Stored as a raw string ("SYSTEM"/"LIGHT"/"DARK") so this data layer doesn't need to know about the UI's ThemeMode enum. */
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    fun savePatternWrap(salt: ByteArray, wrappedDek: ByteArray) {
        prefs.edit()
            .putString(KEY_PATTERN_SALT, salt.toBase64())
            .putString(KEY_PATTERN_WRAPPED_DEK, wrappedDek.toBase64())
            .apply()
    }

    fun patternSalt(): ByteArray? = prefs.getString(KEY_PATTERN_SALT, null)?.fromBase64()
    fun patternWrappedDek(): ByteArray? = prefs.getString(KEY_PATTERN_WRAPPED_DEK, null)?.fromBase64()

    fun saveSecurityWrap(salt: ByteArray, wrappedDek: ByteArray, questions: List<String>) {
        val questionsJson = JSONArray(questions).toString()
        prefs.edit()
            .putString(KEY_SECURITY_SALT, salt.toBase64())
            .putString(KEY_SECURITY_WRAPPED_DEK, wrappedDek.toBase64())
            .putString(KEY_SECURITY_QUESTIONS, questionsJson)
            .apply()
    }

    fun securitySalt(): ByteArray? = prefs.getString(KEY_SECURITY_SALT, null)?.fromBase64()
    fun securityWrappedDek(): ByteArray? = prefs.getString(KEY_SECURITY_WRAPPED_DEK, null)?.fromBase64()

    fun securityQuestions(): List<String> {
        val json = prefs.getString(KEY_SECURITY_QUESTIONS, null) ?: return emptyList()
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    /** Newest first. Every pattern/biometric/recovery attempt - successful or not - is recorded here. */
    fun recordLoginEvent(type: LoginEventType, success: Boolean) {
        val events = JSONArray(prefs.getString(KEY_LOGIN_EVENTS, "[]"))
        val updated = JSONArray()
        updated.put(
            JSONObject()
                .put("timestamp", System.currentTimeMillis())
                .put("type", type.name)
                .put("success", success)
        )
        for (i in 0 until minOf(events.length(), MAX_LOGIN_EVENTS - 1)) {
            updated.put(events.getJSONObject(i))
        }
        prefs.edit().putString(KEY_LOGIN_EVENTS, updated.toString()).apply()
    }

    fun loginEvents(): List<LoginEvent> {
        val arr = JSONArray(prefs.getString(KEY_LOGIN_EVENTS, "[]"))
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            LoginEvent(
                timestamp = obj.getLong("timestamp"),
                type = LoginEventType.valueOf(obj.getString("type")),
                success = obj.getBoolean("success")
            )
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    companion object {
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTO_LOCK_SECONDS = "auto_lock_seconds"
        private const val KEY_PATTERN_SALT = "pattern_salt"
        private const val KEY_PATTERN_WRAPPED_DEK = "pattern_wrapped_dek"
        private const val KEY_SECURITY_SALT = "security_salt"
        private const val KEY_SECURITY_WRAPPED_DEK = "security_wrapped_dek"
        private const val KEY_SECURITY_QUESTIONS = "security_questions"
        private const val KEY_LOGIN_EVENTS = "login_events"
        private const val MAX_LOGIN_EVENTS = 50
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
