package com.tejas.passvault.auth

import com.tejas.passvault.crypto.DekWrapper
import com.tejas.passvault.crypto.KeyDerivation

/**
 * Handles the pattern-lock side of authentication: wrapping/unwrapping the vault DEK
 * under a key derived from the drawn pattern. The pattern itself is never stored -
 * only a salt and the resulting wrapped DEK, so there is nothing to reverse-engineer
 * the pattern from short of brute-forcing the KDF.
 */
class PatternAuthManager(private val authPrefs: AuthPrefs) {

    companion object {
        const val MIN_PATTERN_LENGTH = 4
    }

    fun hasPattern(): Boolean =
        authPrefs.patternSalt() != null && authPrefs.patternWrappedDek() != null

    /** Wraps [dek] under a freshly derived key from [pattern] and persists it. */
    fun setPattern(pattern: List<Int>, dek: ByteArray) {
        require(pattern.size >= MIN_PATTERN_LENGTH) {
            "Pattern must connect at least $MIN_PATTERN_LENGTH dots"
        }
        val salt = KeyDerivation.newSalt()
        val wrapped = DekWrapper.wrap(dek, pattern.toCharArray(), salt)
        authPrefs.savePatternWrap(salt, wrapped)
    }

    /** Returns the DEK if [pattern] is correct, or null if it doesn't match. */
    fun tryUnlock(pattern: List<Int>): ByteArray? {
        val salt = authPrefs.patternSalt() ?: return null
        val wrapped = authPrefs.patternWrappedDek() ?: return null
        return DekWrapper.tryUnwrap(wrapped, pattern.toCharArray(), salt)
    }

    private fun List<Int>.toCharArray(): CharArray =
        joinToString(separator = ",") { it.toString() }.toCharArray()
}
