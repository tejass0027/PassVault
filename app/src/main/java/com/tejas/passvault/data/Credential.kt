package com.tejas.passvault.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Credential(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val username: String,
    val password: String,
    val url: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("username", username)
        put("password", password)
        put("url", url)
        put("notes", notes)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): Credential = Credential(
            id = obj.optString("id", UUID.randomUUID().toString()),
            title = obj.optString("title"),
            username = obj.optString("username"),
            password = obj.optString("password"),
            url = obj.optString("url"),
            notes = obj.optString("notes"),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
        )

        fun listToJson(credentials: List<Credential>): String {
            val array = JSONArray()
            credentials.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(json: String): List<Credential> {
            if (json.isBlank()) return emptyList()
            val array = JSONArray(json)
            return (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
        }
    }
}
