// Ścieżka: com/example/hdm/services/PhotoDocUploadWorker.kt
package com.example.hdm.services

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hdm.network.NetworkService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PhotoDocUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val fileService: FileService,
    private val networkService: NetworkService,
    private val hdmLogger: HdmLogger
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_REPORT_ID = "REPORT_ID"
        private const val TAG = "PhotoDocUploadWorker"
    }

    override suspend fun doWork(): Result {
        val reportId = inputData.getString(KEY_REPORT_ID) ?: return Result.failure()
        Log.d(TAG, "Rozpoczynam wysyłanie dokumentacji zdjęciowej: $reportId")

        val xmlContent = fileService.getReportXmlContentById(applicationContext, reportId)
        if (xmlContent == null) {
            val errorMsg = "Poczekalnia: Nie znaleziono pliku '$reportId'."
            Log.e(TAG, errorMsg)
            hdmLogger.log(applicationContext, errorMsg, isBackgroundJob = true)
            return Result.failure() // Błąd, nie ponawiaj, pliku nie ma
        }

        try {
            // 1. Wyślij na serwer główny (IP)
            val mainUploadResult = networkService.uploadXmlReport(xmlContent, readTimeout = 120L)

            // 2. Wyślij na serwer DNS (Backup)
            val dnsUploadResult = networkService.uploadXmlToDnsServer(xmlContent, "$reportId.xml")

            if (mainUploadResult.isSuccess && dnsUploadResult.isSuccess) {
                Log.i(TAG, "Pomyślnie wysłano dok. zdjęciową '$reportId' na oba serwery.")
                hdmLogger.log(applicationContext, "Worker: Dok. zdjęciowa '$reportId' wysłana pomyślnie.", isBackgroundJob = true)
                fileService.deleteReportFromWaitingRoom(applicationContext, reportId)
                return Result.success()
            } else {
                Log.w(TAG, "Błąd wysyłania dok. zdjęciowej '$reportId'. Główny: ${mainUploadResult.isFailure}, DNS: ${dnsUploadResult.isFailure}")
                hdmLogger.log(applicationContext, "Worker: Błąd wysyłania dok. zdjęciowej '$reportId'. Retry.", LogLevel.WARNING, true)
                return Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Krytyczny błąd workera dok. zdjęciowej dla '$reportId'", e)
            hdmLogger.log(applicationContext, "Worker: Krytyczny błąd wysyłki dok. zdjęciowej '$reportId': ${e.message}", LogLevel.ERROR, true)
            return Result.retry()
        }
    }
}