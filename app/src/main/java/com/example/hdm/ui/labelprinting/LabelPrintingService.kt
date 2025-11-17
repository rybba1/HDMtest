
package com.example.hdm.ui.labelprinting

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class LabelPrintingService @Inject constructor(
    private val okHttpClient: OkHttpClient // <-- ZMIANA: Przyjmujemy klienta, zamiast go tworzyć
) {

    companion object {
        private const val TAG = "LabelPrintingService"
        private const val SERVER_URL = "http://hdlm.meikotrans.com.pl:6001/api/hdm/print_documents"
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendPrintRequest(requestData: PrintRequest): Result<PrintResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = json.encodeToString(requestData)
                Log.d(TAG, "Wysyłanie żądania do $SERVER_URL z ciałem: $jsonBody")

                val request = Request.Builder()
                    .url(SERVER_URL)
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                // Używamy klienta otrzymanego w konstruktorze
                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Odpowiedź serwera: Kod=${response.code}, Ciało=$responseBody")

                    if (response.isSuccessful && responseBody.isNotEmpty()) {
                        val printResponse = json.decodeFromString<PrintResponse>(responseBody)
                        Result.success(printResponse)
                    } else {
                        try {
                            val errorResponse = json.decodeFromString<PrintResponse>(responseBody)
                            Result.failure(Exception(errorResponse.message))
                        } catch (e: Exception) {
                            Result.failure(Exception("Błąd serwera: ${response.code}. Treść: $responseBody"))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd sieciowy podczas wysyłania żądania drukowania", e)
                Result.failure(Exception("Błąd sieci: ${e.message}"))
            }
        }
    }
}