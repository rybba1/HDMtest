package com.example.hdm.services

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hdm.model.FileLogService
import com.example.hdm.model.LogRepository
import com.example.hdm.network.NetworkService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LogSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val networkService: NetworkService,
    private val fileLogService: FileLogService,
    private val hdmLogger: HdmLogger
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "LogSyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Automat: Uruchamiam sprawdzanie zaległych logów.")

        val allLogs = fileLogService.readLogs(applicationContext)
        val unsentLogs = allLogs.filter { !it.isSent.value && it.message != "PULSE" }

        if (unsentLogs.isEmpty()) {
            Log.d(TAG, "Automat: Brak zaległych logów do wysłania.")
            return Result.success()
        }

        Log.d(TAG, "Automat: Znaleziono ${unsentLogs.size} zaległych logów. Rozpoczynam wysyłkę.")
        var allSucceeded = true
        val updatedLogs = allLogs.toMutableList()

        for (originalLog in unsentLogs) {
            try {
                val xmlLog = LogXmlGenerator.generateLogXml(originalLog.message, applicationContext, true)

                // === LOGOWANIE DO LOGCAT ===
                Log.d("PAYLOAD_DEBUG", "Automat: Wysyłanie zaległego logu XML: $xmlLog")
                // ===========================

                val result = networkService.uploadXmlReport(xmlLog)

                if (result.isSuccess) {
                    Log.i(TAG, "Automat: Pomyślnie wysłano zaległy log: '${originalLog.message}'")

                    val index = updatedLogs.indexOfFirst { it.id == originalLog.id }
                    if (index != -1) {
                        val updatedLog = updatedLogs[index].copy().apply { isSent.value = true }
                        updatedLogs[index] = updatedLog
                        LogRepository.updateLogEntry(updatedLog)
                    }
                } else {
                    Log.w(TAG, "Automat: Nie udało się wysłać zaległego logu '${originalLog.message}'. Błąd: ${result.exceptionOrNull()?.message}")
                    allSucceeded = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Automat: Krytyczny błąd podczas wysyłania zaległego logu '${originalLog.message}'.", e)
                allSucceeded = false
            }
        }

        fileLogService.updateLogs(applicationContext, updatedLogs)

        return if (allSucceeded) Result.success() else Result.retry()
    }
}