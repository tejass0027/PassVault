package com.tejas.passvault.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Metadata only - the actual encrypted image bytes live in their own file, named by [id]. */
data class VaultPhoto(
    val id: String = UUID.randomUUID().toString(),
    val caption: String = "",
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("caption", caption)
        put("addedAt", addedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): VaultPhoto = VaultPhoto(
            id = obj.optString("id", UUID.randomUUID().toString()),
            caption = obj.optString("caption"),
            addedAt = obj.optLong("addedAt", System.currentTimeMillis())
        )

        fun listToJson(photos: List<VaultPhoto>): String {
            val array = JSONArray()
            photos.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(json: String): List<VaultPhoto> {
            if (json.isBlank()) return emptyList()
            val array = JSONArray(json)
            return (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
        }
    }
}
