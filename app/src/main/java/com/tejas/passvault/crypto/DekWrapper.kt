package com.tejas.passvault.crypto

/**
 * Wraps/unwraps the vault's Data Encryption Key (DEK) under a key derived from a
 * low-entropy secret (pattern or security answers). Used by both the pattern and
 * security-question recovery paths so the same DEK - and therefore the same
 * encrypted vault - stays reachable from either route.
 */
object DekWrapper {

    fun wrap(dek: ByteArray, secret: CharArray, salt: ByteArray): ByteArray {
        val wrappingKey = KeyDerivation.deriveKey(secret, salt)
        return try {
            CryptoManager.encrypt(dek, wrappingKey)
        } finally {
            wrappingKey.fill(0)
        }
    }

    /** Returns null if [secret] is wrong (authentication tag mismatch) rather than throwing. */
    fun tryUnwrap(wrappedDek: ByteArray, secret: CharArray, salt: ByteArray): ByteArray? {
        val wrappingKey = KeyDerivation.deriveKey(secret, salt)
        return try {
            CryptoManager.decrypt(wrappedDek, wrappingKey)
        } catch (e: Exception) {
            null
        } finally {
            wrappingKey.fill(0)
        }
    }
}
