package com.tejas.passvault.data

import android.content.Context
import android.util.Log
import com.tejas.passvault.crypto.CryptoManager
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds the unlocked vault in memory for the current session and persists it to a single
 * AES-GCM encrypted file (vault.dat) in app-private storage. Nothing here ever touches
 * disk in plaintext. Call [lock] to drop the DEK and credentials from memory (auto-lock,
 * backgrounding, explicit logout).
 *
 * Encryption and file I/O run on a background dispatcher so they never block the UI thread -
 * e.g. tapping Save shouldn't stall the screen while the vault file is being written.
 */
class VaultRepository(context: Context) {

    private val vaultFile: File = File(context.filesDir, "vault.dat")
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var dek: ByteArray? = null

    private val _credentials = MutableStateFlow<List<Credential>>(emptyList())
    val credentials: StateFlow<List<Credential>> = _credentials.asStateFlow()

    val isUnlocked: Boolean
        get() = dek != null

    /** Unlocks immediately; the vault file itself is decrypted on a background thread so
     * login doesn't stall the UI while the vault list screen is transitioning in. */
    fun unlock(newDek: ByteArray) {
        dek = newDek
        ioScope.launch {
            val loaded = try {
                loadFromDisk(newDek)
            } catch (e: Exception) {
                Log.e("VaultRepository", "Failed to read vault.dat, starting empty", e)
                emptyList()
            }
            _credentials.value = loaded
        }
    }

    fun lock() {
        dek?.fill(0)
        dek = null
        _credentials.value = emptyList()
    }

    fun currentDek(): ByteArray? = dek

    fun upsert(credential: Credential) {
        val current = _credentials.value.toMutableList()
        val index = current.indexOfFirst { it.id == credential.id }
        if (index >= 0) current[index] = credential else current.add(credential)
        _credentials.value = current
        persistAsync()
    }

    fun delete(id: String) {
        _credentials.value = _credentials.value.filterNot { it.id == id }
        persistAsync()
    }

    fun replaceAll(newCredentials: List<Credential>) {
        _credentials.value = newCredentials
        persistAsync()
    }

    fun deleteVaultFile() {
        if (vaultFile.exists()) vaultFile.delete()
    }

    private fun loadFromDisk(key: ByteArray): List<Credential> {
        if (!vaultFile.exists() || vaultFile.length() == 0L) return emptyList()
        val encrypted = vaultFile.readBytes()
        val plaintext = CryptoManager.decrypt(encrypted, key)
        return Credential.listFromJson(String(plaintext, Charsets.UTF_8))
    }

    private fun persistAsync() {
        val key = dek ?: return
        val snapshot = _credentials.value
        ioScope.launch {
            val json = Credential.listToJson(snapshot)
            val encrypted = CryptoManager.encrypt(json.toByteArray(Charsets.UTF_8), key)
            vaultFile.writeBytes(encrypted)
        }
    }
}
