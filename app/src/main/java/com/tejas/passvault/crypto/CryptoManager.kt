package com.tejas.passvault.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Generic AES-256-GCM helpers. Every encrypted blob is stored as [12-byte IV][ciphertext+tag]
 * so callers never need to juggle IVs separately.
 */
object CryptoManager {

    private const val AES_KEY_SIZE_BYTES = 32 // 256-bit
    private const val GCM_IV_SIZE_BYTES = 12
    private const val GCM_TAG_SIZE_BITS = 128

    fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    /** A fresh random 256-bit key, e.g. for use as the vault's Data Encryption Key. */
    fun generateRandomKey(): ByteArray = randomBytes(AES_KEY_SIZE_BYTES)

    fun encrypt(plaintext: ByteArray, keyBytes: ByteArray): ByteArray {
        val iv = randomBytes(GCM_IV_SIZE_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    fun decrypt(ivAndCiphertext: ByteArray, keyBytes: ByteArray): ByteArray {
        require(ivAndCiphertext.size > GCM_IV_SIZE_BYTES) { "Encrypted blob too short" }
        val iv = ivAndCiphertext.copyOfRange(0, GCM_IV_SIZE_BYTES)
        val ciphertext = ivAndCiphertext.copyOfRange(GCM_IV_SIZE_BYTES, ivAndCiphertext.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
        return cipher.doFinal(ciphertext)
    }
}
