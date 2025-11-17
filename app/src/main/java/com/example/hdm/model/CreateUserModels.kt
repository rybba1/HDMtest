// Ścieżka: com/example/hdm/model/CreateUserModels.kt

package com.example.hdm.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class CreateUserRequest(
    val login: String,
    val password: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class CreateUserResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
) {
    fun getDisplayMessage(): String {
        return when {
            success && message != null -> message
            !success && error != null -> error
            success -> "Użytkownik utworzony pomyślnie"
            else -> "Nieznany błąd"
        }
    }
}