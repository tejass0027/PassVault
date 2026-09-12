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
 * Holds the hidden notes vault, encrypted with its own DEK that is completely separate from
 * the main password vault's - wrapped only by the hidden vault's own pattern, never the main
 * one. Mirrors [VaultRepository] in every other respect.
 */
class HiddenNotesRepository(context: Context) {

    private val notesFile: File = File(context.filesDir, "hidden_notes.dat")
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var dek: ByteArray? = null

    private val _notes = MutableStateFlow<List<HiddenNote>>(emptyList())
    val notes: StateFlow<List<HiddenNote>> = _notes.asStateFlow()

    val isUnlocked: Boolean
        get() = dek != null

    fun unlock(newDek: ByteArray) {
        dek = newDek
        ioScope.launch {
            val loaded = try {
                loadFromDisk(newDek)
            } catch (e: Exception) {
                Log.e("HiddenNotesRepository", "Failed to read hidden_notes.dat, starting empty", e)
                emptyList()
            }
            _notes.value = loaded
        }
    }

    fun lock() {
        dek?.fill(0)
        dek = null
        _notes.value = emptyList()
    }

    fun upsert(note: HiddenNote) {
        val current = _notes.value.toMutableList()
        val index = current.indexOfFirst { it.id == note.id }
        if (index >= 0) current[index] = note else current.add(note)
        _notes.value = current
        persistAsync()
    }

    fun delete(id: String) {
        _notes.value = _notes.value.filterNot { it.id == id }
        persistAsync()
    }

    fun deleteVaultFile() {
        if (notesFile.exists()) notesFile.delete()
    }

    private fun loadFromDisk(key: ByteArray): List<HiddenNote> {
        if (!notesFile.exists() || notesFile.length() == 0L) return emptyList()
        val encrypted = notesFile.readBytes()
        val plaintext = CryptoManager.decrypt(encrypted, key)
        return HiddenNote.listFromJson(String(plaintext, Charsets.UTF_8))
    }

    private fun persistAsync() {
        val key = dek ?: return
        val snapshot = _notes.value
        ioScope.launch {
            val json = HiddenNote.listToJson(snapshot)
            val encrypted = CryptoManager.encrypt(json.toByteArray(Charsets.UTF_8), key)
            notesFile.writeBytes(encrypted)
        }
    }
}
