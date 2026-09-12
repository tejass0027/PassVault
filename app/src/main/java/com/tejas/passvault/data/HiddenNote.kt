package com.tejas.passvault.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class HiddenNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("content", content)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): HiddenNote = HiddenNote(
            id = obj.optString("id", UUID.randomUUID().toString()),
            title = obj.optString("title"),
            content = obj.optString("content"),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
        )

        fun listToJson(notes: List<HiddenNote>): String {
            val array = JSONArray()
            notes.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(json: String): List<HiddenNote> {
            if (json.isBlank()) return emptyList()
            val array = JSONArray(json)
            return (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
        }
    }
}
