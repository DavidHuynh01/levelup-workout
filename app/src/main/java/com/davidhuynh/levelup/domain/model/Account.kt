package com.davidhuynh.levelup.domain.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarEmoji: String?,
    val weightUnit: WeightUnit,
    val createdAt: Long,
    val isDemo: Boolean = false,
)

enum class WeightUnit(val label: String, val suffix: String) {
    KG("Kilograms", "kg"),
    LB("Pounds", "lb"),
}

data class Session(
    val userId: String,
    val token: String,
    val createdAt: Long,
    val expiresAt: Long,
) {
    fun isExpiredAt(nowMillis: Long): Boolean = nowMillis >= expiresAt
}

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val userId: String) : AuthState
}
