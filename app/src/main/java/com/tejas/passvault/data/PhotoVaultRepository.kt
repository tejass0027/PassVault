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
import kotlinx.coroutines.withContext

/**
 * A separate encrypted photo store, sharing the same DEK as the password vault (no extra
 * pattern/key to manage) but keeping photo bytes out of the credentials file entirely - each
 * photo is its own encrypted file on disk, with a small metadata index listing them. This
 * keeps adding/removing a photo cheap (no re-encrypting every other photo) and keeps the
 * credentials file itself small and fast to load.
 */
class PhotoVaultRepository(context: Context) {

    private val indexFile: File = File(context.filesDir, "photos_index.dat")
    private val photosDir: File = File(context.filesDir, "photos").apply { mkdirs() }
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var dek: ByteArray? = null

    private val _photos = MutableStateFlow<List<VaultPhoto>>(emptyList())
    val photos: StateFlow<List<VaultPhoto>> = _photos.asStateFlow()

    fun unlock(newDek: ByteArray) {
        dek = newDek
        ioScope.launch {
            val loaded = try {
                loadIndex(newDek)
            } catch (e: Exception) {
                Log.e("PhotoVaultRepository", "Failed to read photo index, starting empty", e)
                emptyList()
            }
            _photos.value = loaded
        }
    }

    fun lock() {
        dek = null
        _photos.value = emptyList()
    }

    /** Encrypts and saves [imageBytes] as a new photo. Runs off the calling thread. */
    suspend fun addPhoto(caption: String, imageBytes: ByteArray) {
        val key = dek ?: return
        val photo = VaultPhoto(caption = caption)
        withContext(Dispatchers.IO) {
            val encrypted = CryptoManager.encrypt(imageBytes, key)
            File(photosDir, photo.id).writeBytes(encrypted)
            val updated = _photos.value + photo
            _photos.value = updated
            persistIndex(updated, key)
        }
    }

    suspend fun deletePhoto(id: String) {
        val key = dek ?: return
        withContext(Dispatchers.IO) {
            File(photosDir, id).delete()
            val updated = _photos.value.filterNot { it.id == id }
            _photos.value = updated
            persistIndex(updated, key)
        }
    }

    /** Decrypts and returns the raw image bytes for [id], or null if missing/unreadable. */
    suspend fun loadPhotoBytes(id: String): ByteArray? {
        val key = dek ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val file = File(photosDir, id)
                if (!file.exists()) return@withContext null
                CryptoManager.decrypt(file.readBytes(), key)
            } catch (e: Exception) {
                Log.e("PhotoVaultRepository", "Failed to decrypt photo $id", e)
                null
            }
        }
    }

    fun deleteAll() {
        photosDir.listFiles()?.forEach { it.delete() }
        if (indexFile.exists()) indexFile.delete()
    }

    private fun loadIndex(key: ByteArray): List<VaultPhoto> {
        if (!indexFile.exists() || indexFile.length() == 0L) return emptyList()
        val encrypted = indexFile.readBytes()
        val plaintext = CryptoManager.decrypt(encrypted, key)
        return VaultPhoto.listFromJson(String(plaintext, Charsets.UTF_8))
    }

    private fun persistIndex(photos: List<VaultPhoto>, key: ByteArray) {
        val json = VaultPhoto.listToJson(photos)
        val encrypted = CryptoManager.encrypt(json.toByteArray(Charsets.UTF_8), key)
        indexFile.writeBytes(encrypted)
    }
}
