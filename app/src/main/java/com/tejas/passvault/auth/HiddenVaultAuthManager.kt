package com.tejas.passvault.auth

import com.tejas.passvault.crypto.DekWrapper
import com.tejas.passvault.crypto.KeyDerivation

/**
 * Handles the hidden vault's own pattern - a second pattern, separate from the main login
 * pattern, that wraps a separate DEK for the hidden notes store. Mirrors [PatternAuthManager]
 * exactly, just pointed at a different slot in [AuthPrefs] so the two are fully independent:
 * neither pattern can unlock the other vault.
 */
class HiddenVaultAuthManager(private val authPrefs: AuthPrefs) {

    fun hasHiddenVault(): Boolean =
        authPrefs.hiddenPatternSalt() != null && authPrefs.hiddenPatternWrappedDek() != null

    /** Wraps [dek] under a freshly derived key from [pattern] and persists it. */
    fun setPattern(pattern: List<Int>, dek: ByteArray) {
        require(pattern.size >= PatternAuthManager.MIN_PATTERN_LENGTH) {
            "Pattern must connect at least ${PatternAuthManager.MIN_PATTERN_LENGTH} dots"
        }
        val salt = KeyDerivation.newSalt()
        val wrapped = DekWrapper.wrap(dek, pattern.toCharArray(), salt)
        authPrefs.saveHiddenPatternWrap(salt, wrapped)
    }

    /** Returns the DEK if [pattern] matches the hidden vault's pattern, or null if it doesn't. */
    fun tryUnlock(pattern: List<Int>): ByteArray? {
        val salt = authPrefs.hiddenPatternSalt() ?: return null
        val wrapped = authPrefs.hiddenPatternWrappedDek() ?: return null
        return DekWrapper.tryUnwrap(wrapped, pattern.toCharArray(), salt)
    }

    private fun List<Int>.toCharArray(): CharArray =
        joinToString(separator = ",") { it.toString() }.toCharArray()
}
