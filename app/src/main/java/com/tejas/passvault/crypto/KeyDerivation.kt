package com.tejas.passvault.crypto

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Derives a symmetric key from low-entropy secrets (a drawn pattern, a set of security
 * answers) using PBKDF2. The derived key is only ever used to wrap/unwrap the vault's
 * real Data Encryption Key (DEK) - it never touches vault data directly. That way the
 * DEK stays constant even when the user later changes their pattern or answers.
 */
object KeyDerivation {

    private const val ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    const val SALT_SIZE_BYTES = 16

    fun deriveKey(secret: CharArray, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secret, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun newSalt(): ByteArray = CryptoManager.randomBytes(SALT_SIZE_BYTES)
}
