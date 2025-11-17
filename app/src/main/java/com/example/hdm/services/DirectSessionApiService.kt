// Ścieżka: com/example/hdm/services/DirectSessionApiService.kt
package com.example.hdm.services

import android.util.Log
import com.example.hdm.model.DirectSessionResponse
import com.example.hdm.model.ImageUploadResponse
import com.example.hdm.model.PendingSessionListResponse
import com.example.hdm.model.SessionDetailsApiResponse
import com.example.hdm.model.SessionDetailsResponse
import com.example.hdm.model.SessionSummaryResponse
import com.example.hdm.model.SessionUpdateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectSessionApiService @Inject constructor(
    private val okHttpClient: OkHttpClient, // Główny klient z Hilt
    private val json: Json
) {
    companion object {
        private const val TAG = "DirectSessionApi"
        private const val BASE_URL = "http://hdlm.meikotrans.com.pl:6001/api/hdm/direct_session"

        // ===== NOWA KONFIGURACJA PONOWIEŃ =====
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
        // ======================================

        /**
         * Generuje 24-znakowy losowy identyfikator sesji w formacie hex.
         * (Format 68c806c10f2fcef6ea51b27b)
         */
        fun generateSessionId(): String {
            val random = SecureRandom()
            val bytes = ByteArray(12) // 12 bajtów = 24 znaki hex
            random.nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    // ===== NOWE, WYSPECJALIZOWANE KLIENTY HTTP =====
    /**
     * Klient do szybkich operacji JSON (GET, POST z tekstem).
     * Agresywny timeout 10 sekund.
     */
    private val jsonClient = okHttpClient.newBuilder()
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Klient do wolniejszych operacji (wysyłanie plików).
     * Dłuższy timeout 15 sekund (zgodnie z sugestią).
     */
    private val fileClient = okHttpClient.newBuilder()
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
    // ================================================

    /**
     * Wysyła aktualizację danych tekstowych (nagłówek, paleta) do sesji.
     * Implementuje logikę ponowień.
     */
    suspend fun updateSession(sessionId: String, data: Map<String, String>): Result<DirectSessionResponse> {
        return withContext(Dispatchers.IO) {
            val requestBody = SessionUpdateRequest(sessionId = sessionId, data = data)
            val jsonBody = json.encodeToString(requestBody)
            val request = Request.Builder()
                .url("$BASE_URL/update")
                .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            // ===== POPRAWKA 1: Zmiana typu z Exception? na Throwable? =====
            var lastException: Throwable? = null
            // ==========================================================

            for (attempt in 1..MAX_RETRIES) {
                try {
                    jsonClient.newCall(request).execute().use { response -> // Użycie jsonClient
                        val result = handleJsonResponse(response)
                        // Zwróć natychmiast, jeśli sukces LUB jeśli serwer dał logiczny błąd (np. 4xx, 5xx)
                        if (result.isSuccess || (response.code < 500 && response.code >= 400)) {
                            return@withContext result
                        }
                        // Jeśli to błąd serwera (np. 503), zapisz go i spróbuj ponownie
                        lastException = result.exceptionOrNull() ?: IOException("Server error ${response.code}")
                    }
                } catch (e: SocketTimeoutException) {
                    Log.w(TAG, "updateSession próba $attempt/$MAX_RETRIES nie powiodła się (Timeout): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt) // Czekaj dłużej przy kolejnych próbach
                } catch (e: IOException) { // Inne błędy sieciowe (np. brak połączenia)
                    Log.w(TAG, "updateSession próba $attempt/$MAX_RETRIES nie powiodła się (IO): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: Exception) { // Inne błędy (np. błąd serializacji) - nie ponawiaj
                    Log.e(TAG, "updateSession próba $attempt/$MAX_RETRIES nie powiodła się (Krytyczny): ${e.message}", e)
                    return@withContext Result.failure(e)
                }
            }
            // Jeśli pętla się skończyła, wszystkie próby zawiodły
            Result.failure(lastException ?: Exception("Nieznany błąd sieciowy po $MAX_RETRIES próbach"))
        }
    }

    /**
     * Wysyła plik (zdjęcie) do konkretnej palety w sesji.
     * Implementuje logikę ponowień.
     */
    suspend fun uploadImage(
        sessionId: String,
        palletIndex: Int,
        imageType: String,
        file: File,
        existingFileId: String?
    ): Result<ImageUploadResponse> {
        return withContext(Dispatchers.IO) {
            if (!file.exists() || file.length() == 0L) {
                return@withContext Result.failure(Exception("Plik obrazu jest pusty lub nie istnieje: ${file.absolutePath}"))
            }

            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("session_id", sessionId)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )

            if (existingFileId != null) {
                requestBodyBuilder.addFormDataPart("existing_file_id", existingFileId)
                Log.d(TAG, "Wysyłanie /upload_image (PODMIANA) dla $sessionId, fileId: $existingFileId, plik: ${file.name}")
            } else {
                requestBodyBuilder.addFormDataPart("pallet_index", palletIndex.toString())
                requestBodyBuilder.addFormDataPart("image_type", imageType)
                Log.d(TAG, "Wysyłanie /upload_image (NOWY) dla $sessionId, index $palletIndex, typ $imageType, plik: ${file.name}")
            }

            val request = Request.Builder()
                .url("$BASE_URL/upload_image")
                .post(requestBodyBuilder.build())
                .build()

            // ===== POPRAWKA 1: Zmiana typu z Exception? na Throwable? =====
            var lastException: Throwable? = null
            // ==========================================================

            for (attempt in 1..MAX_RETRIES) {
                try {
                    fileClient.newCall(request).execute().use { response -> // Użycie fileClient
                        val result = handleImageUploadResponse(response)
                        if (result.isSuccess || (response.code < 500 && response.code >= 400)) {
                            return@withContext result
                        }
                        lastException = result.exceptionOrNull() ?: IOException("Server error ${response.code}")
                    }
                } catch (e: SocketTimeoutException) {
                    Log.w(TAG, "uploadImage próba $attempt/$MAX_RETRIES nie powiodła się (Timeout): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: IOException) {
                    Log.w(TAG, "uploadImage próba $attempt/$MAX_RETRIES nie powiodła się (IO): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: Exception) {
                    Log.e(TAG, "uploadImage próba $attempt/$MAX_RETRIES nie powiodła się (Krytyczny): ${e.message}", e)
                    return@withContext Result.failure(e)
                }
            }
            Result.failure(lastException ?: Exception("Nieznany błąd sieciowy po $MAX_RETRIES próbach"))
        }
    }

    /**
     * Wysyła wygenerowany plik PDF (PL lub EN) do sesji.
     * Implementuje logikę ponowień.
     */
    suspend fun uploadPdf(
        sessionId: String,
        pdfTag: String, // "PL" lub "EN"
        file: File
    ): Result<DirectSessionResponse> {
        return withContext(Dispatchers.IO) {
            if (!file.exists() || file.length() == 0L) {
                return@withContext Result.failure(Exception("Plik PDF jest pusty lub nie istnieje: ${file.absolutePath}"))
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("session_id", sessionId)
                .addFormDataPart("pdf_tag_suffix", pdfTag)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("application/pdf".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/upload_pdf")
                .post(requestBody)
                .build()

            // ===== POPRAWKA 1: Zmiana typu z Exception? na Throwable? =====
            var lastException: Throwable? = null
            // ==========================================================

            for (attempt in 1..MAX_RETRIES) {
                try {
                    fileClient.newCall(request).execute().use { response -> // Użycie fileClient
                        val result = handleJsonResponse(response)
                        if (result.isSuccess || (response.code < 500 && response.code >= 400)) {
                            return@withContext result
                        }
                        lastException = result.exceptionOrNull() ?: IOException("Server error ${response.code}")
                    }
                } catch (e: SocketTimeoutException) {
                    Log.w(TAG, "uploadPdf próba $attempt/$MAX_RETRIES nie powiodła się (Timeout): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: IOException) {
                    Log.w(TAG, "uploadPdf próba $attempt/$MAX_RETRIES nie powiodła się (IO): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: Exception) {
                    Log.e(TAG, "uploadPdf próba $attempt/$MAX_RETRIES nie powiodła się (Krytyczny): ${e.message}", e)
                    return@withContext Result.failure(e)
                }
            }
            Result.failure(lastException ?: Exception("Nieznany błąd sieciowy po $MAX_RETRIES próbach"))
        }
    }

    /**
     * Pobiera listę niedokończonych sesji.
     * Implementuje logikę ponowień.
     */
    suspend fun getPendingSessions(): Result<PendingSessionListResponse> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url("$BASE_URL/list_pending").get().build()
            // ===== POPRAWKA 1: Zmiana typu z Exception? na Throwable? =====
            var lastException: Throwable? = null
            // ==========================================================

            for (attempt in 1..MAX_RETRIES) {
                try {
                    jsonClient.newCall(request).execute().use { response -> // Użycie jsonClient
                        if (!response.isSuccessful) {
                            lastException = IOException("Błąd serwera (list_pending): ${response.code}")
                            delay(RETRY_DELAY_MS * attempt)
                            // ===== POPRAWKA 2: Zmiana `continue` na `return@use` =====
                            return@use
                            // =======================================================
                        }
                        val responseBody = response.body?.string() ?: """{"success": false, "sessions": []}"""
                        val sessionsResponse = json.decodeFromString<PendingSessionListResponse>(responseBody)
                        return@withContext Result.success(sessionsResponse)
                    }
                } catch (e: SocketTimeoutException) {
                    Log.w(TAG, "getPendingSessions próba $attempt/$MAX_RETRIES nie powiodła się (Timeout): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: IOException) {
                    Log.w(TAG, "getPendingSessions próba $attempt/$MAX_RETRIES nie powiodła się (IO): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: Exception) {
                    Log.e(TAG, "getPendingSessions próba $attempt/$MAX_RETRIES nie powiodła się (Krytyczny): ${e.message}", e)
                    return@withContext Result.failure(e)
                }
            }
            Result.failure(lastException ?: Exception("Nieznany błąd sieciowy po $MAX_RETRIES próbach"))
        }
    }

    /**
     * Pobiera szczegóły konkretnej sesji.
     * Implementuje logikę ponowień.
     */
    suspend fun getSessionDetails(sessionId: String): Result<SessionDetailsResponse> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url("$BASE_URL/details/$sessionId").get().build()
            // ===== POPRAWKA 1: Zmiana typu z Exception? na Throwable? =====
            var lastException: Throwable? = null
            // ==========================================================

            for (attempt in 1..MAX_RETRIES) {
                try {
                    jsonClient.newCall(request).execute().use { response -> // Użycie jsonClient
                        if (!response.isSuccessful) {
                            lastException = IOException("Błąd serwera (details): ${response.code}")
                            delay(RETRY_DELAY_MS * attempt)
                            // ===== POPRAWKA 2: Zmiana `continue` na `return@use` =====
                            return@use
                            // =======================================================
                        }
                        val responseBody = response.body?.string()
                        if (responseBody.isNullOrEmpty()) {
                            lastException = IOException("Pusta odpowiedź serwera (details)")
                            delay(RETRY_DELAY_MS * attempt)
                            // ===== POPRAWKA 2: Zmiana `continue` na `return@use` =====
                            return@use
                            // =======================================================
                        }

                        val detailsResponse = json.decodeFromString<SessionDetailsApiResponse>(responseBody)
                        if (detailsResponse.success) {
                            val details = detailsResponse.session
                            if (details._id.isEmpty() && details.headerData == null) {
                                return@withContext Result.failure(Exception("Odpowiedź serwera (details) zawierała pusty obiekt 'session'."))
                            }
                            return@withContext Result.success(details)
                        } else {
                            return@withContext Result.failure(Exception("Serwer (details) zwrócił success: false"))
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    Log.w(TAG, "getSessionDetails próba $attempt/$MAX_RETRIES nie powiodła się (Timeout): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: IOException) {
                    Log.w(TAG, "getSessionDetails próba $attempt/$MAX_RETRIES nie powiodła się (IO): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: Exception) {
                    Log.e(TAG, "getSessionDetails próba $attempt/$MAX_RETRIES nie powiodła się (Krytyczny): ${e.message}", e)
                    return@withContext Result.failure(e)
                }
            }
            Result.failure(lastException ?: Exception("Nieznany błąd sieciowy po $MAX_RETRIES próbach"))
        }
    }

    /**
     * Pobiera podsumowanie palet i ich liczbę dla danej sesji.
     * Implementuje logikę ponowień.
     */
    suspend fun getSessionSummary(sessionId: String): Result<SessionSummaryResponse> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url("$BASE_URL/summary/$sessionId").get().build()
            // ===== POPRAWKA 1: Zmiana typu z Exception? na Throwable? =====
            var lastException: Throwable? = null
            // ==========================================================

            for (attempt in 1..MAX_RETRIES) {
                try {
                    jsonClient.newCall(request).execute().use { response -> // Użycie jsonClient
                        if (!response.isSuccessful) {
                            if (response.code == 404) {
                                Log.w(TAG, "Błąd 404 (summary) dla $sessionId. Traktuję jako nową sesję (0 palet).")
                                return@withContext Result.success(SessionSummaryResponse(0, emptyList(), sessionId, true))
                            }
                            lastException = IOException("Błąd serwera (summary): ${response.code}")
                            delay(RETRY_DELAY_MS * attempt)
                            // ===== POPRAWKA 2: Zmiana `continue` na `return@use` =====
                            return@use
                            // =======================================================
                        }
                        val responseBody = response.body?.string()

                        if (responseBody.isNullOrEmpty()) {
                            Log.w(TAG, "Pusta odpowiedź (summary) dla $sessionId. Traktuję jako nową sesję (0 palet).")
                            return@withContext Result.success(SessionSummaryResponse(0, emptyList(), sessionId, true))
                        } else {
                            val summary = json.decodeFromString<SessionSummaryResponse>(responseBody)
                            return@withContext Result.success(summary)
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    Log.w(TAG, "getSessionSummary próba $attempt/$MAX_RETRIES nie powiodła się (Timeout): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: IOException) {
                    Log.w(TAG, "getSessionSummary próba $attempt/$MAX_RETRIES nie powiodła się (IO): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: Exception) {
                    Log.e(TAG, "getSessionSummary próba $attempt/$MAX_RETRIES nie powiodła się (Krytyczny): ${e.message}", e)
                    return@withContext Result.failure(e) // Błąd deserializacji (jak ten z 'barcode') zakończy pętlę
                }
            }
            Result.failure(lastException ?: Exception("Nieznany błąd sieciowy po $MAX_RETRIES próbach"))
        }
    }


    /**
     * Finalizuje sesję na serwerze.
     * Implementuje logikę ponowień.
     */
    suspend fun finalizeSession(sessionId: String): Result<DirectSessionResponse> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$BASE_URL/finalize/$sessionId")
                .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType())) // Puste body JSON
                .build()

            // ===== POPRAWKA 1: Zmiana typu z Exception? na Throwable? =====
            var lastException: Throwable? = null
            // ==========================================================

            for (attempt in 1..MAX_RETRIES) {
                try {
                    jsonClient.newCall(request).execute().use { response -> // Użycie jsonClient
                        val result = handleJsonResponse(response)
                        if (result.isSuccess || (response.code < 500 && response.code >= 400)) {
                            return@withContext result
                        }
                        lastException = result.exceptionOrNull() ?: IOException("Server error ${response.code}")
                    }
                } catch (e: SocketTimeoutException) {
                    Log.w(TAG, "finalizeSession próba $attempt/$MAX_RETRIES nie powiodła się (Timeout): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: IOException) {
                    Log.w(TAG, "finalizeSession próba $attempt/$MAX_RETRIES nie powiodła się (IO): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: Exception) {
                    Log.e(TAG, "finalizeSession próba $attempt/$MAX_RETRIES nie powiodła się (Krytyczny): ${e.message}", e)
                    return@withContext Result.failure(e)
                }
            }
            Result.failure(lastException ?: Exception("Nieznany błąd sieciowy po $MAX_RETRIES próbach"))
        }
    }

    /**
     * Usuwa sesję z serwera.
     * Implementuje logikę ponowień.
     */
    suspend fun deleteSession(sessionId: String): Result<DirectSessionResponse> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$BASE_URL/delete/$sessionId")
                .delete() // Metoda DELETE
                .build()

            // ===== POPRAWKA 1: Zmiana typu z Exception? na Throwable? =====
            var lastException: Throwable? = null
            // ==========================================================

            for (attempt in 1..MAX_RETRIES) {
                try {
                    jsonClient.newCall(request).execute().use { response -> // Użycie jsonClient
                        val result = handleJsonResponse(response)
                        if (result.isSuccess || (response.code < 500 && response.code >= 400)) {
                            return@withContext result
                        }
                        lastException = result.exceptionOrNull() ?: IOException("Server error ${response.code}")
                    }
                } catch (e: SocketTimeoutException) {
                    Log.w(TAG, "deleteSession próba $attempt/$MAX_RETRIES nie powiodła się (Timeout): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: IOException) {
                    Log.w(TAG, "deleteSession próba $attempt/$MAX_RETRIES nie powiodła się (IO): ${e.message}")
                    lastException = e
                    delay(RETRY_DELAY_MS * attempt)
                } catch (e: Exception) {
                    Log.e(TAG, "deleteSession próba $attempt/$MAX_RETRIES nie powiodła się (Krytyczny): ${e.message}", e)
                    return@withContext Result.failure(e)
                }
            }
            Result.failure(lastException ?: Exception("Nieznany błąd sieciowy po $MAX_RETRIES próbach"))
        }
    }

    /**
     * Prywatna funkcja pomocnicza do obsługi odpowiedzi z /upload_image.
     */
    private fun handleImageUploadResponse(response: okhttp3.Response): Result<ImageUploadResponse> {
        val responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            Log.w(TAG, "Odpowiedź błędu serwera (upload_image, ${response.code}): $responseBody")
            try {
                val errorResponse = json.decodeFromString<ImageUploadResponse>(responseBody)
                return Result.failure(Exception(errorResponse.message))
            } catch (e: Exception) {
                return Result.failure(Exception("Błąd serwera (upload_image, ${response.code}): $responseBody"))
            }
        }
        if (responseBody.isEmpty()) {
            Log.w(TAG, "Pusta odpowiedź serwera (upload_image, 200)")
            return Result.failure(Exception("Pusta odpowiedź serwera (upload_image)"))
        }

        return try {
            val uploadResponse = json.decodeFromString<ImageUploadResponse>(responseBody)
            if (uploadResponse.success && uploadResponse.fileId != null) {
                Result.success(uploadResponse)
            } else {
                Log.w(TAG, "Serwer (upload_image) zwrócił 'success: false' lub brak file_id: ${uploadResponse.message}")
                Result.failure(Exception(uploadResponse.message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd parsowania odpowiedzi JSON (upload_image)", e)
            Result.failure(e)
        }
    }

    /**
     * Prywatna funkcja pomocnicza do obsługi standardowych odpowiedzi JSON (sukces/błąd).
     */
    private fun handleJsonResponse(response: okhttp3.Response): Result<DirectSessionResponse> {
        val responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            Log.w(TAG, "Odpowiedź błędu serwera (${response.code}): $responseBody")
            try {
                val errorResponse = json.decodeFromString<DirectSessionResponse>(responseBody)
                return Result.failure(Exception(errorResponse.message))
            } catch (e: Exception) {
                return Result.failure(Exception("Błąd serwera (${response.code}): $responseBody"))
            }
        }
        if (responseBody.isEmpty()) {
            Log.w(TAG, "Pusta odpowiedź serwera (200)")
            // Traktujemy pustą odpowiedź 200 OK jako sukces
            return Result.success(DirectSessionResponse(true, "OK"))
        }

        return try {
            val sessionResponse = json.decodeFromString<DirectSessionResponse>(responseBody)
            if (sessionResponse.success) {
                Result.success(sessionResponse)
            } else {
                Log.w(TAG, "Serwer zwrócił 'success: false': ${sessionResponse.message}")
                Result.failure(Exception(sessionResponse.message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd parsowania odpowiedzi JSON", e)
            Result.failure(e)
        }
    }
}