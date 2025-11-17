package com.example.hdm.services

import android.util.Log
import com.example.hdm.model.CreateUserRequest
import com.example.hdm.model.CreateUserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import java.io.File // <-- NOWY IMPORT
import java.io.FileOutputStream // <-- NOWY IMPORT
class LoginApiService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "LoginApiService"
        private const val BASE_URL = "http://hdlm.meikotrans.com.pl:6001/api/hdm"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * Tworzy nowego użytkownika w systemie
     *
     * @param login Login użytkownika (np. "lmazur")
     * @param password Hasło z karty QR (plain text, np. "4m52A8i2")
     * @return Result z odpowiedzią serwera
     */
    suspend fun createUser(login: String, password: String): Result<CreateUserResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val requestData = CreateUserRequest(login = login, password = password)
                val jsonBody = json.encodeToString(requestData)

                Log.d(TAG, "Wysyłanie żądania do $BASE_URL/create_user")
                Log.d(TAG, "Body: $jsonBody")

                val request = Request.Builder()
                    .url("$BASE_URL/create_user")
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Odpowiedź serwera: Kod=${response.code}, Ciało=$responseBody")

                    if (responseBody.isNotEmpty()) {
                        val createUserResponse = json.decodeFromString<CreateUserResponse>(responseBody)

                        if (createUserResponse.success) {
                            Result.success(createUserResponse)
                        } else {
                            // Błąd biznesowy (np. brak w FeniksWMS)
                            Result.failure(Exception(createUserResponse.getDisplayMessage()))
                        }
                    } else {
                        Result.failure(Exception("Pusta odpowiedź serwera (kod: ${response.code})"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd sieciowy podczas tworzenia użytkownika", e)
                Result.failure(Exception("Błąd sieci: ${e.message}"))
            }
        }
    }

    /**
     * Pobiera listę wszystkich użytkowników i ich haseł
     *
     * @return Result z listą użytkowników
     */
    suspend fun syncCredentials(): Result<List<com.example.hdm.model.LoginCredentials>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Pobieranie danych użytkowników z $BASE_URL/feniks_users")

                val request = Request.Builder()
                    .url("$BASE_URL/feniks_users")
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Odpowiedź sync: Kod=${response.code}, Długość=${responseBody.length}")

                    // ===== DODAJ TĘ LINIĘ DO LOGOWANIA =====
                    Log.d("SERVER_RESPONSE", "Surowa odpowiedź serwera: $responseBody")
                    // =======================================

                    if (response.isSuccessful && responseBody.isNotEmpty()) {
                        val credentials = json.decodeFromString<List<com.example.hdm.model.LoginCredentials>>(responseBody)
                        Log.d(TAG, "Pobrano ${credentials.size} użytkowników z Feniks")
                        Result.success(credentials)
                    } else {
                        Result.failure(Exception("Błąd synchronizacji: ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd sieciowy podczas synchronizacji", e)
                Result.failure(Exception("Błąd sieci: ${e.message}"))
            }
        }
    }
    /**
     * Sprawdza dostępność aktualizacji
     *
     * @param request Dane o urządzeniu i wersji
     * @return Result z odpowiedzią serwera
     */
    suspend fun checkUpdate(request: UpdateCheckRequest): Result<UpdateCheckResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = json.encodeToString(request)
                Log.d(TAG, "Wysyłanie żądania do $BASE_URL/check_update")

                val httpRequest = Request.Builder()
                    .url("$BASE_URL/check_update")
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                okHttpClient.newCall(httpRequest).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Odpowiedź check_update: Kod=${response.code}, Ciało=$responseBody")

                    if (response.isSuccessful && responseBody.isNotEmpty()) {
                        val updateResponse = json.decodeFromString<UpdateCheckResponse>(responseBody)
                        Result.success(updateResponse)
                    } else {
                        Result.failure(Exception("Błąd sprawdzania aktualizacji: ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd sieciowy podczas sprawdzania aktualizacji", e)
                Result.failure(Exception("Błąd sieci: ${e.message}"))
            }
        }
    }

    /**
     * Pobiera listę zmian (changelog)
     *
     * @return Result z listą zmian
     */
    suspend fun getChangelog(): Result<ChangelogResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Pobieranie changeloga z $BASE_URL/changelog")

                val request = Request.Builder()
                    .url("$BASE_URL/changelog")
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Odpowiedź changelog: Kod=${response.code}")

                    if (response.isSuccessful && responseBody.isNotEmpty()) {
                        val changelogResponse = json.decodeFromString<ChangelogResponse>(responseBody)
                        Result.success(changelogResponse)
                    } else {
                        Result.failure(Exception("Błąd pobierania changeloga: ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd sieciowy podczas pobierania changeloga", e)
                Result.failure(Exception("Błąd sieci: ${e.message}"))
            }
        }
    }

    /**
     * Pobiera plik APK i zapisuje go na dysku, raportując postęp.
     *
     * @param filename Nazwa pliku APK do pobrania
     * @param destinationFile Plik docelowy
     * @param onProgress Lambda raportująca postęp (bajty pobrane, rozmiar całkowity)
     * @return Result
     */
    suspend fun downloadUpdateApk(
        filename: String,
        destinationFile: File,
        onProgress: suspend (Long, Long) -> Unit // Zmieniono na suspend lambda
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/download_update?filename=$filename"
                Log.d(TAG, "Pobieranie pliku APK z $url")

                val request = Request.Builder().url(url).get().build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Błąd serwera: ${response.code}"))
                    }

                    val body = response.body ?: return@withContext Result.failure(Exception("Pusta odpowiedź serwera"))
                    val totalBytes = body.contentLength()

                    body.byteStream().use { input ->
                        FileOutputStream(destinationFile).use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var bytesRead: Int
                            var totalBytesRead = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                onProgress(totalBytesRead, totalBytes)
                            }
                        }
                    }
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd sieciowy podczas pobierania APK", e)
                Result.failure(Exception("Błąd pobierania pliku: ${e.message}"))
            }
        }
    }


}