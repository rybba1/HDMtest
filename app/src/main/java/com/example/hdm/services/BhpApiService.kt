// Ścieżka: com/example/hdm/services/BhpApiService.kt
package com.example.hdm.services

import android.util.Log
import com.example.hdm.model.BhpStatsUser
import com.example.hdm.model.SubmitPointsRequest
import com.example.hdm.model.SubmitPointsResponse // Nowy import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class BhpApiService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "BhpApiService"
        private const val BASE_URL = "http://hdlm.meikotrans.com.pl:6001/api/bhp" // Główny base URL dla BHP
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    /**
     * Wysyła zebrane punkty na serwer po zalogowaniu. (Endpoint 1)
     * Zwraca teraz SubmitPointsResponse.
     */
    suspend fun submitPoints(requestData: SubmitPointsRequest): Result<SubmitPointsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = json.encodeToString(requestData)
                Log.d(TAG, "Wysyłanie punktów BHP do $BASE_URL/submit: $jsonBody")

                val request = Request.Builder()
                    .url("$BASE_URL/submit") // Pełna ścieżka
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Odpowiedź submit: Kod=${response.code}, Ciało=$responseBody")

                    if (response.isSuccessful && responseBody.isNotEmpty()) {
                        val submitResponse = json.decodeFromString<SubmitPointsResponse>(responseBody)
                        if (submitResponse.success) {
                            Result.success(submitResponse)
                        } else {
                            Result.failure(Exception(submitResponse.message)) // Błąd logiki serwera
                        }
                    } else {
                        Result.failure(Exception("Błąd wysyłania punktów (HTTP ${response.code})"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd sieciowy podczas wysyłania punktów", e)
                Result.failure(Exception("Błąd sieci submitPoints: ${e.message}"))
            }
        }
    }

    /**
     * Pobiera pełną listę statystyk wszystkich użytkowników. (Endpoint 2)
     * Używa teraz ścieżki /ranking.
     */
    suspend fun getAllStats(): Result<List<BhpStatsUser>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/ranking" // Użycie nowej ścieżki
                Log.d(TAG, "Pobieranie wszystkich statystyk BHP z $url")

                val request = Request.Builder().url(url).get().build() // Proste GET bez body

                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Odpowiedź getAllStats/ranking: Kod=${response.code}, Długość=${responseBody.length}")

                    if (response.isSuccessful && responseBody.isNotEmpty()) {
                        if (responseBody.trim() == "[]") {
                            Result.success(emptyList())
                        } else {
                            val allStats = json.decodeFromString<List<BhpStatsUser>>(responseBody)
                            Result.success(allStats)
                        }
                    } else {
                        Result.failure(Exception("Błąd pobierania rankingu (HTTP ${response.code})"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd sieciowy podczas pobierania rankingu", e)
                Result.failure(Exception("Błąd sieci getAllStats/ranking: ${e.message}"))
            }
        }
    }
}