package com.tejas.passvault.auth

enum class LoginEventType { PATTERN, BIOMETRIC, RECOVERY }

data class LoginEvent(
    val timestamp: Long,
    val type: LoginEventType,
    val success: Boolean
)
