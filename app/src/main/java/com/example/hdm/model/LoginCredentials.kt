package com.example.hdm.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class LoginCredentials(
    val id: String,
    val login: String,
    val name: String,
    val surname: String,
    @SerialName("password")
    val passwordHash: String,
    val lastSync: Long = 0L
) {
    /**
     * Pełne imię i nazwisko
     */
    val fullName: String
        get() = "$name $surname"

    /**
     * Wyciąga numeryczne ID z dowolnego formatu (np. "Id. 36" → 69)
     * Jeśli w tekście nie ma cyfr, zwraca 0.
     */
    val workerId: Int
        // ===== TA LINIA ROZWIĄZUJE PROBLEM ZE SPACJAMI I WIELKOŚCIĄ LITER =====
        get() = id.filter { it.isDigit() }.toIntOrNull() ?: 0
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class LoginCredentialsCache(
    val credentials: List<LoginCredentials>,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)