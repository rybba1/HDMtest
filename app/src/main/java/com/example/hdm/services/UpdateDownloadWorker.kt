// ZASTĄP CAŁY PLIK: com/example/hdm/services/UpdateDownloadWorker.kt
package com.example.hdm.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.hdm.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class UpdateDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val loginApiService: LoginApiService
) : CoroutineWorker(context, workerParams) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val KEY_FILENAME = "FILENAME"
        const val KEY_PROGRESS = "PROGRESS"
        // --- POCZĄTEK ZMIANY ---
        const val KEY_APK_URI = "APK_URI" // Klucz dla zwracanych danych
        // --- KONIEC ZMIANY ---
        private const val NOTIFICATION_ID = 42
        private const val CHANNEL_ID = "HdmUpdateChannel"
        private const val TAG = "UpdateDownloadWorker"
    }

    override suspend fun doWork(): Result {
        val filename = inputData.getString(KEY_FILENAME)
            ?: return Result.failure(workDataOf("ERROR" to "Brak nazwy pliku"))

        val apkDir = File(context.cacheDir, "updates")
        if (!apkDir.exists()) {
            apkDir.mkdirs()
        }
        val destinationFile = File(apkDir, filename)

        createNotificationChannel()
        val startingNotification = createNotification("Pobieranie aktualizacji", "Rozpoczynanie...", 0)

        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                startingNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, startingNotification)
        }
        setForeground(foregroundInfo)

        try {
            Log.i(TAG, "Rozpoczynam pobieranie: $filename do $destinationFile")

            val result = loginApiService.downloadUpdateApk(filename, destinationFile) { bytesRead, totalBytes ->
                val progress = if (totalBytes > 0) ((bytesRead * 100) / totalBytes).toInt() else -1
                setProgress(workDataOf(KEY_PROGRESS to progress))

                val notification = createNotification("Pobieranie aktualizacji", "$progress%", progress)
                notificationManager.notify(NOTIFICATION_ID, notification)
            }

            return if (result.isSuccess) {
                Log.i(TAG, "Pobieranie zakończone pomyślnie.")

                // --- POCZĄTEK ZMIANY ---
                // Zamiast pokazywać powiadomienie, pobierz URI i zwróć je jako output
                val apkUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    destinationFile
                )
                // Usuń powiadomienie o postępie
                notificationManager.cancel(NOTIFICATION_ID)
                // Zwróć URI jako wynik
                Result.success(workDataOf(KEY_APK_URI to apkUri.toString()))
                // --- KONIEC ZMIANY ---

            } else {
                Log.e(TAG, "Błąd pobierania: ${result.exceptionOrNull()?.message}")
                destinationFile.delete()
                showErrorNotification("Błąd pobierania pliku.")
                val errorMsg = result.exceptionOrNull()?.message ?: "Błąd pobierania"
                Result.failure(workDataOf("ERROR" to errorMsg))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Wyjątek w doWork", e)
            destinationFile.delete()
            showErrorNotification("Błąd: ${e.message}")
            val errorMsg = e.message ?: "Nieznany wyjątek"
            return Result.failure(workDataOf("ERROR" to errorMsg))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Aktualizacje aplikacji"
            val descriptionText = "Powiadomienia o pobieraniu nowych wersji HDM"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, text: String, progress: Int): android.app.Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)

        if (progress >= 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, true) // Indeterminate
        }

        return builder.build()
    }

    // --- POCZĄTEK ZMIANY ---
    // Ta funkcja nie jest już potrzebna, została usunięta
    // private suspend fun showDownloadCompleteNotification(apkFile: File) { ... }
    // --- KONIEC ZMIANY ---

    private fun showErrorNotification(text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Błąd aktualizacji")
            .setContentText(text)
            .setSmallIcon(R.drawable.logo)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}