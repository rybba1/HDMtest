package com.example.hdm.network

import android.content.Context
import android.util.Log
import com.example.hdm.model.DnsLogRequest
import com.example.hdm.services.LogXmlGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis
import com.example.hdm.ui.log_monitor.PingResult

@Singleton
class NetworkService @Inject constructor(private val okHttpClient: OkHttpClient) {

    companion object {
        private const val TAG = "NetworkService"
        private const val IP_SERVER_URL = "http://10.1.0.19:8765"
        private const val DNS_UPLOAD_URL = "http://hdlm.meikotrans.com.pl:6001/upload"
        private const val DNS_LOG_URL = "http://hdlm.meikotrans.com.pl:6001/api/hdm/log"
        private const val PING_TEST_URL = "http://hdlm.meikotrans.com.pl:6001/api/hdm/ping_test"
    }

    @Serializable
    private data class MinimalPingResponse(
        @SerialName("processing_time_ms")
        val processingTimeMs: Double? = null
    )

    private val json = Json { ignoreUnknownKeys = true }

    private fun getDeviceIpAddress(): String {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "Brak IP"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Nie udało się pobrać adresu IP urządzenia", e)
        }
        return "Brak IP"
    }

    suspend fun uploadDnsLog(logRequest: DnsLogRequest): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val finalRequest = logRequest.copy(deviceIp = getDeviceIpAddress())
                val jsonBody = json.encodeToString(finalRequest)

                val request = Request.Builder()
                    .url(DNS_LOG_URL)
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Błąd serwera DNS Log: ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ===== POPRAWIONA FUNKCJA pingServer =====
    suspend fun pingServer(context: Context): Result<PingResult> {
        return withContext(Dispatchers.IO) {
            // ✅ POPRAWKA: Używamy oryginalnego okHttpClient z connection pooling
            // zamiast tworzyć nowego klienta za każdym razem
            //
            // PRZED (ZŁE):
            // val pingClient = okHttpClient.newBuilder()
            //     .connectTimeout(5, TimeUnit.SECONDS)
            //     .readTimeout(5, TimeUnit.SECONDS)
            //     .writeTimeout(5, TimeUnit.SECONDS)
            //     .build()  ← To tworzyło NOWĄ pulę połączeń za każdym razem!
            //
            // PO (DOBRE):
            // Używamy okHttpClient z AppModule, który ma skonfigurowany
            // connection pool i będzie reużywał połączenia TCP

            val request = Request.Builder()
                .url(PING_TEST_URL)
                .get()
                .header("User-Agent", "HDM-Android-App-Pinger")
                // ✅ OPCJONALNIE: Możesz dodać header dla pewności
                .header("Connection", "keep-alive")
                .build()

            try {
                val result: PingResult
                val roundTripTimeMs = measureTimeMillis {
                    // ✅ Używamy oryginalnego klienta z connection pool
                    okHttpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""
                        result = PingResult(
                            rawResponse = responseBody,
                            roundTripTimeMs = 0, // Placeholder, nadpisany poniżej
                            httpCode = response.code
                        )
                    }
                }
                // Zwróć sukces z poprawnym czasem
                Result.success(result.copy(roundTripTimeMs = roundTripTimeMs))

            } catch (e: SocketTimeoutException) {
                // Złapano timeout
                Log.w(TAG, "pingServer Timeout: ${e.message}")
                Result.failure(e)
            } catch (e: Exception) {
                // Inne błędy sieciowe (np. brak hosta)
                Log.w(TAG, "pingServer Inny błąd sieciowy: ${e.message}")
                Result.failure(e)
            }
        }
    }
    // =========================================

    suspend fun uploadXmlReport(
        xmlData: String,
        readTimeout: Long? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val client = if (readTimeout != null) {
                    okHttpClient.newBuilder()
                        .readTimeout(readTimeout, TimeUnit.SECONDS)
                        .build()
                } else {
                    okHttpClient
                }

                val requestBody = xmlData.toRequestBody("application/xml; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(IP_SERVER_URL)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success("Raport wysłany pomyślnie na serwer IP")
                    } else {
                        val errorBody = response.body?.string() ?: "Brak treści błędu"
                        val errorMessage = "Błąd serwera IP: ${response.code} ${response.message}"
                        Log.e(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                        Log.e(TAG, "BŁĄD WYSYŁKI RAPORTU (szczegóły poniżej)")
                        Log.e(TAG, "URL: ${request.url}")
                        Log.e(TAG, "Kod odpowiedzi: ${response.code}")
                        Log.e(TAG, "Wiadomość: ${response.message}")
                        Log.e(TAG, "Treść odpowiedzi serwera: $errorBody")
                        Log.e(TAG, "Fragment wysyłanego XML: ${xmlData.take(500)}...")
                        Log.e(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                        Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun uploadXmlToDnsServer(xmlData: String, fileName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, xmlData.toRequestBody("application/xml; charset=utf-8".toMediaType()))
                    .build()

                val request = Request.Builder()
                    .url(DNS_UPLOAD_URL)
                    .post(requestBody)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.i(TAG, "Pomyślnie wysłano raport na serwer DNS.")
                        Result.success("Raport wysłany pomyślnie na serwer DNS")
                    } else {
                        val errorMsg = "Błąd serwera DNS: ${response.code} ${response.message}"
                        Log.e(TAG, errorMsg)
                        Result.failure(Exception(errorMsg))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Krytyczny błąd podczas wysyłania na serwer DNS", e)
                Result.failure(e)
            }
        }
    }
}