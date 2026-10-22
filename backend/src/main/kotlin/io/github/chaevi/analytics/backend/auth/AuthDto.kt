package io.github.chaevi.analytics.backend.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    val adminUser: AdminUserResponse,
)

@Serializable
data class AdminUserResponse(
    val id: String,
    val email: String,
    val name: String,
)
