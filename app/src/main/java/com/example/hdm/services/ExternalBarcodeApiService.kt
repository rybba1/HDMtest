package com.example.hdm.services

import android.util.Log
import com.example.hdm.model.ExternalBarcodeRequest
import com.example.hdm.model.ExternalBarcodeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ExternalBarcodeApiService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "ExternalBarcodeApi"
        // --- POCZĄTEK POPRAWKI ---
        // Zmiana adresu URL i portu zgodnie z nową specyfikacją.
        private const val SERVER_URL = "http://hdlm.meikotrans.com.pl:6001/api/hdm/external_barcode_proxy"
        // --- KONIEC POPRAWKI ---
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun queryBarcode(barcode: String): Result<ExternalBarcodeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val requestData = ExternalBarcodeRequest(barcodes = listOf(barcode))
                val jsonBody = json.encodeToString(requestData)
                Log.d(TAG, "Wysyłanie zapytania do $SERVER_URL z ciałem: $jsonBody")

                val request = Request.Builder()
                    .url(SERVER_URL)
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Odpowiedź serwera: Kod=${response.code}, Ciało=$responseBody")

                    if (response.isSuccessful && responseBody.isNotEmpty()) {
                        val barcodeResponse = json.decodeFromString<ExternalBarcodeResponse>(responseBody)
                        if (barcodeResponse.success) {
                            Result.success(barcodeResponse)
                        } else {
                            Result.failure(Exception("Zapytanie nie powiodło się (success: false)"))
                        }
                    } else {
                        Result.failure(Exception("Błąd serwera: ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd sieciowy podczas zapytania o kod kreskowy", e)
                Result.failure(Exception("Błąd sieci: ${e.message}"))
            }
        }
    }
}