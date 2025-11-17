// Ścieżka: app/src/main/java/com/example/hdm/services/LoginRepository.kt

package com.example.hdm.services

import android.content.Context
import android.util.Log
import com.example.hdm.model.LoginCredentials
import com.example.hdm.model.LoginCredentialsCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object LoginRepository {
    private const val TAG = "LoginRepository"
    private const val CREDENTIALS_FILE = "login_credentials.json"
    private const val SYNC_INTERVAL = 24 * 60 * 60 * 1000L // 24 godziny

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Pobiera wszystkie zapisane dane logowania z cache
     */
    suspend fun getAllCredentials(context: Context): List<LoginCredentials> {
        return withContext(Dispatchers.IO) {
            val file = File(context.filesDir, CREDENTIALS_FILE)
            if (!file.exists()) {
                Log.d(TAG, "Brak pliku cache - pusta lista")
                return@withContext emptyList()
            }

            try {
                val jsonString = file.readText()
                val cache = json.decodeFromString<LoginCredentialsCache>(jsonString)
                Log.d(TAG, "Wczytano ${cache.credentials.size} użytkowników z cache")
                cache.credentials
            } catch (e: Exception) {
                Log.e(TAG, "Błąd odczytu cache", e)
                emptyList()
            }
        }
    }

    /**
     * Zapisuje dane logowania do cache
     */
    suspend fun saveCredentials(context: Context, credentials: List<LoginCredentials>) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, CREDENTIALS_FILE)
                val cache = LoginCredentialsCache(
                    credentials = credentials,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
                val jsonString = json.encodeToString(cache)
                file.writeText(jsonString)
                Log.d(TAG, "Zapisano ${credentials.size} użytkowników do cache")
            } catch (e: Exception) {
                Log.e(TAG, "Błąd zapisu cache", e)
            }
        }
    }

    /**
     * Waliduje hasło z QR i zwraca dane użytkownika
     *
     * @param scannedPassword Hasło zeskanowane z karty QR (np. "4m52A8i2")
     * @return LoginCredentials jeśli znaleziono, null jeśli nie
     */
    suspend fun validateLogin(
        context: Context,
        scannedPassword: String
    ): LoginCredentials? {
        val credentials = getAllCredentials(context)
        val found = credentials.find { it.passwordHash == scannedPassword }

        if (found != null) {
            Log.d(TAG, "Znaleziono użytkownika: ${found.fullName} (${found.login})")
        } else {
            Log.w(TAG, "Nie znaleziono użytkownika dla podanego hasła")
        }

        return found
    }

    /**
     * Sprawdza czy trzeba odświeżyć dane (co 24h lub jeśli cache pusty)
     */
    suspend fun needsSync(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            val file = File(context.filesDir, CREDENTIALS_FILE)
            if (!file.exists()) {
                Log.d(TAG, "Cache nie istnieje - wymagana synchronizacja")
                return@withContext true
            }

            try {
                val jsonString = file.readText()
                val cache = json.decodeFromString<LoginCredentialsCache>(jsonString)

                if (cache.credentials.isEmpty()) {
                    Log.d(TAG, "Cache pusty - wymagana synchronizacja")
                    return@withContext true
                }

                val now = System.currentTimeMillis()
                val timeSinceSync = now - cache.lastSyncTimestamp
                val needsSync = timeSinceSync > SYNC_INTERVAL

                Log.d(TAG, "Czas od ostatniej synchronizacji: ${timeSinceSync / 1000 / 60 / 60}h")
                Log.d(TAG, "Wymagana synchronizacja: $needsSync")

                needsSync
            } catch (e: Exception) {
                Log.e(TAG, "Błąd sprawdzania cache - wymuszam synchronizację", e)
                true
            }
        }
    }

    /**
     * Synchronizuje dane z serwerem
     */
    suspend fun syncWithServer(
        context: Context,
        apiService: LoginApiService
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Rozpoczynam synchronizację z serwerem...")

            val result = apiService.syncCredentials()

            result.onSuccess { credentialsList ->
                saveCredentials(context, credentialsList)
                Log.d(TAG, "Synchronizacja zakończona pomyślnie: ${credentialsList.size} użytkowników")
            }.onFailure { exception ->
                Log.e(TAG, "Błąd synchronizacji: ${exception.message}")
            }

            result.map { }
        } catch (e: Exception) {
            Log.e(TAG, "Nieoczekiwany błąd podczas synchronizacji", e)
            Result.failure(e)
        }
    }

    /**
     * Czyści cache (np. do testów)
     */
    suspend fun clearCache(context: Context) {
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, CREDENTIALS_FILE)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Cache wyczyszczony")
            }
        }
    }
}