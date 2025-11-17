
package com.example.hdm.services

import android.util.Log
import com.example.hdm.model.PalletInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PalletApiService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "PalletApiService"
        private const val BASE_URL = "http://hdlm.meikotrans.com.pl:6001/api"
    }

    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun getPalletInfo(barcode: String): Result<PalletInfoResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/pallet/$barcode"
                Log.d(TAG, "Wysyłanie zapytania GET na adres: $url")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    Log.d(TAG, "Kod odpowiedzi: ${response.code}")
                    if (responseBody != null) {
                        Log.d(TAG, "Surowa odpowiedź JSON z serwera: ${responseBody.take(500)}...")
                    }

                    if (response.isSuccessful && responseBody != null) {
                        Log.i(TAG, "Otrzymano pomyślną odpowiedź dla palety '$barcode'.")
                        val palletInfo = jsonParser.decodeFromString<PalletInfoResponse>(responseBody)
                        Result.success(palletInfo)
                    } else {
                        val errorMsg = "Błąd serwera: ${response.code}. Treść: $responseBody"
                        Log.e(TAG, errorMsg)
                        Result.failure(Exception(errorMsg))
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Błąd sieciowy podczas pobierania danych palety: ${e.message}"
                Log.e(TAG, "Wystąpił wyjątek sieciowy.", e)
                Result.failure(Exception(errorMsg))
            }
        }
    }
}