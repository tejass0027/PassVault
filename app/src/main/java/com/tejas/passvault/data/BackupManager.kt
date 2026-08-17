package com.tejas.passvault.data

import com.tejas.passvault.crypto.CryptoManager
import com.tejas.passvault.crypto.KeyDerivation
import java.io.InputStream
import java.io.OutputStream

/**
 * Manual, user-initiated encrypted backup of the vault contents. Completely independent
 * of the DEK/pattern/security-question system - a backup is encrypted with its own
 * password so it can be restored even after a full app reinstall (e.g. new phone) where
 * none of the on-device auth state survives.
 */
object BackupManager {

    private val MAGIC = byteArrayOf('P'.code.toByte(), 'V'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    private const val VERSION: Byte = 1

    fun export(output: OutputStream, credentials: List<Credential>, backupPassword: CharArray) {
        val salt = KeyDerivation.newSalt()
        val key = KeyDerivation.deriveKey(backupPassword, salt)
        val json = Credential.listToJson(credentials)
        val encrypted = try {
            CryptoManager.encrypt(json.toByteArray(Charsets.UTF_8), key)
        } finally {
            key.fill(0)
        }

        output.write(MAGIC)
        output.write(byteArrayOf(VERSION))
        output.write(salt)
        output.write(encrypted)
        output.flush()
    }

    /** Returns the decrypted credential list, or null if [backupPassword] is wrong or the file is invalid. */
    fun import(input: InputStream, backupPassword: CharArray): List<Credential>? {
        val bytes = input.readBytes()
        val headerSize = MAGIC.size + 1 + KeyDerivation.SALT_SIZE_BYTES
        if (bytes.size <= headerSize) return null

        val magic = bytes.copyOfRange(0, MAGIC.size)
        if (!magic.contentEquals(MAGIC)) return null

        val salt = bytes.copyOfRange(MAGIC.size + 1, headerSize)
        val encrypted = bytes.copyOfRange(headerSize, bytes.size)

        val key = KeyDerivation.deriveKey(backupPassword, salt)
        return try {
            val plaintext = CryptoManager.decrypt(encrypted, key)
            Credential.listFromJson(String(plaintext, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        } finally {
            key.fill(0)
        }
    }
}
